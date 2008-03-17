/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.operators;

import esmska.data.Icons;
import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 *
 * @author ripper
 */
public class DefaultOperator implements Operator {

    private static final OperatorInterpreter interpreter = new OperatorInterpreter();
    private File script;
    private String name;
    private String countryPrefix;
    private String[] operatorPrefixes;
    private int smsLength,  maxParts,  maxChars,  signatureExtraLength;
    private Icon icon;

    public DefaultOperator(File script) throws Exception {
        try {
            this.script = script;
            OperatorInfo info = interpreter.parseInfo(script);
            if (info == null || info.getName() == null || info.getName().length() <= 0) {
                throw new InstantiationException("Not a valid script file.");
            }
            name = info.getName();
            countryPrefix = info.getCountryPrefix() != null ? info.getCountryPrefix() : "";
            operatorPrefixes = info.getOperatorPrefixes();
            smsLength = info.getSMSLength();
            maxParts = info.getMaxParts();
            maxChars = info.getMaxChars();
            signatureExtraLength = info.getSignatureExtraLength();

            String filename = script.getAbsolutePath();
            filename = filename.replaceFirst("\\.operator$", ".png");
            if (new File(filename).exists()) {
                icon = new ImageIcon(filename);
            } else {
                icon = Icons.OPERATOR_BLANK;
            }
        } catch (Exception ex) {
            throw new Exception("Not a valid script file", ex);
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
        if (obj == this)
            return true;
        if (!(obj instanceof Operator))
            return false;
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
