package esmska.data;

import esmska.data.event.ValuedEventSupport;
import esmska.data.event.ValuedListener;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/** Class managing all of the signatures.
 */
public class Signatures {
    /** Event to fire from this class. */
    public static enum Events {
        /** The collection of signatures was updated. */
        UPDATED,
    }

    private static Signatures instance;
    /** all user-defined signatures */
    private TreeMap<String, Signature> signatures = new TreeMap<String, Signature>();
    /** all pre-defined signatures */
    private LinkedHashMap<String, Signature> special = new LinkedHashMap<String, Signature>();

    // <editor-fold defaultstate="collapsed" desc="ValuedEvent support">
    private ValuedEventSupport<Events, Signature> valuedSupport = new ValuedEventSupport<Events, Signature>(this);
    public void addValuedListener(ValuedListener<Events, Signature> valuedListener) {
        valuedSupport.addValuedListener(valuedListener);
    }
    public void removeValuedListener(ValuedListener<Events, Signature> valuedListener) {
        valuedSupport.removeValuedListener(valuedListener);
    }
    // </editor-fold>
    
    private Signatures() {
        special.put(Signature.DEFAULT.getProfileName(), Signature.DEFAULT);
        special.put(Signature.NONE.getProfileName(), Signature.NONE);
    }

    public static Signatures getInstance() {
        if (instance == null) {
            instance = new Signatures();
        }
        return instance;
    }

    /** Get signature by its name. 
     * @return null if no such signature exists
     */
    public Signature get(String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        
        if (signatures.containsKey(name)) {
            return signatures.get(name);
        } else if (special.containsKey(name)) {
            return special.get(name);
        } else {
            return null;
        }
    }

    /** Add new signature.
     * @param signature not null
     * @return true if added, false otherwise (e.g. it already existed)
     */
    public boolean add(Signature signature) {
        Validate.notNull(signature);
        if (exists(signature.getProfileName())) {
            return false;
        }
        Signature previous = signatures.put(signature.getProfileName(), signature);
        assert previous == null : "Signature overwritten, shouldn't have happened";
        valuedSupport.fireEventOccured(Events.UPDATED, null);
        return true;
    }

    /** Remove signature by its name.
     *
     * @param name not empty
     */
    public void remove(String name) {
        Validate.notEmpty(name);
        signatures.remove(name);
        valuedSupport.fireEventOccured(Events.UPDATED, null);
    }

    /** Decide if such signature exists. 
     * Searches in both user-defined and pre-defined signatures.
     */
    public boolean exists(String name) {
        return signatures.containsKey(name) || special.containsKey(name);
    }

    /** Get all user-defined signatures. */
    public Collection<Signature> getAll() {
        return Collections.unmodifiableCollection(signatures.values());
    }

    /** Get all pre-defined signatures. */
    public Collection<Signature> getSpecial() {
        return Collections.unmodifiableCollection(special.values());
    }
}
