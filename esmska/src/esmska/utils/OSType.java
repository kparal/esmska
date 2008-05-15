/*
 * @(#) OSType.java	May 12, 2008 - 6:59:25 PM 
 */
package esmska.utils;

/** Enum of operating system types and some helper methods
 * 
 * @author  Marian Bouček
 * @version 1.0
 */
public enum OSType {

    /** GNU/Linux */
    LINUX,
    /** Mac OS X */
    MAC_OS_X,
    /** Microsoft Windows */
    WINDOWS,
    /** Other OS */
    OTHER;

    /** Detect type of current OS
     * 
     * @return OS type
     */
    public static OSType detect() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("linux")) {
            return OSType.LINUX;
        } else if (os.contains("mac os x")) {
            return OSType.MAC_OS_X;
        } else if (os.contains("windows")) {
            return OSType.WINDOWS;
        } else {
            return OSType.OTHER;
        }
    }

    /** Check whether current OS type is the same as in the parameter */
    public static boolean isEqual(OSType osType) {
        return detect().equals(osType);
    }
}
