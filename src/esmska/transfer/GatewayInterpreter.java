package esmska.transfer;

import esmska.data.Config;
import esmska.data.Keyring;
import esmska.data.Gateway;
import esmska.data.Gateways;
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

/** Class that takes care of parsing gateway script files for extracting
 * gateway info and sending messages.
 * 
 * @author ripper
 */
public class GatewayInterpreter {

    private static final Logger logger = Logger.getLogger(GatewayInterpreter.class.getName());
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final ScriptEngineManager manager = new ScriptEngineManager();
    private static final Keyring keyring = Keyring.getInstance();
    private static final Config config = Config.getInstance();
    private Map<GatewayVariable, String> variables;
    private GatewayExecutor executor;
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
             variables = new HashMap<GatewayVariable, String>();
        }
    }
    
    /** Send a message
     * @param sms sms to be sent
     * @return whether the message was sent successfully
     */
    public boolean sendMessage(SMS sms) {
        Gateway gateway = Gateways.getInstance().get(sms.getGateway());
        logger.log(Level.FINE, "Sending SMS to: {0}", gateway);
        
        init();
        executor = new GatewayExecutor(sms);
        if (gateway == null) {
            executor.setErrorMessage(l10n.getString("GatewayInterpreter.unknown_gateway"));
            return false;
        }

        this.variables = extractVariables(sms, gateway);
        
        Reader reader = null;
        boolean sentOk = false;
        try {
            reader = new InputStreamReader(gateway.getScript().openStream(), "UTF-8");
            
            //set preferred language
            String language = getPreferredLanguage(gateway);
            executor.setPreferredLanguage(language);
            
            //forward variables to the script and evaluate it
            forwardVariables();
            engine.eval(reader);

            //send the message
            sentOk = (Boolean) invocable.invokeFunction("send", new Object[0]);
            logger.log(Level.FINE, "SMS sent ok: {0}", sentOk);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error executing gateway script file " + gateway, ex);
            // setting ERROR_UNKNOWN will also log last webpage content
            executor.setErrorMessage(GatewayExecutor.ERROR_UNKNOWN);
            return false;
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Error closing gateway script file " + gateway, ex);
            }
        }

        return sentOk;
    }

    /** Extract variables from SMS to a map */
    private static HashMap<GatewayVariable,String> extractVariables(SMS sms, Gateway gateway) {
        HashMap<GatewayVariable,String> map = new HashMap<GatewayVariable, String>();
        map.put(GatewayVariable.NUMBER, sms.getNumber());
        map.put(GatewayVariable.MESSAGE, sms.getText());
        map.put(GatewayVariable.SENDERNAME, sms.getSenderName());
        map.put(GatewayVariable.SENDERNUMBER, sms.getSenderNumber());
        
        Tuple<String, String> key = keyring.getKey(sms.getGateway());
        if (key != null) {
            map.put(GatewayVariable.LOGIN, key.get1());
            map.put(GatewayVariable.PASSWORD, key.get2());
        }

        if (gateway.getConfig().isReceipt()) {
            map.put(GatewayVariable.RECEIPT, "true");
        }

        return map;
    }

    /** Forward all the declared variables into the script.
     * All variable values are transformed to the x-www-form-urlencoded format.
     */
    private void forwardVariables() {
        for (GatewayVariable var : GatewayVariable.values()) {
            String value = variables.get(var);
            engine.put(var.toString(), value != null ? value : "");
        }

        engine.put("EXEC", executor);
    }
    
    /** Compute preffered language to retrieve web content based on user default
     * language and set of supported languages by gateway script.
     * @return two-letter language code as defined in ISO 639-1
     */
    private String getPreferredLanguage(Gateway gateway) {
        List<String> languages = Arrays.asList(gateway.getSupportedLanguages());
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
    
    /** Get additional message from gateway. May be null.
     * @throws IllegalStateException when called before sending any sms
     */
    public String getGatewayMessage() {
        if (executor == null) {
            throw new IllegalStateException("Getting gateway message before even sending the very sms");
        }
        return executor.getGatewayMessage();
    }
}
