package esmska.transfer;

import esmska.data.Tuple;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/** Class for connecting to HTTP resources and sending GET and POST requests.
 *  For each SMS there should be a separate instance.
 * @author ripper
 */
public class GatewayConnector {

    private static final Logger logger = Logger.getLogger(GatewayConnector.class.getName());
    private static final String USER_AGENT = "Mozilla/5.0 (X11; U; Linux x86_64; cs-CZ; rv:1.9.1.9) " +
            "Gecko/20100402 Ubuntu/9.10 (karmic) Firefox/3.5.9";
    private static final Pattern metaRedirPattern = Pattern.compile(
            "<meta\\s+http-equiv=[^>]*refresh[^>]*url=([^>]*)(\"|')[^>]*>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern metaCharsetPattern = Pattern.compile(
            "<meta\\s+http-equiv=[^>]*content-type[^>]*charset=([^>]*)(\"|')[^>]*>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern xmlCharsetPattern = Pattern.compile(
            "<\\?xml[^>]*encoding=(\"|')([^>]*)(\"|')[^>]*\\?>",
            Pattern.CASE_INSENSITIVE);
    private final HttpClient client = new HttpClient();
    private String[] postData;
    private boolean doPost;
    private String textContent;
    private byte[] binaryContent;
    private String referer;
    private String fullURL;
    // remembers last redirect and number of requests to that URL
    private Tuple<String, Integer> lastRedirect = new Tuple<String, Integer>(null,0);

    /** Constructor for GatewayConnector. */
    public GatewayConnector() {
        //set cookie compatibility mode
        client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        client.getParams().setParameter(HttpMethodParams.SINGLE_COOKIE_HEADER, true);

        //set user-agent - just to be sure that the server won't screw us
        client.getParams().setParameter(HttpMethodParams.USER_AGENT, USER_AGENT);

        //set wise redirect policy
        //allow circular redirects because some sites use it (with cookies)
        client.getParams().setParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);
        client.getParams().setParameter(HttpClientParams.REJECT_RELATIVE_REDIRECT, false);
        client.getParams().setParameter(HttpClientParams.MAX_REDIRECTS, 50);

