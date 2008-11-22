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
    
    /** Check whether current OS is Linux */
    public static boolean isLinux() {
        return isEqual(LINUX);
    }
    
    /** Check whether current OS is Mac */
    public static boolean isMac() {
        return isEqual(MAC_OS_X);
    }
    
    /** Check whether current OS is Windows */
    public static boolean isWindows() {
        return isEqual(WINDOWS);
    }
    
    /** Check whether current desktop environment is Gnome */
    public static boolean isGnomeDesktop() {
        return System.getenv("GNOME_DESKTOP_SESSION_ID") != null;
    }
    
    /** Check whether current desktop environment is KDE */
    public static boolean isKDEDesktop() {
        return System.getenv("KDE_FULL_SESSION") != null;
    }
    
    /** Get version of current KDE desktop environment
     * @return KDE version if running under KDE; null otherwise
     */
    public static String getKDEDesktopVersion() {
        if (!isKDEDesktop()) {
            return null;
        }
        String version = System.getenv("KDE_SESSION_VERSION");
        if (version != null) {
            return version;
        } else {
            //KDE_SESSION_VERSION is introduced only in KDE4. If it is not
            //present it is most probably KDE3.
            return "3";
        }
    }
}
