package esmska.persistence;

import com.csvreader.CsvReader;
import esmska.data.*;
import esmska.data.Config.GlobalConfig;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import esmska.data.Contact;
import esmska.data.SMS;
import esmska.data.Gateway;
import esmska.data.Tuple;
import esmska.update.VersionFile;
import java.beans.IntrospectionException;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.JarURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Import program data
 *
 * @author ripper
 */
public class ImportManager {

    private static final Logger logger = Logger.getLogger(ImportManager.class.getName());

    /** Disabled constructor */
    private ImportManager() {
    }

    /** Import contacts from file */
    public static ArrayList<Contact> importContacts(File file, ContactParser.ContactType type)
            throws Exception {
        logger.finer("Importing contacts of type " + type + " from file: " + file.getAbsolutePath());
        ContactParser parser = new ContactParser(file, type);
        parser.execute();
        ArrayList<Contact> contacts = parser.get();
        logger.finer("Imported " + contacts.size() + " contacts");
        return parser.get();
    }

    /** Import sms queue from file */
    public static ArrayList<SMS> importQueue(File file) throws Exception {
        logger.finer("Importing queue from file: " + file.getAbsolutePath());
        ArrayList<SMS> queue = new ArrayList<SMS>();
        CsvReader reader = null;
        
        try {
            reader = new CsvReader(file.getPath(), ',', Charset.forName("UTF-8"));
            reader.setUseComments(true);
            while (reader.readRecord()) {
                try {
                    String name = reader.get(0);
                    String number = reader.get(1);
                    String gateway = reader.get(2);
                    String text = reader.get(3);

                    SMS sms = new SMS(number, text, gateway, name);
                    queue.add(sms);
                } catch (Exception e) {
                    logger.severe("Invalid queue record: " + reader.getRawRecord());
                    throw e;
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        logger.finer("Imported " + queue.size() + " SMSs to queue");
        return queue;
    }

    /** Import sms history from file */
    public static ArrayList<History.Record> importHistory(File file) throws Exception {
        logger.finer("Importing history from file: " + file.getAbsolutePath());
        ArrayList<History.Record> history = new ArrayList<History.Record>();
        CsvReader reader = null;
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG,
                            DateFormat.LONG, Locale.ROOT);
        
        try {
            reader = new CsvReader(file.getPath(), ',', Charset.forName("UTF-8"));
            reader.setUseComments(true);

            while (reader.readRecord()) {
                try {
                    String dateString = reader.get(0);
                    String name = reader.get(1);
                    String number = reader.get(2);
                    String gateway = reader.get(3);
                    String text = reader.get(4);
                    String senderName = reader.get(5);
                    String senderNumber = reader.get(6);

                    Date date = df.parse(dateString);

                    History.Record record = new History.Record(number, text, gateway,
                            name, senderNumber, senderName, date);
                    history.add(record);
                } catch (Exception e) {
                    logger.severe("Invalid history record: " + reader.getRawRecord());
                    throw e;
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        logger.finer("Imported " + history.size() + " history records");
        return history;
    }

    /** Import all gateways from jar resource
     * @param resource jar absolute resource path where to look for gateways
     * @throws IOException When there is problem accessing gateway directory or files
     * @throws IntrospectionException When current JRE does not support JavaScript execution
     */
    public static TreeSet<Gateway> importGateways(String resource) throws
            IOException, IntrospectionException {
        logger.finer("Importing gateways from resource: " + resource);
        URL gatewayBase = ImportManager.class.getResource(resource);
        if (gatewayBase == null || //resource doesn't exist
                !gatewayBase.getProtocol().equals("jar")) { //resource not packed in jar
            throw new IOException("Could not find jar gateway resource: " + resource);
        }
        HashSet<URL> gatewayURLs = new HashSet<URL>();
        
        JarURLConnection con = (JarURLConnection) gatewayBase.openConnection();
        for (Enumeration entries = con.getJarFile().entries(); entries.hasMoreElements();) {
            JarEntry entry = (JarEntry) entries.nextElement();
            String name = entry.getName();
            String absoluteName = name.startsWith("/") ? name : ("/" + name);
            if (absoluteName.startsWith(resource) && absoluteName.endsWith(".gateway")) {
                gatewayURLs.add(new URL("jar:" + con.getJarFileURL() + "!/" + name));
            }
        }

        return importGateways(gatewayURLs, false);
    }

    /** Import all gateways from directory
     * @param directory directory where to look for gateways
     * @param deleteOnFail whether to delete gateway files that fail to be loaded
     * @throws IOException When there is problem accessing gateway directory or files
     * @throws IntrospectionException When current JRE does not support JavaScript execution
     */
    public static TreeSet<Gateway> importGateways(File directory, boolean deleteOnFail) throws
            IOException, IntrospectionException {
        logger.finer("Importing gateways from directory: " + directory.getAbsolutePath());
        if (!directory.canRead() || !directory.isDirectory()) {
            throw new IOException("Invalid gateway directory: " + directory.getAbsolutePath());
        }
        HashSet<URL> gatewayURLs = new HashSet<URL>();
        
        File[] files = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getAbsolutePath().endsWith(".gateway") && pathname.canRead();
            }
        });

        for (File f : files) {
            gatewayURLs.add(f.toURI().toURL());
        }

        return importGateways(gatewayURLs, deleteOnFail);
    }

    /** Get set of gateways from set of gateway URLs
     * @param gatewayURLs set of gateway URLs (file or jar URLs)
     * @param deleteOnFail whether to delete gateway files that fail to be loaded
     * @return set of gateways
     * @throws java.beans.IntrospectionException When current JRE does not support JavaScript execution
     */
    private static TreeSet<Gateway> importGateways(Set<URL> gatewayURLs, boolean deleteOnFail) throws
            IntrospectionException {
        logger.log(Level.FINER, "Importing gateways from set of {0} URLs", gatewayURLs.size());
        TreeSet<Gateway> gateways = new TreeSet<Gateway>();

        for (URL gatewayURL : gatewayURLs) {
            try {
                Gateway gateway = new Gateway(gatewayURL);
                //check that this gateway can be used in this program
                if (Config.compareProgramVersions(Config.getLatestVersion(),
                        gateway.getMinProgramVersion()) < 0) {
                    logger.log(Level.INFO, "Gateway {0} requires program of version at least {1}, skipping.",
                            new Object[]{gateway.getName(), gateway.getMinProgramVersion()});
                    continue;
                }
                //check that some older version of the same gateway is not
                //already present (can happen when renaming gateway file,
                //see http://code.google.com/p/esmska/issues/detail?id=235)
                if (gateways.contains(gateway)) {
                    for (Iterator<Gateway> it = gateways.iterator(); it.hasNext(); ) {
                        Gateway op = it.next();
                        if (op.equals(gateway) &&
                                op.getVersion().compareTo(gateway.getVersion()) < 0) {
                            it.remove();
                        }
                    }
                }
                gateways.add(gateway);
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Problem accessing gateway resource: " +
                        gatewayURL.toExternalForm(), ex);
            } catch (IntrospectionException ex) {
                throw ex;
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Ivalid gateway resource: " +
                        gatewayURL.toExternalForm(), ex);
                if (deleteOnFail) {
                    try {
                        logger.log(Level.FINE, "Deleting invalid gateway file: {0}", gatewayURL);
                        File gwFile = new File(gatewayURL.toURI());
                        gwFile.delete();
                    } catch (Exception exc) {
                        logger.log(Level.WARNING, "Can't delete gateway: " + gatewayURL, exc);
                    }
                }
            }
        }

        logger.log(Level.FINER, "Imported {0} gateways", gateways.size());
        return gateways;
    }

