/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.transfer;

import esmska.data.Operator;
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
    private Map<OperatorVariable, String> variables;
    private OperatorExecutor executor = new OperatorExecutor(null);
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
    public boolean sendMessage(Operator operator, Map<OperatorVariable, String> variables) {
        logger.fine("Sending SMS to: " + operator);
        this.variables = variables;
        init();
        executor = new OperatorExecutor(operator);
        
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

    /** Get the error message created when sending of the message failed. May be null. */
    public String getErrorMessage() {
        return executor.getErrorMessage();
    }
    
    /** Get additional message from operator. May be null. */
    public String getOperatorMessage() {
        return executor.getOperatorMessage();
    }
}
