/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.update;

import com.csvreader.CsvReader;
import esmska.data.Config;
import esmska.data.CountryPrefix;
import esmska.data.Keyring;
import esmska.data.Tuple;
import esmska.persistence.ContinuousSaveManager;
import esmska.persistence.PersistenceManager;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

/** Class for updating from older to newer versions of the program.
 * Makes the needed changes when user updates his version.
 * @author ripper
 */
public class LegacyUpdater {

    private static final Logger logger = Logger.getLogger(LegacyUpdater.class.getName());

    /** Checks if some update is needed to be done a executes it if neccessary */
    public static void update() {
        String version = Config.getInstance().getVersion();
        if (version == null) {
            return;
        }
        if (ObjectUtils.equals(version, Config.getLatestVersion())) { //already updated
            return;
        }
        logger.info("Updating from legacy version " + version + " to current version " +
                Config.getLatestVersion());

        //changes to 0.8.0
        if (Config.compareProgramVersions(version, "0.8.0") < 0) {
            //set country prefix from locale
            if (StringUtils.isEmpty(Config.getInstance().getCountryPrefix())) {
                Config.getInstance().setCountryPrefix(
                        CountryPrefix.getCountryPrefix(Locale.getDefault().getCountry()));
            }
        }

        //changes to 0.17.0
        if (Config.compareProgramVersions(version, "0.17.0") < 0) {
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
                    String operatorName = reader.get(0);
                    String login = reader.get(1);
                    String password = reader.get(2);

                    byte[] ciphertext = Base64.decodeBase64(password.getBytes("UTF-8"));
                    byte[] cleartext = cipher.doFinal(ciphertext);
                    password = new String(cleartext, "UTF-8");

                    Tuple<String, String> key = new Tuple<String, String>(login, password);
                    keyring.putKey(operatorName, key);
                }
                reader.close();

                ContinuousSaveManager.enableKeyring();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Updating keyring file failed", ex);
            }
        }
    }
}
