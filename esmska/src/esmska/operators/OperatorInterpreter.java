/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.operators;

import esmska.utils.L10N;
import java.beans.IntrospectionException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
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
import javax.script.ScriptException;

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
    private OperatorExecutor executor = new OperatorExecutor();
    private ScriptEngine engine;
    private Invocable invocable;
    
    /** recreate needed variables before every use */
    private void init() {
        engine = manager.getEngineByName("js");
        invocable = (Invocable) engine;
        executor = new OperatorExecutor();
        if (variables == null) {
             variables = new HashMap<OperatorVariable, String>();
        }
    }
    
    /** Parse OperatorInfo implementation from the provided URL.
     * @param script URL (file or jar) of operator script
     * @return OperatorInfo implementation
     * @throws IOException when there is problem accessing the script file
     * @throws ScriptException when the script is not valid
     * @throws IntrospectionException when current JRE does not support JavaScript execution
     */
    public OperatorInfo parseInfo(URL script) throws IOException, ScriptException, IntrospectionException {
        init();
        if (engine == null)
            throw new IntrospectionException("JavaScript execution not supported");
        Reader reader = null;
        try {
            reader = new InputStreamReader(script.openStream(), "UTF-8");
            //the script must be evaluated before extracting the interface
            engine.eval(reader);
            OperatorInfo operatorInfo = invocable.getInterface(OperatorInfo.class);
            return operatorInfo;
        } finally {
            try {
                reader.close();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Error closing script: " + script.toExternalForm(), ex);
            }
        }
    }

    /** Send message for provided Operator with provided variables.
     * @param operator Operator, which should be used to send message.
     * @param variables Map of OperatorVariable to String. May contain null values.
     *                  Doesn't have to contain all the keys from OperatorVariable.
     * @return whether the message was sent successfully
     */
    public boolean sendMessage(Operator operator, Map<OperatorVariable, String> variables) {
        this.variables = variables;
        init();
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
