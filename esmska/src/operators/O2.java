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
    private final int MAX_CHARS = 60;
    private final int SMS_CHARS = 60;
    
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
    
    public int getMaxChars() {
        return MAX_CHARS;
    }
    
    public int getSMSCount(int chars) {
        return (int)Math.ceil((double)chars / SMS_CHARS);
    }
    
}
