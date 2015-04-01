package esmska.update;

import com.csvreader.CsvReader;
import esmska.Context;
import esmska.data.Config;
import esmska.data.CountryPrefix;
import esmska.data.Keyring;
import esmska.data.Signature;
import esmska.data.Signatures;
import esmska.data.Tuple;
import esmska.persistence.ContinuousSaveManager;
import esmska.persistence.PersistenceManager;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;

/** Class for updating from older to newer versions of the program.
 * Makes the needed changes when user updates his version.
 * @author ripper
 */
public class LegacyUpdater {

    private static final Logger logger = Logger.getLogger(LegacyUpdater.class.getName());

    /** Checks if some update is needed to be done and executes it if neccessary */
    public static void update() throws Exception {
        String version = Config.getInstance().getVersion();
        if (StringUtils.isEmpty(version)) {
            //program is started for the first time, no update neccessary
            return;
        }
        if (ObjectUtils.equals(version, Config.getLatestVersion())) { //already updated
            return;
        }
        logger.log(Level.INFO, "Updating from legacy version {0} to current version {1}", 
                new Object[]{version, Config.getLatestVersion()});

        //changes to 0.8.0
        if (Config.compareProgramVersions(version, "0.8.0") < 0) {
            //set country prefix from locale
            if (StringUtils.isEmpty(Config.getInstance().getCountryPrefix())) {
                Config.getInstance().setCountryPrefix(
                        CountryPrefix.getCountryPrefix(Locale.getDefault().getCountry()));
            }
        }

        //changes to 0.17.0
        if (Config.compareProgramVersions(version, "0.16.0") <= 0) {
            //keyring encryption changed from AES to XOR
            logger.fine("Updating keyring file to newer encryption...");
            try {
                byte[] passphrase = new byte[]{
                    -53, -103, 123, -53, -119, -12, -27, -82,
                    3, -115, 119, -101, 86, 92, 92, 28
                };
                SecretKeySpec keySpec = new SecretKeySpec(passphrase, "AES");
                String CIPHER_TRANSFORMATION = "AES/ECB/PKCS5Padding";
                Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
                cipher.init(Cipher.DECRYPT_MODE, keySpec);

                Field keyringFileField = PersistenceManager.class.getDeclaredField("keyringFile");
                keyringFileField.setAccessible(true);
                File keyringFile = (File) keyringFileField.get(null);

                ContinuousSaveManager.disableKeyring();
                Keyring keyring = Keyring.getInstance();
                keyring.clearKeys();

                CsvReader reader = new CsvReader(keyringFile.getPath(), ',', Charset.forName("UTF-8"));
                reader.setUseComments(true);
                while (reader.readRecord()) {
                    String gatewayName = reader.get(0);
                    String login = reader.get(1);
                    String password = reader.get(2);

                    byte[] ciphertext = Base64.decodeBase64(password.getBytes("UTF-8"));
                    byte[] cleartext = cipher.doFinal(ciphertext);
                    password = new String(cleartext, "UTF-8");

                    Tuple<String, String> key = new Tuple<String, String>(login, password);
                    keyring.putKey(gatewayName, key);
                }
                reader.close();

                ContinuousSaveManager.enableKeyring();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Updating keyring file failed", ex);
            }
        }
        
        //changes to 0.22.0
        if (Config.compareProgramVersions(version, "0.21") <= 0) {
            //transfer senderName and senderNumber settings
            Field configFileField = PersistenceManager.class.getDeclaredField("configFile");
            configFileField.setAccessible(true);
            File configFile = (File) configFileField.get(null);
            
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            XPathFactory xpf = XPathFactory.newInstance();
            XPath xpath = xpf.newXPath();
            
            Document doc = db.parse(configFile);
            String senderNumber = xpath.evaluate("//void[@property='senderNumber']/string", doc);
            String senderName = xpath.evaluate("//void[@property='senderName']/string", doc);
            
            Signature defaultSig = Signatures.getInstance().get(Signature.DEFAULT.getProfileName());
            if (StringUtils.isNotEmpty(senderName)) {
                defaultSig.setUserName(senderName);
            }
            if (StringUtils.isNotEmpty(senderNumber)) {
                defaultSig.setUserNumber(senderNumber);
            }
        }
        
        //changes to 1.4
        if (Config.compareProgramVersions(version, "1.4") < 0) {
            //add message ID to queue
            logger.fine("Updating queue to add message IDs...");
            try {
                Field queueFileField = PersistenceManager.class.getDeclaredField("queueFile");
                queueFileField.setAccessible(true);
                File queueFile = (File) queueFileField.get(null);

                List<String> lines = FileUtils.readLines(queueFile, "UTF-8");
                ArrayList<String> newLines = new ArrayList<String>();
                for (String line : lines) {
                    newLines.add(line + ",");
                }
                FileUtils.writeLines(queueFile, "UTF-8", newLines);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Updating queue file failed", ex);
            }
        }

        //changes to 1.7
        if (Config.compareProgramVersions(version, "1.6") <= 0) {
            // change signature suffix to signature prefix -> append a colon
            // to the signature
            logger.fine("Updating signature suffix to prefix...");
            Context.persistenceManager.loadGateways();
            Context.persistenceManager.loadGatewayProperties();
            ArrayList<Signature> sigList = new ArrayList<Signature>();
            sigList.addAll(Signatures.getInstance().getAll());
            sigList.addAll(Signatures.getInstance().getSpecial());
            for (Signature signature : sigList) {
                String userName = signature.getUserName();
                if (StringUtils.isNotEmpty(userName)) {
                    signature.setUserName(userName + ":");
                }
            }
            Context.persistenceManager.saveGatewayProperties();
        }
        if (Config.compareProgramVersions(version, "1.6.99") <= 0) {
            //add message ID to history
            logger.fine("Updating history to add sms IDs...");
            try {
                Field queueFileField = PersistenceManager.class.getDeclaredField("historyFile");
                queueFileField.setAccessible(true);
                File queueFile = (File) queueFileField.get(null);
                List<String> lines = FileUtils.readLines(queueFile, "UTF-8");
                ArrayList<String> newLines = new ArrayList<String>();
                for (String line : lines) {
                    newLines.add(line + ",");
                }
                FileUtils.writeLines(queueFile, "UTF-8", newLines);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Updating history file failed", ex);
            }
        }
    }
}