        //set UTF-8 as default charset
        client.getParams().setParameter(HttpClientParams.HTTP_CONTENT_CHARSET, "UTF-8");
        client.getParams().setParameter(HttpClientParams.HTTP_ELEMENT_CHARSET, "UTF-8");
        client.getParams().setParameter(HttpClientParams.HTTP_URI_CHARSET, "UTF-8");
    }

    // <editor-fold defaultstate="collapsed" desc="Get Methods">
    /** True if received response is textual, false if binary */
    public boolean isTextContent() {
        return textContent != null;
    }

    /** Get text response */
    public String getTextContent() {
        return textContent;
    }

    /** Get binary response */
    public byte[] getBinaryContent() {
        return binaryContent;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Set Methods">
    /** Set referer. Default is empty string. Use null to clear referer. */
    public void setReferer(String referer) {
        this.referer = referer;
    }

    /** Sets preferred language to retrieve web content.
     * @param languageCode two-letter language code as defined in ISO 639-1
     */
    public void setLanguage(String languageCode) {
        //set Accept-Language headers
        @SuppressWarnings("unchecked")
        HashSet<Header> headerSet = (HashSet<Header>) client.getHostConfiguration().
                getParams().getParameter("http.default-headers");
        if (headerSet == null) {
            headerSet = new HashSet<Header>();
        }
        Header languageHeader = new Header("Accept-Language", languageCode);
        headerSet.add(languageHeader);
        client.getHostConfiguration().getParams().setParameter("http.default-headers", headerSet);
        logger.log(Level.FINER, "Preferred language set: {0}", languageCode);
    }
    
    /** Sets binary content, clears text content. */
    private void setBinaryContent(byte[] binaryContent) {
        this.binaryContent = binaryContent;
        this.textContent = null;
    }

    /** Sets text content, clears binary content. */
    private void setTextContent(String textContent) {
        this.textContent = textContent;
        this.binaryContent = null;
    }
    // </editor-fold>

    /** Remove specified cookie from the cookie cache. Removes all cookies that
     * satisfies the requirements (the parameters). The parameters are handled
     * like having logical AND between them. Any of the parameter may be null to
     * mean "any value". Therefore all null parameters mean remove all cookies.
     *
     * @param name name of the cookie; may be null
     * @param domain domain of the cookie; may be null
     * @param path path of the cookie; may be null
     */
    public void forgetCookie(String name, String domain, String path) {
        Cookie[] cookies = client.getState().getCookies();
        for (int i = 0; i < cookies.length; i++) {
            Cookie cookie = cookies[i];
            if ((name == null || name.equals(cookie.getName())) &&
                    (domain == null || domain.equals(cookie.getDomain())) &&
                    (path == null || path.equals(cookie.getPath()))) {
                cookies[i] = null;
            }
        }
        client.getState().clearCookies();
        client.getState().addCookies(cookies);
    }

    /** Prepare connector for a new connection.
     * @param url URL where to connect. If you specify <tt>params</tt>, this must not 
     *  contain '?'.
     * @param params Additional parameters to the URL (aka query string).
     *  The array is in the form [key1,value1,key2,value2,...]. Use null or
     *  empty array for no parameters.
     * @param doPost true if this should be POST request; false if this should
     *  bet GET request
     * @param postData Data to be sent in the POST request. The array is in the 
     *  form [key1,value1,key2,value2,...]. Use null or empty array for no data.
     * @throws IllegalArgumentException When <tt>url</tt> is null.
     * @throws IOException When the <tt>url</tt> and <tt>params</tt> together does not
     *  create a correct URL.
     */
    public void setConnection(String url, String[] params, boolean doPost, String[] postData)
            throws IOException {
        if (url == null) {
            throw new IllegalArgumentException("url");
        }
        this.doPost = doPost;
        this.postData = postData;

        //create final url
        fullURL = url;
        String param = convertParamsToString(params);
        if (param.length() > 0) {
            fullURL += "?" + param;
        }

        //set host - useful for redirects
        URL address = new URL(fullURL);
        client.getHostConfiguration().setHost(address.getHost(), address.getPort(),
                address.getProtocol());

        //set proxy
        if ("http".equals(address.getProtocol())) {
            client.getHostConfiguration().setProxyHost(
                    ProxyManager.getProxyHost(ProxyManager.ProxyType.HTTP));
        } else if ("https".equals(address.getProtocol())) {
            client.getHostConfiguration().setProxyHost(
                    ProxyManager.getProxyHost(ProxyManager.ProxyType.HTTPS));
        } else {
            client.getHostConfiguration().setProxyHost(
                    ProxyManager.getProxyHost(ProxyManager.ProxyType.SOCKS));
        }
    }
    
    /** Perform a connection (GET or POST, depending on configuration).
     * @throws IOException when there is a problem with connection
     */
    public boolean connect() throws IOException {
        //delete previous response to allow repeated usage
        textContent = null;
        binaryContent = null;

        //connect
        if (doPost) {
            return doPost(fullURL, postData);
        } else {
            return doGet(fullURL);
        }
    }

    /** Find charset definition inside html/xml file
     * @param content content retrieved from web page
     * @return charset name or null if none found
     */
    private String findContentCharset(byte[] content) {
        Validate.notNull(content);
        try {
            String text = new String(content, "UTF-8");
            Matcher matcher = metaCharsetPattern.matcher(text);
            if (matcher.find()) {
                String charset = matcher.group(1);
                return charset;
            }
            matcher = xmlCharsetPattern.matcher(text);
            if (matcher.find()) {
                String charset = matcher.group(2);
                return charset;
            }
        } catch (Exception ex) {
            logger.log(Level.FINER, "Could not find encoding of reponse: ", ex);
        }
        return null;
    }

    /** Perform GET request.
     * @param url URL where to connect
     * @return true if connection succeeded; false otherwise
     * @throws java.io.IOException When there is some problem with connection
     */
    private boolean doGet(String url) throws IOException {
        logger.log(Level.FINE, "Getting url: {0}", url);
        GetMethod method = new GetMethod(url);

        //set referer
        if (referer != null) {
            method.setRequestHeader("Referer", referer);
        }

        int statusCode = client.executeMethod(method);

        //only HTTP 200 OK status code is correct
        if (statusCode != HttpStatus.SC_OK) {
            logger.log(Level.WARNING, "Problem connecting to \"{0}\". Response: {1}", 
                    new Object[]{url, method.getStatusLine()});
            return false;
        }

        //decide whether text or binary response
        Header contentType = method.getResponseHeader("Content-Type");
        boolean text = (contentType != null && contentType.getValue().startsWith("text"));

        //read the response
        byte[] response = new byte[0];
        InputStream responseStream = method.getResponseBodyAsStream();
        if (responseStream != null) {
            response = IOUtils.toByteArray(responseStream);
            responseStream.close();
        }

        //don't forget to release connection
        method.releaseConnection();

        //save response
        if (text) { //text content
            String charset = findContentCharset(response);
            setTextContent(new String(response, 
                    StringUtils.defaultIfEmpty(charset, method.getResponseCharSet())));
            logger.log(Level.FINEST,"Retrieved text web content: {0}\n" +
                    "#### WEB CONTENT START ####\n{1}\n#### WEB CONTENT END ####", 
                    new Object[]{contentType, getTextContent()});
        } else { //binary content
            setBinaryContent(response);
            logger.log(Level.FINEST, "Retrieved binary web content: {0}", contentType);
        }

        //if text response, check for meta redirects
        if (text) {
            String redirect = checkMetaRedirect(textContent);
            if (redirect != null) {
                //redirect to new url
                logger.log(Level.FINE, "Following web redirect to: {0}", redirect);
                String redir = computeRedirect(redirect, method.getURI());
                return doGet(redir);
            }
        }

        return true;
    }

    /** Perform POST request.
     * @param url URL where to connect
     * @param postData data which to send. In the form [key1, value1, key2, value2, ...].
     * @return true if connection succeeded; false otherwise
     * @throws java.io.IOException When there is some problem with connection
     */
    private boolean doPost(String url, String[] postData) throws IOException {
        logger.log(Level.FINE, "Posting data to url: {0}", url);
        PostMethod method = new PostMethod(url);

        //set referer
        if (referer != null) {
            method.setRequestHeader("Referer", referer);
        }

        //set post data
        method.setRequestEntity(new StringRequestEntity(
                convertParamsToString(postData),
                "application/x-www-form-urlencoded",
                "UTF-8"));

        int statuscode = client.executeMethod(method);

        //check for error (4xx or 5xx) HTTP status codes
        if (statuscode >= 400) {
            logger.log(Level.WARNING, "Problem connecting to \"{0}\". Response: {1}", 
                    new Object[]{url, method.getStatusLine()});
            return false;
        }

        //decide whether text or binary response
        Header contentType = method.getResponseHeader("Content-Type");
        boolean text = (contentType != null && contentType.getValue().startsWith("text"));

        //read the response
        byte[] response = new byte[0];
        InputStream responseStream = method.getResponseBodyAsStream();
        if (responseStream != null) {
            response = IOUtils.toByteArray(responseStream);
            responseStream.close();
        }

        //don't forget to release connection
        method.releaseConnection();

        //save response
        if (text) { //text content
            String charset = findContentCharset(response);
            setTextContent(new String(response,
                    StringUtils.defaultIfEmpty(charset, method.getResponseCharSet())));
            logger.log(Level.FINEST,"Retrieved text web content: {0}\n" +
                    "#### WEB CONTENT START ####\n{1}\n#### WEB CONTENT END ####", 
                    new Object[]{contentType, getTextContent()});
        } else { //binary content
            setBinaryContent(response);
            logger.log(Level.FINEST, "Retrieved binary web content: {0}", contentType);
        }

        //check for HTTP redirection
        if (statuscode >= 300 && statuscode < 400) {
            Header header = method.getResponseHeader("Location");
            if (header == null) {
                throw new IOException("Invalid HTTP redirect, no Location header");
            }
            String newURL = header.getValue();
            if (StringUtils.isEmpty(newURL)) {
                throw new IOException("Invalid HTTP redirect, Location header is empty");
            }
            if (!isAbsoluteURL(newURL) && !newURL.startsWith("/")) {
                newURL = computeRedirect(newURL, method.getURI());
            }
            //sometimes websites send us binary mess instead of properly urlencoded address
            //see http://code.google.com/p/esmska/issues/detail?id=436
            //we need to convert it in that case
            if (!StringUtils.isAsciiPrintable(newURL)) {
                logger.log(Level.FINE, "Received invalidly encoded redirect URL: {0}", newURL);
                String[] parts = StringUtils.split(newURL, "?", 2);
                newURL = parts[0] + "?" + URLEncoder.encode(parts[1], "UTF-8");
                logger.log(Level.FINE, "URL converted to: {0}", newURL);
            }
            //some characters are not allowed in URI spec, but websites still use them
            //and browsers tolerate them, e.g. `|`
            //see https://code.google.com/p/esmska/issues/detail?id=469
            //we need to convert those
            if (newURL.contains("|")) {
                logger.log(Level.FINE, "URL contains invalid character `|`: {0}", newURL);
                newURL = newURL.replace("|", "%7C");
                logger.log(Level.FINE, "URL converted to: {0}", newURL);
            }
            //redirect to new url
            logger.log(Level.FINE, "Following http redirect to: {0}", newURL);
            return doGet(newURL);
        }

        //if text response, check for meta redirects
        if (text) {
            String redirect = checkMetaRedirect(textContent);
            if (redirect != null) {
                //redirect to new url
                logger.log(Level.FINE, "Following web redirect to: {0}", redirect);
                String redir = computeRedirect(redirect, method.getURI());
                return doGet(redir);
            }
        }

        return true;
    }

    /** Convert url parameters to string
     * @param params input array in form [key1,value1,key2,value2,...]
     * @return string key1=value1&key2=value2&... in the x-www-form-urlencoded format;
     *  or null when <tt>params</tt> are null
     */
    private static String convertParamsToString(String[] params) throws UnsupportedEncodingException {
        if (params == null) {
            return null;
        }

        String string = "";
        for (int i = 0; i < params.length; i++) {
            //skip the even ones
            if (i % 2 == 0) {
                continue;
            }
            String value = params[i];
            String key = params[i - 1];
            //skip empty keys
            if (StringUtils.isEmpty(key)) {
                continue;
            }
            string += key + "=";
            string += URLEncoder.encode(value, "UTF-8") + "&";
        }
        if (string.endsWith("&")) {
            string = string.substring(0, string.length() - 1);
        }
        return string;
    }

    /** Check in the HTML page for meta redirects
     * (<meta http-equiv="refresh"...> tags).
     * 
     * @param page A HTML page as string.
     * @return URL of the new address if meta redirect found; null otherwise.
     */
    private static String checkMetaRedirect(String page) {
        Matcher matcher = metaRedirPattern.matcher(page);
        if (matcher.find()) {
            String redirect = matcher.group(1);
            return redirect;
        }
        return null;
    }

    /** Combine an old URL and a redirect to a new URL.
     * 
     * @param redirectURL new URL. May be absolute or relative. It if starts with
     * slash (/) it is applied to domain root.
     * @param currentURI current URI. Absolute or relative.
     * @return new URL to get
     * @throws java.io.IOException Problem when computing redirect
     */
    private String computeRedirect(String redirectURL, URI currentURI) throws IOException {
        if (redirectURL == null) {
            throw new IllegalArgumentException("redirectURL");
        }
        if (currentURI == null) {
            throw new IllegalArgumentException("currentURI");
        }

        if (redirectURL.startsWith("./") || redirectURL.startsWith("../")) {
            try {
                redirectURL = convertRelativeRedirectToAbsolute(currentURI.toString(), redirectURL);
            } catch (IOException ex) {
                throw new IOException("Invalid HTTP redirect, Location header must " +
                        "be an absolute path and is: '" + redirectURL + "'", ex);
            }
        }

        if (redirectURL.startsWith("/")) {
            //relative redirect with slash
            if (!isAbsoluteURL(currentURI.getEscapedURI())) {
                //current uri is not absolute, nothing to do with it
                //keep redirect url intact
            } else {
                //current uri is absolute, strip it to domain and append redirect
                String uri = currentURI.getEscapedURI();
                int slash = uri.indexOf('/', "https://".length());
                if (slash > 0) {
                    uri = uri.substring(0, slash);
                }
                redirectURL = uri + redirectURL;
            }
        } else if (!isAbsoluteURL(redirectURL)) {
            //relative redirect without slash
            if (!isAbsoluteURL(currentURI.getEscapedURI())) {
                //current uri is not absolute, strip it to last slash
                //and append redirect
                String uri = currentURI.getEscapedURI();
                int slash = uri.lastIndexOf('/');
                if (slash > 0) {
                    uri = uri.substring(0, slash + 1);
                } else {
                    uri = "";
                }
                redirectURL = uri + redirectURL;
            } else {
                //current uri is absolute, strip it to last slash
                //(but preserve domain) and append redirect
                String uri = currentURI.getEscapedURI();
                int slash = uri.lastIndexOf('/');
                if (slash > "https://".length()) {
                    uri = uri.substring(0, slash);
                }
                redirectURL = uri + "/" + redirectURL;
            }
        } else {
            //absolute redirect
            //keep redirect url intact
        }

        //because some websites sends incorrectly escaped (or more specifically
        //unescaped) URL characters, see http://code.google.com/p/esmska/issues/detail?id=269,
        //let's try to build an URI from it, and if it fails, let's try to fix it
        //and encode characters properly
        try {
            URI uri = new URI(redirectURL, true);
        } catch (URIException ex) {
            //the URL is bad, we have to fix it
            logger.log(Level.FINER, "The computed redirect URL has invalid syntax ({0}): {1}", 
                    new Object[]{ex.getMessage(), redirectURL});
            try {
                URI uri = new URI(redirectURL, false);
                redirectURL = uri.getEscapedURI();
            } catch (Exception e) {
                throw new IOException("The computed redirect URL has invalid " +
                        "syntax and it can't be even fixed (" +
                        e.getMessage() + "): " + redirectURL);
            }
        }

        if (ObjectUtils.equals(lastRedirect.get1(), redirectURL)) {
            lastRedirect.set2(lastRedirect.get2() + 1);
        } else {
            lastRedirect.set1(redirectURL);
            lastRedirect.set2(0);
        }

        //check for redirection loops
        if (lastRedirect.get2() > 5) {
            throw new IOException("HTTP meta redirection endless loop detected");
        }

        logger.log(Level.FINE, "Computed new redirect full URL is: {0}", redirectURL);
        return redirectURL;
    }
    
    /** Convert relative redirect to absolute url
     * @param oldUrl full original URL
     * @param redirect relative redirect starting with './' or '../'
     * @throws IOException when redirect can't be applied to original URL
     */
    private String convertRelativeRedirectToAbsolute(String oldUrl, String redirect) 
            throws IOException {
        try {
            String protocol = oldUrl.substring(0, oldUrl.indexOf("//") + 2);
            String stub = oldUrl.substring(protocol.length());
            String redir = redirect;

            //strip ?a=b part
            if (stub.contains("?")) {
                stub = stub.substring(0, stub.indexOf("?"));
            }

            //strip the last path segment
            if (stub.contains("/")) {
                stub = stub.substring(0, stub.lastIndexOf("/"));
            }

            //traverse
            while (redir.startsWith("./") || redir.startsWith("../")) {
                if (redir.startsWith("./")) {
                    redir = redir.substring(2);
                    continue;
                }
                if (redir.startsWith("../")) {
                    redir = redir.substring(3);
                    stub = stub.substring(0, stub.lastIndexOf("/"));
                }
            }
            
            return protocol + stub + "/" + redir;
            
        } catch (Exception ex) {
            throw new IOException("The redirect '" + redirect + "' is not valid " +
                    "redirect to URL '" + oldUrl + "'", ex);
        }
    }

    /** Return true if string starts with http:// or https:// */
    private static boolean isAbsoluteURL(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

}
