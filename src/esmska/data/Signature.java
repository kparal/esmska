package esmska.data;

import org.apache.commons.lang.Validate;

/** This class represent user signature that is appended to an SMS.
 */
public class Signature implements Comparable<Signature> {
    /** Special NONE signature */
    public static final Signature NONE = new Signature("None", null, null, false);
    /** Special DEFAULT signature */
    public static final Signature DEFAULT = new Signature("Default", null, null, false);

    private String profileName;
    private String userName;
    private String userNumber;
    private boolean prepend;

    /** Required for JavaBean support. */
    private Signature() {
    }

    /** Create new signature.
     * @param profileName name of the profile, must not be empty
     */
    public Signature(String profileName, String userName, String userNumber, boolean prepend) {
        Validate.notEmpty(profileName);
        
        this.profileName = profileName;
        this.userName = userName;
        this.userNumber = userNumber;
        this.prepend = prepend;
    }

    /** Get name of this signature profile. */
    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        Validate.notEmpty(profileName);
        this.profileName = profileName;
    }

    /** Get sender name that should be appended to the message. */
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    /** Get sender number that should be set as the originator of the message. */
    public String getUserNumber() {
        return userNumber;
    }

    public void setUserNumber(String userNumber) {
        this.userNumber = userNumber;
    }

    /** Get value indicating whether the sender name should be prepended (true) or appended (false) to the message. */
    public boolean isPrepend() {
        return prepend;
    }

    public void setPrepend(boolean prepend) {
        this.prepend = prepend;
    }

    @Override
    public int compareTo(Signature o) {
        if (o == null) {
            return 1;
        }
        return getProfileName().compareTo(o.getProfileName());
    }
}