    /** Get set of deprecated gateways from jar resource
     * @param resource jar absolute resource path with xml file
     * @see #importDeprecatedGateways(java.net.URL)
     */
    public static HashSet<DeprecatedGateway> importDeprecatedGateways(String resource) throws
            IOException, SAXException {
        logger.finer("Importing deprecated gateways from resource: " + resource);
        URL deprecFile = ImportManager.class.getResource(resource);
        if (deprecFile == null || //resource doesn't exist
                !deprecFile.getProtocol().equals("jar")) { //resource not packed in jar
            throw new IOException("Could not find jar gateway resource: " + resource);
        }

        return importDeprecatedGateways(deprecFile);
    }

    /** Get set of deprecated gateways from a file
     * @param file xml containing description of deprecated gateways
     * @see #importDeprecatedGateways(java.net.URL) 
     */
    public static HashSet<DeprecatedGateway> importDeprecatedGateways(File file)
            throws IOException, SAXException {
        logger.finer("Importing deprecated gateways from file: " + file.getAbsolutePath());

        if (!file.canRead() || !file.isFile()) {
            throw new IOException("Invalid deprecated gateway file: " + file.getAbsolutePath());
        }
        
        return importDeprecatedGateways(file.toURI().toURL());
    }

    /** Get set of deprecated gateways from URL
     * @param url url to xml file (file or jar url) containing description
     * of deprecated gateways
     * @return set of deprecated gateways
     * @throws IOException problem accessing jar resource
     * @throws SAXException problem parsing the file
     */
    public static HashSet<DeprecatedGateway> importDeprecatedGateways(URL url) throws
            IOException, SAXException {
        logger.finer("Importing deprecated gateways from URL: " + url);

        HashSet<DeprecatedGateway> deprecated = new HashSet<DeprecatedGateway>();

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            XPathFactory xpf = XPathFactory.newInstance();
            XPath xpath = xpf.newXPath();

            Document doc = db.parse(url.openStream());

            NodeList gateways = doc.getElementsByTagName(VersionFile.TAG_DEPRECATED_GATEWAY);
            for (int i = 0; i < gateways.getLength(); i++) {
                Node gateway = gateways.item(i);
                String name = xpath.evaluate(VersionFile.TAG_NAME + "/text()", gateway);
                String version = xpath.evaluate(VersionFile.TAG_VERSION + "/text()", gateway);
                String reason = xpath.evaluate(VersionFile.TAG_REASON + "/text()", gateway);

                DeprecatedGateway depr = new DeprecatedGateway(name, version, reason);
                deprecated.add(depr);
            }
        } catch (ParserConfigurationException ex) {
            throw new SAXException(ex);
        } catch (XPathExpressionException ex) {
            throw new SAXException(ex);
        }

