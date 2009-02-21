/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.data;

import esmska.utils.ActionEventSupport;
import esmska.utils.Nullator;
import esmska.utils.Tuple;
import java.awt.event.ActionListener;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;

/** Storage for logins and passwords to operators.
 * Also offers password encryption and decryption.
 * @author ripper
 */
public class Keyring {

    /** shared instance */
    private static final Keyring instance = new Keyring();

    /** new key added or existing changed */
    public static final int ACTION_ADD_KEY = 0;
    /** existing key removed */
    public static final int ACTION_REMOVE_KEY = 1;
    /** all keys removed */
    public static final int ACTION_CLEAR_KEYS = 2;

    private static final Logger logger = Logger.getLogger(Keyring.class.getName());

    /** AES passphrase */
    private static final byte[] passphrase = new byte[]{
        -53, -103, 123, -53, -119, -12, -27, -82,
        3, -115, 119, -101, 86, 92, 92, 28
    };
    private static final SecretKeySpec keySpec = new SecretKeySpec(passphrase, "AES");
    private static final String CIPHER_TRANSFORMATION = "AES/ECB/PKCS5Padding";
    /** map of [operator, [login, password]] */
    private Map<String, Tuple<String, String>> keyring = Collections.synchronizedMap(
            new HashMap<String, Tuple<String, String>>());

    // <editor-fold defaultstate="collapsed" desc="ActionEvent support">
    private ActionEventSupport actionSupport = new ActionEventSupport(this);
    public void addActionListener(ActionListener actionListener) {
        actionSupport.addActionListener(actionListener);
    }

    public void removeActionListener(ActionListener actionListener) {
        actionSupport.removeActionListener(actionListener);
    }
    // </editor-fold>

    /** Disabled constructor */
    private Keyring() {
    }

    /** Get shared instance */
    public static Keyring getInstance() {
        return instance;
    }

    /** Get key for chosen operator.
     * @param operatorName Name of the operator.
     * @return tuple in the form [login, password] if key for this operator
     *         exists. Null otherwise.
     */
    public synchronized Tuple<String, String> getKey(String operatorName) {
        return keyring.get(operatorName);
    }

    /** Put key for chosen operator. If a key for this operator already exists,
     * overwrite previous one.
     * @param operatorName Name of the operator.
     * @param key tuple in the form [login, password].
     * @throws IllegalArgumentException If operatorName or key is null.
     */
    public synchronized void putKey(String operatorName, Tuple<String, String> key) {
        if (putKeyImpl(operatorName, key)) {
            logger.finer("New keyring key added: [operatorName=" + operatorName + "]");
            actionSupport.fireActionPerformed(ACTION_ADD_KEY, null);
        }
    }

    /** Inner execution code for putKey method
     * @return true if keyring was updated (key was not present or was modified
     * by the update); false if nothing has changed
     */
    private boolean putKeyImpl(String operatorName, Tuple<String, String> key) {
        if (operatorName == null) {
            throw new IllegalArgumentException("operatorName");
        }
        if (key == null) {
            throw new IllegalArgumentException("key");
        }
        Tuple<String, String> previous = keyring.put(operatorName, key);
        return previous == null || !Nullator.isEqual(previous, key);
    }

    /** Put keys for more operators. If a key for particular operator already exists,
     * overwrite previous one.
     * @param keys Map in the form [operatorName, Key], where Key is in the
     *             form [login, password].
     * @throws IllegalArgumentException If some operatorName or some key is null.
     */
    public synchronized void putKeys(Map<String, Tuple<String, String>> keys) {
        int changed = 0;
        for (Entry<String, Tuple<String, String>> entry : keys.entrySet()) {
            changed += putKeyImpl(entry.getKey(), entry.getValue()) ? 1 : 0;
        }
        if (changed > 0) {
            logger.finer(changed + " new keyring keys added");
            actionSupport.fireActionPerformed(ACTION_ADD_KEY, null);
        }
    }

    /** Remove chosen operator from the keyring.
     * @param operatorName Name of the operator.
     */
    public synchronized void removeKey(String operatorName) {
        if (keyring.remove(operatorName) != null) {
            logger.finer("A keyring key removed: [operatorName=" + operatorName + "]");
            actionSupport.fireActionPerformed(ACTION_REMOVE_KEY, null);
        }
    }

    /** Get set of all operator names, which are in the keyring.
     * @return Unmodifiable set of all operator names, which are in the keyring.
     */
    public synchronized Set<String> getOperatorNames() {
        return Collections.unmodifiableSet(keyring.keySet());
    }

    /** Clear all operator names and corresponding keys from the keyring. 
     * The keyring will be empty after this.
     */
    public synchronized void clearKeys() {
        keyring.clear();
        logger.finer("All keyring keys removed");
        actionSupport.fireActionPerformed(ACTION_CLEAR_KEYS, null);
    }

    /** Encrypt input string. The string is encrypted using AES encryption with
     * internal passphrase and the result is encoded using the Base64 encoding.
     * @param input Input string. Null is transformed to empty string.
     * @return Encrypted string using AES encryption with internal passphrase and
     *         after that encoded using the Base64 encoding.
     * @throws GeneralSecurityException When the platform does not support the
     *                                  particular encryption.
     */
    public static String encrypt(String input) throws GeneralSecurityException {
        if (input == null) {
            input = "";
        }

        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] cleartext = input.getBytes("UTF-8");
            byte[] ciphertext = cipher.doFinal(cleartext);

            return new String(Base64.encodeBase64(ciphertext), "UTF-8");

        } catch (UnsupportedEncodingException ex) {
            throw new GeneralSecurityException(ex);
        } catch (InvalidKeyException ex) {
            throw new GeneralSecurityException("Internal key is invalid", ex);
        }
    }

    /** Decrypt input string. The input string is decoded using the Base64 encoding
     * and the result is decrypted using AES encryption with internal passphrase.
     * @param input Input string. The input must originate from the encrypt() function.
     *              Null is transformed to empty string.
     * @return Decrypted string.
     * @throws GeneralSecurityException When the platform does not support the
     *                                  particular encryption.
     */
    public static String decrypt(String input) throws GeneralSecurityException {
        if (input == null) {
            input = "";
        }

        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] ciphertext = Base64.decodeBase64(input.getBytes("UTF-8"));
            byte[] cleartext = cipher.doFinal(ciphertext);

            return new String(cleartext, "UTF-8");

        } catch (UnsupportedEncodingException ex) {
            throw new GeneralSecurityException(ex);
        } catch (InvalidKeyException ex) {
            throw new GeneralSecurityException("Internal key is invalid", ex);
        }
    }
}
