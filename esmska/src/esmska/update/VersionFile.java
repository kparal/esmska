/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.update;

import esmska.data.Config;
import esmska.data.Icons;
import esmska.data.Operator;
import esmska.data.Operators;
import esmska.persistence.PersistenceManager;
import java.io.OutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/** Class describing and creating the version file.
 *
 * @author ripper
 */
public class VersionFile {

    static final String TAG_ROOT = "esmska";
    static final String TAG_LAST_VERSION = "latestStableVersion";
    static final String TAG_OPERATOR = "operator";
    static final String TAG_NAME = "name";
    static final String TAG_VERSION = "version";
    static final String TAG_DOWNLOAD = "downloadURL";
    static final String TAG_MIN_VERSION = "minProgramVersion";
    static final String TAG_ICON = "iconURL";
    
    private static final String downloadBase = "http://ripper.profitux.cz/esmska/operators/";

    private static String programVersion = Config.getLatestVersion();

    /** Create new version file printed to standard output
     * @param args the command line arguments; the first argument is optional and
     * may contain the latest stable program version to use
     */
    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            programVersion = args[0];
        }
        PersistenceManager.getInstance().loadOperators();

        create(System.out);
    }

    /** Create new version file printed to provided output stream */
    public static void create(OutputStream out) throws Exception {
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

        //latest version
        Node lastVersion = doc.createElement(TAG_LAST_VERSION);
        lastVersion.setTextContent(programVersion);
        root.appendChild(lastVersion);

        //operators
        for (Operator op : Operators.getInstance().getAll()) {
            Node operator = doc.createElement(TAG_OPERATOR);
            Node name = doc.createElement(TAG_NAME);
            name.setTextContent(op.getName());
            Node version = doc.createElement(TAG_VERSION);
            version.setTextContent(op.getVersion());
            Node download = doc.createElement(TAG_DOWNLOAD);
            download.setTextContent(downloadBase + op.getName() + ".operator");
            Node minVersion = doc.createElement(TAG_MIN_VERSION);
            minVersion.setTextContent(op.getMinProgramVersion());
            Node icon = doc.createElement(TAG_ICON);
            if (op.getIcon() != Icons.OPERATOR_DEFAULT) {
                icon.setTextContent(downloadBase + op.getName() + ".png");
            }

            operator.appendChild(name);
            operator.appendChild(version);
            operator.appendChild(download);
            operator.appendChild(minVersion);
            operator.appendChild(icon);

            root.appendChild(operator);
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
