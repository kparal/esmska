/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.transfer;

import esmska.data.Config;
import esmska.data.Keyring;
import esmska.data.Operator;
import esmska.data.Operators;
import esmska.data.SMS;
import esmska.data.Tuple;
import esmska.utils.L10N;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/** Class that takes care of parsing operator script files for extracting 
 * operator info and sending messages.
 * 
 * @author ripper
 */
public class OperatorInterpreter {

    private static final Logger logger = Logger.getLogger(OperatorInterpreter.class.getName());
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final ScriptEngineManager manager = new ScriptEngineManager();
    private static final Keyring keyring = Keyring.getInstance();
    private static final Config config = Config.getInstance();
    private Map<OperatorVariable, String> variables;
    private OperatorExecutor executor;
    private ScriptEngine engine;
    private Invocable invocable;
    
    /** recreate needed variables before every use */
    private void init() {
        if (engine == null) {
            engine = manager.getEngineByName("js");
            invocable = (Invocable) engine;
        }
        if (engine == null) {
            throw new IllegalStateException("JavaScript execution not supported");
        }
        if (variables == null) {
             variables = new HashMap<OperatorVariable, String>();
        }
    }
    
    /** Send message for provided Operator with provided variables.
     * @param operator Operator, which should be used to send message.
     * @param variables Map of OperatorVariable to String. May contain null values.
     *                  Doesn't have to contain all the keys from OperatorVariable.
     * @return whether the message was sent successfully
     */
    public boolean sendMessage(SMS sms) {
        Operator operator = Operators.getOperator(sms.getOperator());
        this.variables = extractVariables(sms);

        logger.fine("Sending SMS to: " + operator);
        init();
        executor = new OperatorExecutor(sms);
        
        if (operator == null) {
            executor.setErrorMessage(l10n.getString("OperatorInterpreter.unknown_operator"));
            return false;
        }
        
        Reader reader = null;
        boolean sentOk = false;
        try {
            reader = new InputStreamReader(operator.getScript().openStream(), "UTF-8");
            
            //set preferred language
            String language = getPreferredLanguage(operator);
            executor.setPreferredLanguage(language);
            
            //forward variables to the script and evaluate it
            forwardVariables();
            engine.eval(reader);

            //send the message
            sentOk = (Boolean) invocable.invokeFunction("send", new Object[0]);
            logger.fine("SMS sent ok: " + sentOk);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error executing operator script file", ex);
            executor.setErrorMessage(OperatorExecutor.ERROR_UKNOWN);
            return false;
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Error closing operator script file", ex);
            }
        }

        return sentOk;
    }

    /** Extract variables from SMS to a map */
    private static HashMap<OperatorVariable,String> extractVariables(SMS sms) {
        HashMap<OperatorVariable,String> map = new HashMap<OperatorVariable, String>();
        map.put(OperatorVariable.NUMBER, sms.getNumber());
        map.put(OperatorVariable.MESSAGE, sms.getText());
        map.put(OperatorVariable.SENDERNAME, sms.getSenderName());
        map.put(OperatorVariable.SENDERNUMBER, sms.getSenderNumber());

        Tuple<String, String> key = keyring.getKey(sms.getOperator());
        if (key != null) {
            map.put(OperatorVariable.LOGIN, key.get1());
            map.put(OperatorVariable.PASSWORD, key.get2());
        }

        if (config.isDemandDeliveryReport()) {
            map.put(OperatorVariable.DELIVERY_REPORT, "true");
        }

        return map;
    }

    /** Forward all the declared variables into the script.
     * All variable values are transformed to the x-www-form-urlencoded format.
     */
    private void forwardVariables() {
        for (OperatorVariable var : OperatorVariable.values()) {
            String value = variables.get(var);
            engine.put(var.toString(), value != null ? value : "");
        }

        engine.put("EXEC", executor);
    }
    
    /** Compute preffered language to retrieve web content based on user default
     * language and set of supported languages by operator script.
     * @return two-letter language code as defined in ISO 639-1
     */
    private String getPreferredLanguage(Operator operator) {
        List<String> languages = Arrays.asList(operator.getSupportedLanguages());
        String defLang = Locale.getDefault().getLanguage();
        if (languages.isEmpty() || languages.contains(defLang)) {
            return defLang;
        } else {
            return languages.get(0);
        }
    }

    /** Get the error message created when sending of the message failed. May be null.
     * @throws IllegalStateException when called before sending any sms
     */
    public String getErrorMessage() {
        if (executor == null) {
            throw new IllegalStateException("Getting error message before even sending the very sms");
        }
        return executor.getErrorMessage();
    }
    
    /** Get additional message from operator. May be null.
     * @throws IllegalStateException when called before sending any sms
     */
    public String getOperatorMessage() {
        if (executor == null) {
            throw new IllegalStateException("Getting operator message before even sending the very sms");
        }
        return executor.getOperatorMessage();
    }
}
