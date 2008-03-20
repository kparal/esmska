/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.operators;

import esmska.data.Icons;
import java.io.File;
import java.io.IOException;
import java.security.PrivilegedActionException;
import java.text.Collator;
import javax.script.ScriptException;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/** Default implementation of the Operator interface.
 *
 * @author ripper
 */
public class DefaultOperator implements Operator {

    private static final OperatorInterpreter interpreter = new OperatorInterpreter();
    private File script;
    private String name, version, author, countryPrefix;
    private String[] operatorPrefixes;
    private int smsLength,  maxParts,  maxChars,  signatureExtraLength;
    private Icon icon;

    /** Creates new DefaultOperator.
     * 
     * @param script operator script
     * @throws IOException When there are problem accessing the script file
     * @throws ScriptException When operator script is invalid
     * @throws PrivilegedActionException When operator script is invalid
     */
    public DefaultOperator(File script) throws IOException, ScriptException, PrivilegedActionException {
        this.script = script;
        
        OperatorInfo info = interpreter.parseInfo(script);
        if (info == null || info.getName() == null || info.getName().length() <= 0) {
            throw new ScriptException("Not a valid script file.", script.getAbsolutePath(), 0);
        }
        
        //remember all the values from OperatorInfo interface internally in order
        //to increase speed (Java code vs JavaScript execution for every method access).
        name = info.getName();
        version = info.getVersion();
        author = info.getAuthor();
        countryPrefix = info.getCountryPrefix();
        operatorPrefixes = info.getOperatorPrefixes();
        smsLength = info.getSMSLength();
        maxParts = info.getMaxParts();
        maxChars = info.getMaxChars();
        signatureExtraLength = info.getSignatureExtraLength();

        //find icon - for "[xx]abc.operator" look for "[xx]abc.png"
        String filename = script.getAbsolutePath();
        filename = filename.replaceFirst("\\.operator$", ".png");
        if (new File(filename).exists()) {
            icon = new ImageIcon(filename);
        } else {
            icon = Icons.OPERATOR_DEFAULT;
        }
    }

    public int compareTo(Operator o) {
        Collator collator = Collator.getInstance();
        return collator.compare(this.getName(), o.getName());
    }

    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Operator)) {
            return false;
        }
        Operator o = (Operator) obj;
        return this.compareTo(o) == 0;
    }

    public File getScript() {
        return script;
    }

    public Icon getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getAuthor() {
        return author;
    }
    
    public String getCountryPrefix() {
        return countryPrefix;
    }

    public String[] getOperatorPrefixes() {
        return operatorPrefixes;
    }

    public int getSMSLength() {
        return smsLength;
    }

    public int getMaxParts() {
        return maxParts;
    }

    public int getMaxChars() {
        return maxChars;
    }

    public int getSignatureExtraLength() {
        return signatureExtraLength;
    }
}
