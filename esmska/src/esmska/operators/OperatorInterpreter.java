/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.operators;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
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
        if (variables == null)
             variables = new HashMap<OperatorVariable, String>();
    }
    
    /** Parse OperatorInfo implementation from the provided script file.
     * @return OperatorInfo implementation
     * @throws IOException when there is problem accessing the script file
     * @throws ScriptException when the script is not valid
     */
    public OperatorInfo parseInfo(File file) throws IOException, ScriptException {
        init();
        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
            //the script must be evaluated before extracting the interface
            engine.eval(reader);
            OperatorInfo operatorInfo = invocable.getInterface(OperatorInfo.class);
            return operatorInfo;
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Error closing file " + file.getAbsolutePath(), ex);
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
            executor.setErrorMessage("Neznámý operátor!");
            return false;
        }
        
        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(operator.getScript()), "UTF-8");
            
            //forward variables to the script and evaluate it
            forwardVariables();
            engine.eval(reader);

            //send the message
            invocable.invokeFunction("send", new Object[0]);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error executing operator script file", ex);
            return false;
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Error closing operator script file", ex);
            }
        }

        return executor.getSuccess();
    }

    /** Forward all the declared variables into the script.
     * All variable values are transformed to the x-www-form-urlencoded format.
     */
    private void forwardVariables() throws UnsupportedEncodingException {
        for (OperatorVariable var : OperatorVariable.values()) {
            String value = variables.get(var);
            engine.put(var.toString(), 
                    value != null ? URLEncoder.encode(value, "UTF-8") : "");
        }

        engine.put("EXEC", executor);
    }

    /** Get the error message created when sending of the message failed. May be null. */
    public String getErrorMessage() {
        return executor.getErrorMessage();
    }
}
