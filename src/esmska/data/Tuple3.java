/*
 * Taken over from http://jirablog.blogspot.com/2008/10/jopenspace-2008-java-vs-dynamick-jazyky.html
 * with author's permission.
 *
 */
package esmska.data;

import java.io.Serializable;
import org.apache.commons.lang.ObjectUtils;

/**
 * Container for tuple of three objects of different classes.
 */
public class Tuple3<A, B, C> implements Serializable {

    protected A v1;
    protected B v2;
    protected C v3;
    private static final long serialVersionUID = -4987109478796050934L;

    /** Create new tuple of three objects.
     *
     * @param v1 first object
     * @param v2 second object
     * @param v3 third object
     */
    public Tuple3(A v1, B v2, C v3) {
        super();
        set1(v1);
        set2(v2);
        set3(v3);
    }

    /** Get first object */
    public A get1() {
        return v1;
    }

    /** Set first object */
    public void set1(A v1) {
        this.v1 = v1;
    }

    /** Get second object */
    public B get2() {
        return v2;
    }

    /** Set second object */
    public void set2(B v2) {
        this.v2 = v2;
    }

    /** Get third object */
    public C get3() {
        return v3;
    }

    /** Set third object */
    public void set3(C v3) {
        this.v3 = v3;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Tuple3)) {
            return false;
        }
        Tuple3<?, ?, ?> other = (Tuple3<?, ?, ?>) obj;
        return ObjectUtils.equals(get1(), other.get1()) && ObjectUtils.equals(get2(), other.get2()) &&
                ObjectUtils.equals(get3(), other.get3());
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        if (get1() != null) {
            hashCode += get1().hashCode();
        }
        hashCode *= 31;
        if (get2() != null) {
            hashCode += get2().hashCode();
        }
        hashCode *= 71;
        if (get3() != null) {
            hashCode += get3().hashCode();
        }
        return hashCode;
    }

    @Override
    public String toString() {
        return "[" + get1() + ", " + get2() + ", " + get3() + "]";
    }
}
