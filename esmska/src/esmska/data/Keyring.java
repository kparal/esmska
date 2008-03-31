/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.data;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;

/** Storage for logins and passwords to operators.
 * Also offers password encryption and decryption.
 * @author ripper
 */
public class Keyring {

    /** AES passphrase */
    private static final byte[] passphrase = new byte[]{
        -53, -103, 123, -53, -119, -12, -27, -82,
        3, -115, 119, -101, 86, 92, 92, 28
    };
    private static final SecretKeySpec keySpec = new SecretKeySpec(passphrase, "AES");
    private static final String CIPHER_TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private Map<String, String[]> keyring = Collections.synchronizedMap(
            new HashMap<String, String[]>());

    /** Get key for chosen operator.
     * @param operatorName Name of the operator.
     * @return String array in the form [login, password] if key for this operator
     *         exists. Null otherwise.
     */
    public String[] getKey(String operatorName) {
        return keyring.get(operatorName);
    }

    /** Put key for chosen operator. If a key for this operator already exists,
     * overwrite previous one.
     * @param operatorName Name of the operator.
     * @param key String array in the form [login, password].
     * @throws IllegalArgumentException If operatorName or key is null.
     */
    public void putKey(String operatorName, String[] key) {
        if (operatorName == null) {
            throw new IllegalArgumentException("operatorName");
        }
        if (key == null) {
            throw new IllegalArgumentException("key");
        }

        keyring.put(operatorName, key);
    }

    /** Put keys for more operators. If a key for particular operator already exists,
     * overwrite previous one.
     * @param keys Map in the form &lt;operatorName, Key&gt;, where Key is in the
     *             form [login, password].
     * @throws IllegalArgumentException If some operatorName or some key is null.
     */
    public void putKeys(Map<String, String[]> keys) {
        for (Entry<String, String[]> entry : keys.entrySet()) {
            keyring.put(entry.getKey(), entry.getValue());
        }
    }

    /** Remove chosen operator from the keyring.
     * @param operatorName Name of the operator.
     */
    public void removeKey(String operatorName) {
        keyring.remove(operatorName);
    }

    /** Get set of all operator names, which are in the keyring.
     * @return Set of all operator names, which are in the keyring.
     */
    public Set<String> getOperatorNames() {
        return keyring.keySet();
    }

    /** Clear all operator names and corresponding keys from the keyring. 
     * The keyring will be empty after this.
     */
    public void clearKeys() {
        keyring.clear();
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
