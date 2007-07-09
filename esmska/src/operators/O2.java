/*
 * O2.java
 *
 * Created on 7. červenec 2007, 18:14
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
public class O2 implements Operator {
    
    /**
     * Creates a new instance of O2
     */
    public O2() {
    }

    public URL getSecurityImage() {
        return null;
    }

    public boolean send(SMS sms) {
        return true;
    }
    
    public String toString() {
        return "O2";
    }
}
