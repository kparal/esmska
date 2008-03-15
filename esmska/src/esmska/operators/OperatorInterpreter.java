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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
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
    
    private void init() {
        engine = manager.getEngineByName("js");
        invocable = (Invocable) engine;
        executor = new OperatorExecutor();
        if (variables == null)
             variables = new HashMap<OperatorVariable, String>();
    }
    
    public OperatorInfo parseInfo(File file) {
        init();
        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
            engine.eval(reader);
            OperatorInfo operatorInfo = invocable.getInterface(OperatorInfo.class);
            return operatorInfo;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error exploring operator script file", ex);
            return null;
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Error closing operator script file", ex);
            }
        }
    }

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
            
            forwardVariables();
            engine.eval(reader);

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

    private void forwardVariables() {
        for (OperatorVariable var : OperatorVariable.values()) {
            String value = variables.get(var);
            engine.put(var.toString(), value != null ? value : "");
        }

        engine.put("EXEC", executor);
    }

    public String getErrorMessage() {
        return executor.getErrorMessage();
    }
}
