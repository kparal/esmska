/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.update;

import esmska.Context;
import esmska.data.Config;
import esmska.data.Icons;
import esmska.data.Gateway;
import esmska.data.Gateways;
import esmska.persistence.PersistenceManager;
import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashSet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/** Class describing and creating the version file.
 *
 * @author ripper
 */
public class VersionFile {

    public static final String TAG_ROOT = "esmska";
    public static final String TAG_LAST_VERSION = "latestStableVersion";
    public static final String TAG_LAST_UNSTABLE_VERSION = "latestUnstableVersion";
    public static final String TAG_GATEWAY = "gateway";
    public static final String TAG_NAME = "name";
    public static final String TAG_FILENAME = "fileName";
    public static final String TAG_VERSION = "version";
    public static final String TAG_MIN_VERSION = "minProgramVersion";
    public static final String TAG_DOWNLOAD = "downloadURL";
    public static final String TAG_ICON = "iconURL";

    //deprecated gateways
    public static final String TAG_DEPRECATED_GATEWAY = "deprecatedGateway";
    public static final String TAG_REASON = "reason";

    private static final String downloadProtocol = "http";
    private static final String downloadHost = "ripper.profitux.cz";
    private static final String downloadPath = "/esmska/gateways/";

    private static String stableProgramVersion = Config.getLatestVersion();
    private static String unstableProgramVersion = stableProgramVersion;

    /** Create new version file printed to standard output
     * @param args the command line arguments; the first argument is optional and
     * may contain the latest stable program version to use; the second argument
     * is optional and may contain the latest unstable program version to use
     */
    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            stableProgramVersion = args[0];
        }
        if (args.length > 1) {
            unstableProgramVersion = args[1];
        }

        PersistenceManager.instantiate();
        Context.persistenceManager.loadGateways();

        // remove fake gateways from the list
        Gateways gateways = Gateways.getInstance();
        HashSet<Gateway> fakeGateways = new HashSet<Gateway>();
        for (Gateway gateway : gateways.getAll()) {
            if (Gateways.isFakeGateway(gateway.getName())) {
                fakeGateways.add(gateway);
            }
        }
        gateways.removeAll(fakeGateways);

        create(System.out, null, null);
    }

    /** Create new version file printed to provided output stream
     * @param out output stream, not null
     * @param stableProgramVersion latest stable program version, may be null
     * @param unstableProgramVersion latest unstable program version, may be null
     */
    public static void create(OutputStream out, String stableProgramVersion, 
            String unstableProgramVersion) throws Exception {
        Validate.notNull(out);
        if (stableProgramVersion != null) {
            VersionFile.stableProgramVersion = stableProgramVersion;
        }
        if (unstableProgramVersion != null) {
            VersionFile.unstableProgramVersion = unstableProgramVersion;
        }

        Document doc = createDocument();
        serializetoXML(doc, out);
        out.flush();
    }

    /** create DOM of version file */
    private static Document createDocument() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        //root
        Node root = doc.createElement(TAG_ROOT);
        doc.appendChild(root);

        //latest stable version
        Node lastVersion = doc.createElement(TAG_LAST_VERSION);
        lastVersion.setTextContent(stableProgramVersion);
        root.appendChild(lastVersion);

        //latest unstable version
        Node lastUnstableVersion = doc.createElement(TAG_LAST_UNSTABLE_VERSION);
        lastUnstableVersion.setTextContent(unstableProgramVersion);
        root.appendChild(lastUnstableVersion);

        //gateways
        for (Gateway op : Gateways.getInstance().getAll()) {
            Node gateway = doc.createElement(TAG_GATEWAY);
            Node name = doc.createElement(TAG_NAME);
            name.setTextContent(op.getName());
            String opFileName = new File(op.getScript().toURI()).
                    getName().replaceFirst("\\.gateway$", "");
            Node fileName = doc.createElement(TAG_FILENAME);
            fileName.setTextContent(opFileName);
            Node version = doc.createElement(TAG_VERSION);
            version.setTextContent(op.getVersion());
            Node minVersion = doc.createElement(TAG_MIN_VERSION);
            minVersion.setTextContent(op.getMinProgramVersion());
            Node download = doc.createElement(TAG_DOWNLOAD);
            URI dlUri = new URI(downloadProtocol, downloadHost,
                    downloadPath + opFileName + ".gateway", null);
            download.setTextContent(dlUri.toASCIIString());
            Node icon = doc.createElement(TAG_ICON);
            if (op.getIcon() != Icons.GATEWAY_DEFAULT) {
                URI iconUri = new URI(downloadProtocol, downloadHost,
                        downloadPath + opFileName + ".png", null);
                icon.setTextContent(iconUri.toASCIIString());
            }

            gateway.appendChild(name);
            gateway.appendChild(fileName);
            gateway.appendChild(version);
            gateway.appendChild(minVersion);
            gateway.appendChild(download);
            gateway.appendChild(icon);

            root.appendChild(gateway);
        }

        return doc;
    }

    /** serialize DOM document to XML to output stream */
    private static void serializetoXML(Document doc, OutputStream output) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        //format the xml prettily
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(output);

        transformer.transform(source, result);
    }
}
