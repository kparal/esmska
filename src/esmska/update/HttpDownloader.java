package esmska.update;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import org.apache.commons.io.IOUtils;

/** Download file via HTTP.
 * Returns String or byte[] depending if text or binary content was required.
 * @author ripper
 */
public class HttpDownloader extends SwingWorker<Object, Void> {
    private static final Logger logger = Logger.getLogger(HttpDownloader.class.getName());

    private String url;
    private boolean binary;
    private String textContent;
    private byte[] binaryContent;
    private boolean finishedOk;

    /** Constructor.
     *
     * @param url file url
     * @param binary if file contents is binary
     */
    public HttpDownloader(String url, boolean binary) {
        this.url = url;
        this.binary = binary;
    }

    @Override
    protected Object doInBackground() {
        InputStream in = null;
        try {
            URL link = new URL(url);
            HttpURLConnection con = (HttpURLConnection) link.openConnection();
            if (con.getResponseCode() != 200) {
                throw new IOException("Downloading of file '" + url + "' not succeeded, " +
                        "server responded: " + con.getResponseCode() + " " + con.getResponseMessage());
            }
            in = con.getInputStream();
            if (binary) {
                binaryContent = IOUtils.toByteArray(in);
            } else {
                textContent = IOUtils.toString(in, "UTF-8");
            }
            con.disconnect();
            finishedOk = true;
            return binary ? binaryContent : textContent;
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Could not download file: " + url, t);
        } finally {
            IOUtils.closeQuietly(in);
        }
        return null;
    }

    /** Retrieve text content. Null if requested binary file or some error. */
    public String getTextContent() {
        return textContent;
    }

    /** Retrieve binary content. Null if requested text file or some error. */
    public byte[] getBinaryContent() {
        return binaryContent;
    }

    /** Whether file was downloaded ok, or there was some error (or still running) */
    public boolean isFinishedOk() {
        return finishedOk;
    }
}
