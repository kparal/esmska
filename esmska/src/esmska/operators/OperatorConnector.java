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

/**
 *
 * @author ripper
 */
public class OperatorConnector {

    private static final Logger logger = Logger.getLogger(OperatorConnector.class.getName());
    private URL url;
    private String params;
    private String postData;
    private boolean doPost;
    private String textContent;
    private byte[] binaryContent;
    private String referer;
    private boolean useCookies;

    public void setURL(String url) {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException ex) {
            logger.log(Level.SEVERE, "Wrong URL: " + url, ex);
        }
    }

    public void setParams(String params) {
        this.params = params;
    }

    public void setPostData(String postData) {
        this.postData = postData;
    }

    public void setDoPost(boolean doPost) {
        this.doPost = doPost;
    }

    public boolean isTextContent() {
        return textContent != null;
    }

    public String getTextContent() {
        return textContent;
    }

    public byte[] getBinaryContent() {
        return binaryContent;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public String getReferer() {
        return referer;
    }

    public void setUseCookies(boolean useCookies) {
        this.useCookies = useCookies;
    }

    public boolean getUseCookies() {
        return useCookies;
    }

    public boolean connect() throws IOException {
        if (url == null) {
            throw new IOException("URL empty");
        }

        textContent = null;
        binaryContent = null;

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        if (referer != null) {
            con.setRequestProperty("Referer", referer);
        }

        if (doPost) {
            return doPost(con, postData);
        } else {
            return doGet(con);
        }
    }

    private boolean doGet(HttpURLConnection con) throws IOException {
        con.connect();
        if (con.getResponseCode() >= 400) {
            logger.warning("Problem connecting to \"" + con.getURL() +
                    "\". Response: " + con.getResponseCode() + " " + con.getResponseMessage());
            return false;
        }

        if (useCookies && CookieHandler.getDefault() instanceof CookieManager) {
            //workaround Sun's Java bug: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6610534
            Locale locale = Locale.getDefault();
            Locale.setDefault(Locale.US);
            
            CookieManager manager = (CookieManager) CookieHandler.getDefault();
            CookieHandler.setDefault(null);
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
                for (String c : cookies) {
                    List<HttpCookie> cooks = HttpCookie.parse(c);
                    for (HttpCookie cook : cooks) {
                        manager.getCookieStore().add(con.getURL().toURI(), cook);
                    }
                }
            } catch (URISyntaxException ex) {
                logger.log(Level.WARNING, "Problem saving cookie", ex);
            }
            CookieHandler.setDefault(manager);
            Locale.setDefault(locale);
        }

        String encoding = con.getContentEncoding();
        if (encoding == null) {
            encoding = con.getContentType().replaceFirst("^.*charset=", "").trim();
        }
        String contentType = con.getContentType();
        boolean text = contentType != null && contentType.startsWith("text");

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

    private boolean doPost(HttpURLConnection con, String postData) throws IOException {
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
