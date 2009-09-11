/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.data;

import esmska.data.event.ActionEventSupport;
import java.awt.event.ActionListener;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.ObjectUtils;

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
    /** randomly generated passphrase */
    private static final byte[] passphrase = new byte[]{
        -47, 12, -115, -66, 28, 102, 93, 101,
        -98, -87, 96, -11, -72, 117, -39, 39,
        102, 73, -122, 91, -14, -118, 5, -82,
        -126, 3, 38, -19, -63, -127, 46, -82,
        27, -38, -89, 29, 10, 81, -108, 17,
        -96, -71, 120, 63, -128, -3, -3, -63,
        65, -40, 109, 70, 69, -122, 80, -83,
        37, -45, 61, 60, -12, -101, 0, -126,
        44, -125, -83, 47, -48, -7, 8, 16,
        127, 25, -1, -23, 27, -78, 124, 36,
        59, 52, -66, 40, -31, -7, 111, -101,
        -5, 85, -65, -90, -56, -51, 53, 44,
        20, 15, 111, 37, -97, 120, -60, 53,
        -80, 69, 34, 109, -71, 101, -66, 77,
        52, -14, 112, 112, 97, 12, -76, -96,
        -101, 103, -59, 38, -24, -10, -85, -119
    };
    /** map of [operator, [login, password]] */
    private final Map<String, Tuple<String, String>> keyring = Collections.synchronizedMap(
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
    public Tuple<String, String> getKey(String operatorName) {
        return keyring.get(operatorName);
    }

    /** Put key for chosen operator. If a key for this operator already exists,
     * overwrite previous one.
     * @param operatorName Name of the operator.
     * @param key tuple in the form [login, password].
     * @throws IllegalArgumentException If operatorName or key is null.
     */
    public void putKey(String operatorName, Tuple<String, String> key) {
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
        return previous == null || !ObjectUtils.equals(previous, key);
    }

    /** Put keys for more operators. If a key for particular operator already exists,
     * overwrite previous one.
     * @param keys Map in the form [operatorName, Key], where Key is in the
     *             form [login, password].
     * @throws IllegalArgumentException If some operatorName or some key is null.
     */
    public void putKeys(Map<String, Tuple<String, String>> keys) {
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
    public void removeKey(String operatorName) {
        if (keyring.remove(operatorName) != null) {
            logger.finer("A keyring key removed: [operatorName=" + operatorName + "]");
            actionSupport.fireActionPerformed(ACTION_REMOVE_KEY, null);
        }
    }

    /** Get set of all operator names, which are in the keyring.
     * @return Unmodifiable set of all operator names, which are in the keyring.
     */
    public Set<String> getOperatorNames() {
        return Collections.unmodifiableSet(keyring.keySet());
    }

    /** Clear all operator names and corresponding keys from the keyring. 
     * The keyring will be empty after this.
     */
    public void clearKeys() {
        keyring.clear();
        logger.finer("All keyring keys removed");
        actionSupport.fireActionPerformed(ACTION_CLEAR_KEYS, null);
    }

    /** Encrypt input string. The string is encrypted using XOR encryption with
     * internal passphrase, doubled and the result is encoded using the Base64 encoding.
     * @param input Input string. Null is transformed to empty string.
     * @return Encrypted string 
     */
    public static String encrypt(String input) {
        if (input == null) {
            input = "";
        }

        try {
            byte[] inputArray = input.getBytes("UTF-8");
            byte[] encrArray = new byte[inputArray.length*2];

            for (int i = 0; i < inputArray.length; i++) {
                byte k = i < passphrase.length ? passphrase[i] : 0;
                encrArray[2*i] = (byte) (inputArray[i] ^ k);
                //let's double the string, if hides too short password lengths
                encrArray[2*i+1] = encrArray[2*i];
            }

            String encrString = new String(Base64.encodeBase64(encrArray), "US-ASCII");
            return encrString;
        } catch (UnsupportedEncodingException ex) {
            assert false : "Basic charsets must be supported";
            throw new IllegalStateException("Basic charsets must be supported", ex);
        }
    }

    /** Decrypt input string. The input string is decoded using the Base64 encoding,
     * halved, and the result is decrypted using XOR encryption with internal passphrase.
     * @param input Input string. The input must originate from the encrypt() function.
     *              Null is transformed to empty string.
     * @return Decrypted string.
     */
    public static String decrypt(String input) {
        if (input == null) {
            input = "";
        }

        try {
            byte[] encrArray = Base64.decodeBase64(input.getBytes("US-ASCII"));
            byte[] decrArray = new byte[encrArray.length/2];

            for (int i = 0; i < encrArray.length; i+=2) {
                byte k = i/2 < passphrase.length ? passphrase[i/2] : 0;
                //array must be halved, encrypted is doubled
                decrArray[i/2] = (byte) (encrArray[i] ^ k);
            }

            String decrString = new String(decrArray, "UTF-8");
            return decrString;
        } catch (UnsupportedEncodingException ex) {
            assert false : "Basic charsets must be supported";
            throw new IllegalStateException("Basic charsets must be supported", ex);
        }
    }
}