        return deprecated;
    }

    /** Import keyring data from file.
     * @param file File to import from.
     * @throws Exception When some error occur during file processing.
     */
    public static void importKeyring(File file) throws Exception {
        logger.finer("Importing keyring from file: " + file.getAbsolutePath());
        Keyring keyring = Keyring.getInstance();
        CsvReader reader = null;
        
        try {
            reader = new CsvReader(file.getPath(), ',', Charset.forName("UTF-8"));
            reader.setUseComments(true);
            while (reader.readRecord()) {
                try {
                    String gatewayName = reader.get(0);
                    String login = reader.get(1);
                    String password = Keyring.decrypt(reader.get(2));

                    Tuple<String, String> key = new Tuple<String, String>(login, password);
                    keyring.putKey(gatewayName, key);
                } catch (Exception e) {
                    logger.severe("Invalid keyring record: " + reader.getRawRecord());
                    throw e;
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        logger.finer("Imported keyring");
    }

    /** Import configuration from system-wide config into Config defaults */
    public static void importGlobalConfig(File file) throws IOException {
        logger.finer("Importing global configuration from file: " + file.getAbsolutePath());

        GlobalConfig globalConfig = GlobalConfig.getInstance();
        Reader reader = null;

        try {
            reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
            Properties props = new Properties();
            props.load(reader);

            //checkProgramUpdates
            String prop = props.getProperty("announceProgramUpdates");
            if ("yes".equals(prop)) {
                globalConfig.setAnnounceProgramUpdates(true);
            } else if ("no".equals(prop)) {
                globalConfig.setAnnounceProgramUpdates(false);
            }
            // else config default will aply

        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        logger.finer("Imported global configuration");
    }

    /** Import all gateway properties from file. */
    public static void importGatewayProperties(File file) throws IOException {
        logger.log(Level.FINER, "Importing gateway properties from file: {0}", file.getAbsolutePath());

        String jsonString = FileUtils.readFileToString(file, "UTF-8");
        JSONObject obj = JSONObject.fromObject(jsonString);

        // default signature
        JSONObject jsonDefSig = (JSONObject) obj.get("default signature");
        if (jsonDefSig != null) {
            try {
                Signature defSig = (Signature) JSONObject.toBean(jsonDefSig, Signature.class);
                Signature.DEFAULT.setUserName(defSig.getUserName());
                Signature.DEFAULT.setUserNumber(defSig.getUserNumber());
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Could not load default signature", ex);
            }
        }

        // custom signatures
        JSONArray jsonSignatures = (JSONArray) obj.get("signatures");
        if (jsonSignatures != null) {
            try {
                for (int i = 0; i < jsonSignatures.size(); i++) {
                    JSONObject sigObj = jsonSignatures.getJSONObject(i);
                    Signature signature = (Signature) JSONObject.toBean(sigObj, Signature.class);
                    Signatures.getInstance().add(signature);
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Could not load user signatures", ex);
            }
        }

        // GatewayConfig objects
        JSONObject jsonGwConfigs = (JSONObject) obj.get("configs");
        if (jsonGwConfigs != null) {
            try {
                for (Iterator it = jsonGwConfigs.keys(); it.hasNext(); ) {
                    String gwName = (String) it.next();
                    GatewayConfig gwConfig = (GatewayConfig) JSONObject.toBean(jsonGwConfigs.getJSONObject(gwName), GatewayConfig.class);

                    Gateway gateway = Gateways.getInstance().get(gwName);
                    if (gateway != null) {
                        gateway.setConfig(gwConfig);
                    }
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Could not load gateway configs", ex);
            }
        }

        logger.finer("Imported gateway properties");
    }
}
