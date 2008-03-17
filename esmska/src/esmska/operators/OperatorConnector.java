/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.operators;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Class for connecting to HTTP resources and sending GET and POST requests.
 *
 * @author ripper
 */
public class OperatorConnector {

    private static final Logger logger = Logger.getLogger(OperatorConnector.class.getName());
    private URL url;
    private String postData;
    private boolean doPost;
    private String textContent;
    private byte[] binaryContent;
    private String referer;
    private boolean useCookies;

    // <editor-fold defaultstate="collapsed" desc="Get Methods">
    /** URL where to connect */
    public URL getURL() {
        return url;
    }

    /** Data to be sent in the POST request. They must be in the url-encoded name1=value1&name2=value2 form. */
    public String getPostData() {
        return postData;
    }

    /** True if set to do POST, false if GET. Default is false. */
    public boolean isDoPost() {
        return doPost;
    }

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

    /** Get referer. Default is empty string. */
    public String getReferer() {
        return referer;
    }

    /** Whether to use cookies. Default is false. */
    public boolean isUseCookies() {
        return useCookies;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Set Methods">
    /** URL where to connect */
    public void setURL(String url) throws MalformedURLException {
        this.url = new URL(url);
    }

    /** Data to be sent in the POST request. 
     * They must be in the url-encoded name1=value1&name2=value2 form.
     */
    public void setPostData(String postData) {
        this.postData = postData;
    }

    /** True if set to do POST, false if GET. Default is false. */
    public void setDoPost(boolean doPost) {
        this.doPost = doPost;
    }

    /** Set referer. Default is empty string. */
    public void setReferer(String referer) {
        this.referer = referer;
    }

    /** Whether to use cookies. Default is false. */
    public void setUseCookies(boolean useCookies) {
        this.useCookies = useCookies;
    }
    // </editor-fold>
    
    /** Perform a connection (GET or POST, depending on configuration).
     * @throws IOException when there is a problem with connection
     */
    public boolean connect() throws IOException {
        if (url == null) {
            throw new MalformedURLException("URL empty");
        }

        //delete previous response to allow repeated usage
        textContent = null;
        binaryContent = null;

        //set referer
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        if (referer != null) {
            con.setRequestProperty("Referer", referer);
        }

        //connect
        if (isDoPost()) {
            return doPost(con, getPostData());
        } else {
            return doGet(con);
        }
    }

    /** Perform GET request */
    private boolean doGet(HttpURLConnection con) throws IOException {
        con.connect();
        if (con.getResponseCode() >= 400) {
            logger.warning("Problem connecting to \"" + con.getURL() +
                    "\". Response: " + con.getResponseCode() + " " + con.getResponseMessage());
            return false;
        }

        //handle cookies by hand, there is something very sick about the default Java behaviour
        if (isUseCookies() && CookieHandler.getDefault() instanceof CookieManager) {
            //workaround Sun's Java bug: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6610534
            Locale locale = Locale.getDefault();
            Locale.setDefault(Locale.US);
            //temporarily disable default CookieManager
            CookieManager manager = (CookieManager) CookieHandler.getDefault();
            CookieHandler.setDefault(null);
            //get cookies
            List<String> cookies = new ArrayList<String>();
            List<String> c1 = con.getHeaderFields().get("Set-Cookie");
            List<String> c2 = con.getHeaderFields().get("Set-Cookie2");
            if (c1 != null) {
                cookies.addAll(c1);
            }
            if (c2 != null) {
                cookies.addAll(c2);
            }
            //headers are in reversed order compared to the http response, dunno why
            Collections.reverse(cookies);
            try {
                //save cookies
                for (String c : cookies) {
                    List<HttpCookie> cooks = HttpCookie.parse(c);
                    for (HttpCookie cook : cooks) {
                        manager.getCookieStore().add(con.getURL().toURI(), cook);
                    }
                }
            } catch (URISyntaxException ex) {
                logger.log(Level.WARNING, "Problem saving cookie", ex);
            }
            //return to initial state
            CookieHandler.setDefault(manager);
            Locale.setDefault(locale);
        }

        //parse content type
        String encoding = con.getContentEncoding();
        if (encoding == null) {
            encoding = con.getContentType().replaceFirst("^.*charset=", "").trim();
        }
        String contentType = con.getContentType();
        //decide whether text or binary response
        boolean text = contentType != null && contentType.startsWith("text");

        //read response
        if (text) { //text content
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(),
                    encoding != null ? encoding : "UTF-8"));

            textContent = "";
            String s = "";
            while ((s = br.readLine()) != null) {
                textContent += s + "\n";
            }
            br.close();
        } else { //binary content
            InputStream is = con.getInputStream();
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int count = 0;
            while ((count = is.read(buffer)) >= 0) {
                os.write(buffer, 0, count);
            }
            binaryContent = os.toByteArray();
            is.close();
        }

        con.disconnect();
        return true;
    }

    /** Perform POST request */
    private boolean doPost(HttpURLConnection con, String postData) throws IOException {
        //setup parametres
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream(), "UTF-8");

        //send POST request
        wr.write(postData);
        wr.flush();
        wr.close();

        //get reply
        return doGet(con);
    }
}
