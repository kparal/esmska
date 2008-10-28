/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.operators;

import esmska.data.Icons;
import java.beans.IntrospectionException;
import java.io.IOException;
import java.net.URL;
import java.security.PrivilegedActionException;
import java.text.Collator;
import javax.script.ScriptException;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/** Default implementation of the Operator interface.
 * This implementation caches all information retrieved from operator script
 * in order to reduce the performance impact caused by javascript invocations.
 * @author ripper
 */
public class DefaultOperator implements Operator {

    private static final OperatorInterpreter interpreter = new OperatorInterpreter();
    private URL script;
    private String name, version, maintainer, countryPrefix;
    private String[] operatorPrefixes, supportedLanguages;
    private int smsLength,  maxParts,  maxChars,  signatureExtraLength, 
            delayBetweenMessages;
    private Icon icon;
    private boolean loginRequired;

    /** Creates new DefaultOperator.
     * 
     * @param script system resource containing operator script
     * @throws IOException When there are problem accessing the script file
     * @throws ScriptException When operator script is invalid
     * @throws PrivilegedActionException When operator script is invalid
     * @throws IntrospectionException When current JRE does not support JavaScript execution
     */
    public DefaultOperator(URL script) throws IOException, ScriptException,
            PrivilegedActionException, IntrospectionException {
        this.script = script;
        
        OperatorInfo info = interpreter.parseInfo(script);
        if (info == null || info.getName() == null || info.getName().length() <= 0) {
            throw new ScriptException("Not a valid operator script", script.toExternalForm(), 0);
        }
        
        //remember all the values from OperatorInfo interface internally in order
        //to increase speed (Java code vs JavaScript execution for every method access).
        name = info.getName();
        version = info.getVersion();
        maintainer = info.getMaintainer();
        countryPrefix = info.getCountryPrefix();
        operatorPrefixes = info.getOperatorPrefixes();
        smsLength = info.getSMSLength();
        maxParts = info.getMaxParts();
        maxChars = info.getMaxChars();
        signatureExtraLength = info.getSignatureExtraLength();
        delayBetweenMessages = info.getDelayBetweenMessages();
        loginRequired = info.isLoginRequired();
        supportedLanguages = info.getSupportedLanguages();

        //find icon - for "[xx]abc.operator" look for "[xx]abc.png"
        String iconName = script.toExternalForm().replaceFirst("\\.operator$", ".png");
        URL iconURL = new URL(iconName);
        icon = new ImageIcon(iconURL);
        if (icon.getIconWidth() <= 0) { //non-existing icon, zero-sized image
            icon = Icons.OPERATOR_DEFAULT;
        }
    }

    @Override
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

    @Override
    public URL getScript() {
        return script;
    }

    @Override
    public Icon getIcon() {
        return icon;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getMaintainer() {
        return maintainer;
    }
    
    @Override
    public String getCountryPrefix() {
        return countryPrefix;
    }

    @Override
    public String[] getOperatorPrefixes() {
        return operatorPrefixes;
    }

    @Override
    public int getSMSLength() {
        return smsLength;
    }

    @Override
    public int getMaxParts() {
        return maxParts;
    }

    @Override
    public int getMaxChars() {
        return maxChars;
    }

    @Override
    public int getSignatureExtraLength() {
        return signatureExtraLength;
    }

    @Override
    public int getDelayBetweenMessages() {
        return delayBetweenMessages;
    }

    @Override
    public boolean isLoginRequired() {
        return loginRequired;
    }

    @Override
    public String[] getSupportedLanguages() {
        return supportedLanguages;
    }
}
