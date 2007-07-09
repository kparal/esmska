/*
 * Operator.java
 *
 * Created on 7. červenec 2007, 14:43
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package operators;

import esmska.*;
import java.net.URL;

/**
 *
 * @author ripper
 */
public interface Operator {
    /** get image url so user can type the code */
    URL getSecurityImage();
    /** send the sms */
    boolean send(SMS sms);
    /** operator name */
    String toString();
}
