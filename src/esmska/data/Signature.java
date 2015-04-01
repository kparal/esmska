package esmska.data;

import org.apache.commons.lang.Validate;

/** This class represent user signature that is appended to an SMS.
 */
public class Signature implements Comparable<Signature> {
    /** Special NONE signature */
    public static final Signature NONE = new Signature("None", null, null);
    /** Special DEFAULT signature */
    public static final Signature DEFAULT = new Signature("Default", null, null);

    private String profileName;
    private String userName;
    private String userNumber;

    /** Required for JavaBean support. */
    private Signature() {
    }

    /** Create new signature.
     * @param profileName name of the profile, must not be empty
     */
    public Signature(String profileName, String userName, String userNumber) {
        Validate.notEmpty(profileName);
        
        this.profileName = profileName;
        this.userName = userName;
        this.userNumber = userNumber;
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

    @Override
    public int compareTo(Signature o) {
        if (o == null) {
            return 1;
        }
        return getProfileName().compareTo(o.getProfileName());
    }
}
