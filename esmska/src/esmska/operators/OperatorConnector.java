/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.operators;

import esmska.utils.Nullator;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;

/** Class for connecting to HTTP resources and sending GET and POST requests.
 *  For each SMS there should be a separate instance.
 * @author ripper
 */
public class OperatorConnector {

    private static final Logger logger = Logger.getLogger(OperatorConnector.class.getName());
    private static final String USER_AGENT = "Mozilla/5.0 (X11; U; Linux i686; cs-CZ; rv:1.8.1.13)" +
            " Gecko/20080325 Ubuntu/7.10 (gutsy) Firefox/2.0.0.13";
    private final HttpClient client = new HttpClient();
    private String url;
    private String[] params;
    private String[] postData;
    private boolean doPost;
    private String textContent;
    private byte[] binaryContent;
    private String referer;
    private String fullURL;

    /** Constructor for OperatorConnector. */
    public OperatorConnector() {
        //set cookie compatibility mode
        client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        client.getParams().setParameter("http.protocol.single-cookie-header", true);

        //set user-agent - just to be sure that the server won't screw us
        client.getParams().setParameter("http.useragent", USER_AGENT);
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
        this.url = url;
        this.params = params;
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

    /** Perform GET request.
     * @param url URL where to connect
     * @return true if connection succeeded; false otherwise
     * @throws java.io.IOException When there is some problem with connection
     */
    private boolean doGet(String url) throws IOException {
        GetMethod method = new GetMethod(url);

        //set referer
        if (referer != null) {
            method.setRequestHeader("Referer", referer);
        }

        int statusCode = client.executeMethod(method);

        //only HTTP 200 OK status code is correct
        if (statusCode != HttpStatus.SC_OK) {
            logger.warning("Problem connecting to \"" + url +
                    "\". Response: " + method.getStatusLine());
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
            setTextContent(new String(response, method.getResponseCharSet()));
        } else { //binary content
            setBinaryContent(response);
        }

        //if text response, check for meta redirects
        if (text) {
            String redirect = checkMetaRedirect(textContent);
            if (redirect != null) {
                //redirect to new url
                return followMetaRedirect(redirect, method.getURI());
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
            logger.warning("Problem connecting to \"" + url +
                    "\". Response: " + method.getStatusLine());
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
            setTextContent(new String(response, method.getResponseCharSet()));
        } else { //binary content
            setBinaryContent(response);
        }

        //check for HTTP redirection
        if (statuscode >= 300 && statuscode < 400) {
            Header header = method.getResponseHeader("Location");
            if (header == null) {
                throw new IOException("Invalid HTTP redirect, no Location header");
            }
            String newURL = header.getValue();
            if (Nullator.isEmpty(newURL)) {
                throw new IOException("Invalid HTTP redirect, Location header is empty");
            }
            if (newURL.startsWith("./") || newURL.startsWith("../")) {
                try {
                    newURL = convertRelativeRedirectToAbsolute(url, newURL);
                } catch (IOException ex) {
                    throw new IOException("Invalid HTTP redirect, Location header must " +
                            "be an absolute path and is: '" + newURL + "'", ex);
                }
            }
            if (!newURL.startsWith("http://") && !newURL.startsWith("https://") 
                    && !newURL.startsWith("/")) {
                newURL = "/" + newURL;
            }
            //redirect to new url
            return doGet(newURL);
        }

        //if text response, check for meta redirects
        if (text) {
            String redirect = checkMetaRedirect(textContent);
            if (redirect != null) {
                //redirect to new url
                return followMetaRedirect(redirect, method.getURI());
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
            if (Nullator.isEmpty(key)) {
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
     *  If the resulting URL is relative URL, it will always start with '/'.
     */
    private static String checkMetaRedirect(String page) {
        Pattern pattern = Pattern.compile("<meta\\s+http-equiv=[^>]*refresh[^>]*" +
                "url=(.*)(\"|')[^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(page);
        if (matcher.find()) {
            String redirect = matcher.group(1);
            if (!redirect.startsWith("http://") && !redirect.startsWith("https://") && !redirect.startsWith("/")) {
                redirect = "/" + redirect;
            }
            return redirect;
        }
        return null;
    }

    /** Follow a meta redirect.
     * 
     * @param redirectURL new URL. May be absolute or relative.
     * @param currentURI current URI
     * @return result of doGet(url) method
     * @throws java.io.IOException Problem when connecting
     */
    private boolean followMetaRedirect(String redirectURL, URI currentURI) throws IOException {
        if (redirectURL == null) {
            throw new IllegalArgumentException("redirectURL");
        }
        if (currentURI == null) {
            throw new IllegalArgumentException("currentURI");
        }

        //convert relative to absolute URL
        if (redirectURL.startsWith("/")) {
            String uri = currentURI.getEscapedURI();
            int slash = uri.indexOf('/', "https://".length());
            if (slash > 0) {
                uri = uri.substring(0, slash);
            }
            redirectURL = uri + redirectURL;
        }
        //check for redirection loops
        if (url.equalsIgnoreCase(redirectURL)) {
            throw new IOException("HTTP meta redirection endless loop detected");
        }

        return doGet(redirectURL);
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
                    "redirect to URL '" + oldUrl + "'");
        }
    }
}
