/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.data;

import esmska.data.event.ValuedEventSupport;
import esmska.data.event.ValuedListener;
import esmska.utils.L10N;
import java.beans.IntrospectionException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.apache.commons.lang.Validate;

/** Class managing all operators
 * @author ripper
 */
public class Operators {

    public static enum Events {
        ADDED_OPERATOR,
        ADDED_OPERATORS,
        REMOVED_OPERATOR,
        REMOVED_OPERATORS,
        CLEARED_OPERATORS
    }

    /** shared instance */
    private static final Operators instance = new Operators();
    private static final Logger logger = Logger.getLogger(Operators.class.getName());
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final SortedSet<Operator> operators = Collections.synchronizedSortedSet(new TreeSet<Operator>());
    private static final Keyring keyring = Keyring.getInstance();
    private static final ScriptEngineManager manager = new ScriptEngineManager();
    private static ScriptEngine jsEngine;

    // <editor-fold defaultstate="collapsed" desc="ValuedEvent support">
    private ValuedEventSupport<Events, Operator> valuedSupport = new ValuedEventSupport<Events, Operator>(this);
    public void addValuedListener(ValuedListener<Events, Operator> valuedListener) {
        valuedSupport.addValuedListener(valuedListener);
    }
    public void removeValuedListener(ValuedListener<Events, Operator> valuedListener) {
        valuedSupport.removeValuedListener(valuedListener);
    }
    // </editor-fold>

    /** Disabled contructor */
    private Operators() {
    }

    /** Get shared instance */
    public static Operators getInstance() {
        return instance;
    }

    /** Get unmodifiable collection of all operators sorted by name */
    public SortedSet<Operator> getAll() {
        return Collections.unmodifiableSortedSet(operators);
    }

    /** Add new operator
     * @param operator new operator, not null
     * @return See {@link Collection#add}
     */
    public boolean add(Operator operator) {
        Validate.notNull(operator);

        logger.fine("Adding new operator: "+ operator);
        boolean added = operators.add(operator);

        if (added) {
            valuedSupport.fireEventOccured(Events.ADDED_OPERATOR, operator);
        }
        return added;
    }

    /** Add new operators
     * @param operators collection of operators, not null, no null element
     * @return See {@link Collection#addAll}
     */
    public boolean addAll(Collection<Operator> operators) {
        Validate.notNull(operators);
        Validate.noNullElements(operators);

        logger.fine("Adding " + operators.size() + " operators: " + operators);
        boolean changed = Operators.operators.addAll(operators);

        if (changed) {
            valuedSupport.fireEventOccured(Events.ADDED_OPERATORS, null);
        }
        return changed;
    }

    /** Remove existing operator
     * @param operator operator to be removed, not null
     * @return See {@link Collection#remove}
     */
    public boolean remove(Operator operator) {
        Validate.notNull(operator);

        logger.fine("Removing operator: " + operator);
        boolean removed = operators.remove(operator);

        if (removed) {
            valuedSupport.fireEventOccured(Events.REMOVED_OPERATOR, operator);
        }
        return removed;
    }

    /** Remove existing operators
     * @param operators collection of operators to be removed, not null, no null element
     * @return See {@link Collection#removeAll}
     */
    public boolean removeAll(Collection<Operator> operators) {
        Validate.notNull(operators);
        Validate.noNullElements(operators);

        logger.fine("Removing " + operators.size() + " operators: " + operators);
        boolean changed = Operators.operators.removeAll(operators);

        if (changed) {
            valuedSupport.fireEventOccured(Events.REMOVED_OPERATORS, null);
        }
        return changed;
    }

    /** Remove all operators */
    public void clear() {
        logger.fine("Removing all operators");
        operators.clear();

        valuedSupport.fireEventOccured(Events.CLEARED_OPERATORS, null);
    }

    /** Search for an existing operator
     * @param operator operator to be searched, not null
     * @return See {@link Collection#contains}
     */
    public boolean contains(Operator operator) {
        Validate.notNull(operator);
        return operators.contains(operator);
    }

    /** Return number of operators
     * @return See {@link Collection#size}
     */
    public int size() {
        return operators.size();
    }

    /** Return if there are no operators
     * @return See {@link Collection#isEmpty}
     */
    public boolean isEmpty() {
        return operators.isEmpty();
    }

    /** Find operator by name.
     * @param name Name of the operator. Search is case sensitive. May be null.
     * @return Operator implementation, when an operator with such name is found.
     *         If multiple such operators are found, returns the first one found.
     *         Returns null if no operator is found or provided name was null.
     */
    public static Operator getOperator(String operatorName) {
        if (operatorName == null) {
            return null;
        }

        synchronized(operators) {
            for (Operator operator : operators) {
                if (operator.getName().equals(operatorName)) {
                    return operator;
                }
            }
        }
        return null;
    }

