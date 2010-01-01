/*
 * ImportManager.java
 *
 * Created on 15. září 2007, 13:02
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package esmska.persistence;

import com.csvreader.CsvReader;
import esmska.data.*;
import esmska.data.Config.GlobalConfig;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.ArrayList;
import esmska.data.Contact;
import esmska.data.SMS;
import esmska.data.DefaultOperator;
import esmska.data.Operator;
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
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
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
    public static ArrayList<SMS> importQueue(File file) throws IOException {
        logger.finer("Importing queue from file: " + file.getAbsolutePath());
        ArrayList<SMS> queue = new ArrayList<SMS>();
        CsvReader reader = null;
        
        try {
            reader = new CsvReader(file.getPath(), ',', Charset.forName("UTF-8"));
            reader.setUseComments(true);
            while (reader.readRecord()) {
                String name = reader.get(0);
                String number = reader.get(1);
                String operator = reader.get(2);
                String text = reader.get(3);
                String senderName = reader.get(4);
                String senderNumber = reader.get(5);

                SMS sms = new SMS(number, text, operator, name, senderNumber,
                        senderName);
                queue.add(sms);
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
    public static ArrayList<History.Record> importHistory(File file)
            throws IOException, ParseException {
        logger.finer("Importing history from file: " + file.getAbsolutePath());
        ArrayList<History.Record> history = new ArrayList<History.Record>();
        CsvReader reader = null;
        
        try {
            reader = new CsvReader(file.getPath(), ',', Charset.forName("UTF-8"));
            reader.setUseComments(true);

            while (reader.readRecord()) {
                String dateString = reader.get(0);
                String name = reader.get(1);
                String number = reader.get(2);
                String operator = reader.get(3);
                String text = reader.get(4);
                String senderName = reader.get(5);
                String senderNumber = reader.get(6);

                DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG,
                        DateFormat.LONG, Locale.ROOT);
                Date date = df.parse(dateString);

                History.Record record = new History.Record(number, text, operator,
                        name, senderNumber, senderName, date);
                history.add(record);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        logger.finer("Imported " + history.size() + " history records");
        return history;
    }

    /** Import all operators from jar resource
     * @param resource jar absolute resource path where to look for operators
     * @throws IOException When there is problem accessing operator directory or files
     * @throws IntrospectionException When current JRE does not support JavaScript execution
     */
    public static TreeSet<Operator> importOperators(String resource) throws
            IOException, IntrospectionException {
        logger.finer("Importing operators from resource: " + resource);
        URL operatorBase = ImportManager.class.getResource(resource);
        if (operatorBase == null || //resource doesn't exist
                !operatorBase.getProtocol().equals("jar")) { //resource not packed in jar
            throw new IOException("Could not find jar operator resource: " + resource);
        }
        HashSet<URL> operatorURLs = new HashSet<URL>();
        
        JarURLConnection con = (JarURLConnection) operatorBase.openConnection();
        for (Enumeration entries = con.getJarFile().entries(); entries.hasMoreElements();) {
            JarEntry entry = (JarEntry) entries.nextElement();
            String name = entry.getName();
            String absoluteName = name.startsWith("/") ? name : ("/" + name);
            if (absoluteName.startsWith(resource) && absoluteName.endsWith(".operator")) {
                operatorURLs.add(new URL("jar:" + con.getJarFileURL() + "!/" + name));
            }
        }

        return importOperators(operatorURLs);
    }

    /** Import all operators from directory
     * @param directory directory where to look for operators
     * @throws IOException When there is problem accessing operator directory or files
     * @throws IntrospectionException When current JRE does not support JavaScript execution
     */
    public static TreeSet<Operator> importOperators(File directory) throws
            IOException, IntrospectionException {
        logger.finer("Importing operators from directory: " + directory.getAbsolutePath());
        if (!directory.canRead() || !directory.isDirectory()) {
            throw new IOException("Invalid operator directory: " + directory.getAbsolutePath());
        }
        HashSet<URL> operatorURLs = new HashSet<URL>();
        
        File[] files = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getAbsolutePath().endsWith(".operator") && pathname.canRead();
            }
        });

        for (File f : files) {
            operatorURLs.add(f.toURI().toURL());
        }

        return importOperators(operatorURLs);
    }

    /** Get set of operators from set of operator URLs
     * @param operatorURLs set of operator URLs (file or jar URLs)
     * @return set of operators
     * @throws java.beans.IntrospectionException When current JRE does not support JavaScript execution
     */
    private static TreeSet<Operator> importOperators(Set<URL> operatorURLs) throws
            IntrospectionException {
        logger.finer("Importing operators from set of " + operatorURLs.size() + " URLs");
        TreeSet<Operator> operators = new TreeSet<Operator>();

        for (URL operatorURL : operatorURLs) {
            try {
                DefaultOperator operator = new DefaultOperator(operatorURL);
                //check that this operator can be used in this program
                if (Config.compareProgramVersions(Config.getLatestVersion(),
                        operator.getMinProgramVersion()) >= 0) {
                    operators.add(operator);
                } else {
                    logger.info("Operator " + operator.getName() +
                            " requires program of version at least " +
                            operator.getMinProgramVersion() + ", skipping.");
                }
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Problem accessing operator resource: " +
                        operatorURL.toExternalForm(), ex);
            } catch (IntrospectionException ex) {
                throw ex;
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Ivalid operator resource: " +
                        operatorURL.toExternalForm(), ex);
            }
        }

        logger.finer("Imported " + operators.size() + " operators");
        return operators;
    }

    /** Get set of deprecated operators from a file
     * @param file xml containing description of deprecated operators
     * @return set of deprecated operators
     * @throws IOException problem accessing the file
     * @throws SAXException problem parsing the file
     */
    public static HashSet<DeprecatedOperator> importDeprecatedOperators(File file)
            throws IOException, SAXException {
        logger.finer("Importing deprecated operators from file: " + file.getAbsolutePath());
        if (!file.canRead() || !file.isFile()) {
            throw new IOException("Invalid deprecated operator file: " + file.getAbsolutePath());
        }

        HashSet<DeprecatedOperator> deprecated = new HashSet<DeprecatedOperator>();

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            XPathFactory xpf = XPathFactory.newInstance();
            XPath xpath = xpf.newXPath();

            Document doc = db.parse(file);

            NodeList operators = doc.getElementsByTagName(VersionFile.TAG_DEPRECATED_OPERATOR);
            for (int i = 0; i < operators.getLength(); i++) {
                Node operator = operators.item(i);
                String name = xpath.evaluate(VersionFile.TAG_NAME + "/text()", operator);
                String version = xpath.evaluate(VersionFile.TAG_VERSION + "/text()", operator);
                String reason = xpath.evaluate(VersionFile.TAG_REASON + "/text()", operator);

                DeprecatedOperator depr = new DeprecatedOperator(name, version, reason);
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
     * @throws java.io.IOException When some error occur during file processing.
     * @throws java.security.GeneralSecurityException When there is problem with
     *         key decryption.
     */
    public static void importKeyring(File file)
            throws IOException, GeneralSecurityException {
        logger.finer("Importing keyring from file: " + file.getAbsolutePath());
        Keyring keyring = Keyring.getInstance();
        CsvReader reader = null;
        
        try {
            reader = new CsvReader(file.getPath(), ',', Charset.forName("UTF-8"));
            reader.setUseComments(true);
            while (reader.readRecord()) {
                String operatorName = reader.get(0);
                String login = reader.get(1);
                String password = Keyring.decrypt(reader.get(2));

                Tuple<String, String> key = new Tuple<String, String>(login, password);
                keyring.putKey(operatorName, key);
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

            //checkUpdatePolicy
            String prop = props.getProperty("checkUpdatePolicy");
            if ("none".equals(prop)) {
                globalConfig.setCheckUpdatePolicy(Config.CheckUpdatePolicy.CHECK_NONE);
            } else if ("program".equals(prop)) {
                globalConfig.setCheckUpdatePolicy(Config.CheckUpdatePolicy.CHECK_PROGRAM);
            } else if ("gateways".equals(prop)) {
                globalConfig.setCheckUpdatePolicy(Config.CheckUpdatePolicy.CHECK_GATEWAYS);
            } else if ("all".equals(prop)) {
                globalConfig.setCheckUpdatePolicy(Config.CheckUpdatePolicy.CHECK_ALL);
            }

        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        logger.finer("Imported global configuration");
    }
}
