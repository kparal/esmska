/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.transfer;

import esmska.data.SMS;
import org.apache.commons.lang.Validate;

/** Default implementation of image resolving. Returns empty result all the time.
 *
 * @author ripper
 */
public class DefaultImageCodeResolver implements ImageCodeResolver {

    /** For description see {@link ImageCodeResolver#resolveImageCode(esmska.data.SMS)}.
     * This implementation always returns false and null as security code.
     * @return false
     */
    @Override
    public boolean resolveImageCode(SMS sms) {
        Validate.notNull(sms);
        sms.setImageCode(null);
        return false;
    }

}