    /** Guess operator according to phone number or phone number prefix.
     * Searches through operators and finds the best suited one one
     * supporting this phone number.
     *
     * @param number phone number or it's prefix. The minimum length is two characters,
     *  for shorter input (or null) the method does nothing.
     * @param customOperators collection of operators in which to search.
     *  Use null for searching in all currently available operators.
     * @return the suggested operator or null if none found
     */
    public static Operator suggestOperator(String number, Collection<Operator> customOperators) {
        if (number == null || number.length() < 2) {
            return null;
        }

        Collection<Operator> selectedOperators =
                (customOperators != null ? customOperators : operators);
        Operator operator = null;

        //search in operator prefixes
        synchronized(selectedOperators) {
            for (Operator op : selectedOperators) {
                if (matchesWithOperatorPrefix(op, number)) {
                    //prefer operators without login requirements
                    if (op.isLoginRequired()) {
                        if (operator == null) {
                            operator = op;
                        } else if (keyring.getKey(operator.getName()) == null &&
                                keyring.getKey(op.getName()) != null) {
                            //prefer operators with filled in credentials
                            operator = op;
                        }
                    } else {
                        return op;
                    }
                }
            }
        }

        //if no operator without login found, but some operator with login found
        if (operator != null) {
            return operator;
        }

        //search in country prefixes
        synchronized(selectedOperators) {
            for (Operator op : selectedOperators) {
                if (matchesWithCountryPrefix(op, number)) {
                    //prefer operators without login requirements
                    if (op.isLoginRequired()) {
                        if (operator == null) {
                            operator = op;
                        } else if (keyring.getKey(operator.getName()) == null &&
                                keyring.getKey(op.getName()) != null) {
                            //prefer operators with filled in credentials
                            operator = op;
                        }
                    } else {
                        return op;
                    }
                }
            }
        }

        //if no operator without login found, but some operator with login found
        if (operator != null) {
            return operator;
        }

        return null;
    }

    /** Returns whether current operator matches the number with some of
     * it's operator prefix.
     *
     * @param operator operator
     * @param number phone number
     * @return true if current operator matches the number with some of
     * it's operator prefix; false if operator of phone number is null or if
     * phone number is shorter than 2 characters
     */
    public static boolean matchesWithOperatorPrefix(Operator operator, String number) {
        if (operator == null || number == null || number.length() < 2) {
            return false;
        }

        //search in operator prefixes
        for (String prefix : operator.getOperatorPrefixes()) {
            if (number.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    /** Returns whether current operator matches the number with it's country prefix.
     *
     * @param operator operator
     * @param number phone number
     * @return true if current operator matches the number with it's country prefix;
     * false if operator of phone number is null or if phone number is shorter
     * than 2 characters
     */
    public static boolean matchesWithCountryPrefix(Operator operator, String number) {
        if (operator == null || number == null || number.length() < 2) {
            return false;
        }
        return number.startsWith(operator.getCountryPrefix());
    }

    /** Convert message delay to more human readable string delay.
     * @param delay number of seconds (or milliseconds) of the delay
     * @param inMilliseconds if true,then <code>delay</code> is specified in
     * milliseconds, otherwise in seconds
     * @return human readable string of the delay, eg: "3h 15m 47s"
     */
    public static String convertDelayToHumanString(long delay, boolean inMilliseconds) {
        if (inMilliseconds) {
            delay = Math.round(delay / 1000.0);
        }
        long seconds = delay % 60;
        long minutes = (delay / 60) % 60;
        long hours = delay / 3600;

        StringBuilder builder = new StringBuilder();
        builder.append(seconds);
        builder.append(l10n.getString("QueuePanel.second_shortcut"));
        if (minutes > 0) {
            builder.insert(0, l10n.getString("QueuePanel.minute_shortcut") + " ");
            builder.insert(0, minutes);
        }
        if (hours > 0) {
            builder.insert(0, l10n.getString("QueuePanel.hour_shortcut") + " ");
            builder.insert(0, hours);
        }

        return builder.toString();
    }

    /** Parse OperatorInfo implementation from the provided URL.
     * @param script URL (file or jar) of operator script
     * @return OperatorInfo implementation
     * @throws IOException when there is problem accessing the script file
     * @throws ScriptException when the script is not valid
     * @throws IntrospectionException when current JRE does not support JavaScript execution
     */
    public static OperatorInfo parseInfo(URL script) throws IOException, ScriptException, IntrospectionException {
        logger.finer("Parsing info of script: " + script.toExternalForm());
        if (jsEngine == null) {
            jsEngine = manager.getEngineByName("js");
        }
        if (jsEngine == null) {
            throw new IntrospectionException("JavaScript execution not supported");
        }
        Invocable invocable = (Invocable) jsEngine;
        Reader reader = null;
        try {
            reader = new InputStreamReader(script.openStream(), "UTF-8");
            //the script must be evaluated before extracting the interface
            jsEngine.eval(reader);
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
}
