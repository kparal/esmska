/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.utils;

import esmska.gui.ThemeManager;
import java.awt.Dialog.ModalityType;
import javax.swing.JDialog;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

/** Methods for detecting current runtime environment (operating system,
 * java version).
 *
 * @author ripper
 */
public class RuntimeUtils {

    /** Enum of operating system types */
    public static enum OSType {

        /** GNU/Linux */
        LINUX,
        /** Mac OS X */
        MAC_OS_X,
        /** Microsoft Windows */
        WINDOWS,
        /** Other OS */
        OTHER;
    }

    private static String vendor = StringUtils.defaultString(System.getProperty("java.vendor"));
    private static String vm = StringUtils.defaultString(System.getProperty("java.vm.name"));

    /** Detect type of current OS
     * @return OS type
     */
    public static OSType detectOS() {
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

    /** Check whether current OS is Linux */
    public static boolean isLinux() {
        return detectOS() == OSType.LINUX;
    }

    /** Check whether current OS is Mac */
    public static boolean isMac() {
        return detectOS() == OSType.MAC_OS_X;
    }

    /** Check whether current OS is Windows */
    public static boolean isWindows() {
        return detectOS() == OSType.WINDOWS;
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

    /** Checks whether the current Java implementation is Sun Java */
    public static boolean isSunJava() {
        return vendor.toLowerCase().contains("sun microsystems") && !isOpenJDK();
    }

    /** Checks whether the current Java implementation is OpenJDK */
    public static boolean isOpenJDK() {
        return vm.toLowerCase().contains("openjdk");
    }

    /** Checks whether the current Java implementation is Apple Java */
    public static boolean isAppleJava() {
        return vendor.toLowerCase().contains("apple");
    }

    /** Checks whether the current Java implementation is supported.
     * Currently supported and tested are: Sun Java, OpenJDK, Apple Java
     */
    public static boolean isSupportedJava() {
        return isSunJava() || isOpenJDK() || isAppleJava();
    }

    /** Sorts options provided to dialog as a buttons according to current look
     * and feel. Some L&Fs are reversing order of the buttons, which is an
     * unwanted behaviour. This method reverts it again for such L&Fs, so options
     * are in original order. In every case be sure never rely on exact
     * option position.
     *
     * More detailed explanation:
     * There are two ways to display options:
     * 1. Standard (default option rightmost): Cancel | Action
     * 2. Windows-like (default option leftmost): Action | Cancel
     *
     * In Esmska all dialogs are written using the standard notation.
     *
     * Desktop environments divided by the way of displaying options:
     * 1. Standard: Gnome, Mac OS
     * 2. Windows like: Windows, KDE
     *
     * In following LAFs and OSs Java displays the options in wrong order:
     * 1. Metal: Windows, KDE
     * 2. GTK: Gnome, Mac OS
     * 3. Windows: Windows (available nowhere else)
     * 4. Aqua: Mac OS (available nowhere else)
     * 5. JGoodies: Windows, KDE
     * 6. Substance: Windows, KDE
     * 7. Nimbus: Windows, Gnome, Mac
     *
     * In these cases this method will revert the button order to display
     * it like native application.
     *
     * @param options options written in the standard notation (default option rightmost)
     * @return options adjusted to current environment and LaF;
     * null if <code>options</code> was null
     */
    public static Object[] sortDialogOptions(Object... options) {
        Object[] reversed = ArrayUtils.clone(options);
        ArrayUtils.reverse(reversed);

        if (isWindows()) {
            return reversed;
        }
        if (isKDEDesktop() && !ThemeManager.isGTKCurrentLaF() &&
                !ThemeManager.isNimbusCurrentLaF()) {
            return reversed;
        }
        if (ThemeManager.isGTKCurrentLaF() && !isKDEDesktop()) {
            return reversed;
        }
        if (ThemeManager.isAquaCurrentLaF()) {
            return reversed;
        }
        if (ThemeManager.isNimbusCurrentLaF() && !isKDEDesktop()) {
            return reversed;
        }

        //no change needed
        return options;
    }

    /** Set dialog to be document modal and set property so it will look as
     * native modal dialog on Mac (has no effect on other platforms).
     *
     * @param dialog dialog which should be document modal and Mac native
     * looking (only on Mac)
     */
    public static void setDocumentModalDialog(JDialog dialog) {
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.getRootPane().putClientProperty("apple.awt.documentModalSheet",
                Boolean.TRUE);
    }
}
