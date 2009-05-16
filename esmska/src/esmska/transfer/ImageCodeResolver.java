/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.transfer;

import esmska.data.SMS;

/** Security image resolver. Takes an sms with a security image and provides
 * textual representation of the security image, if it is possible.
 *
 * @author ripper
 */
public interface ImageCodeResolver {
    /** Resolve security image and provide security code in textual form if
     * possible. The security image is extracted from the provided sms (may be
     * null) and the result is stored also in the sms. The resulting code may
     * be null or empty if the resolution was cancelled or did not succeed.
     * @param sms sms for which to resolve the security image; not null
     * @return true if resolution was successful, false otherwise (did not
     * succeed or was cancelled)
     */
    boolean resolveImageCode(SMS sms);
}
