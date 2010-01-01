
package esmska.data;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/** Class describing deprecated operator gateway.
 *
 * @author ripper
 */
public class DeprecatedOperator {
    private final String name;
    private final String version;
    private final String reason;

    /** Create new description of deprecated operator.
     *
     * @param name name of the deprecated operator. May not be null nor empty.
     * @param version version of the deprecated operator.
     * This means all operators of the same or older version are deprecated.
     * Newer ones are not deprecated. May not be null nor empty.
     * @param reason reason why this operator was deprecated. May be null.
     */
    public DeprecatedOperator(String name, String version, String reason) {
        Validate.notEmpty(name);
        Validate.notEmpty(version);
        this.name = name;
        this.version = version;
        this.reason = reason;
    }

    /** Name of the deprecated operator. Never null nor empty. */
    public String getName() {
        return name;
    }

    /** Version of the deprecated operator. This means all operators of the same
     * or older version are deprecated. Newer ones are not deprecated. Never
     * null nor empty.
     */
    public String getVersion() {
        return version;
    }

    /** Reason why this operator was deprecated. May be null. */
    public String getReason() {
        return reason;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DeprecatedOperator)) {
            return false;
        }
        DeprecatedOperator d = (DeprecatedOperator) obj;

        return new EqualsBuilder().append(name, d.name).append(version, d.version).
                append(reason, d.reason).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(937, 39).append(name).append(version).
                append(reason).toHashCode();
    }


}
