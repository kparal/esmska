package esmska.utils;

import esmska.gui.ThemeManager;
import esmska.integration.IntegrationAdapter;
import java.awt.Dialog.ModalityType;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private static final Logger logger = Logger.getLogger(RuntimeUtils.class.getName());

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

    /** Check whether current desktop environment is Gnome 3.x */
    public static boolean isGnome3Desktop() {
        if (!isGnomeDesktop()) {
            return false;
        }
        // gnome-default-applications-properties is only available in GNOME 2.x,
        // but not in GNOME 3.x
        // code taken from: https://bugzilla.redhat.com/show_bug.cgi?format=multiple&id=654746
        try {
            Process p = Runtime.getRuntime().exec("which gnome-default-applications-properties");
            p.waitFor();
            if (p.exitValue() != 0 && p.exitValue() != 1) {
                logger.log(Level.WARNING, "''which'' returned an unexpected exit code: {0}", p.exitValue());
            }
            return p.exitValue() != 0;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Can't test whether this is Gnome 3", e);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Can't test whether this is Gnome 3", e);
        }
        return false;
    }

    /** Checks whether the current Java implementation is Oracle Java */
    public static boolean isOracleJava() {
        return (vendor.toLowerCase().contains("sun microsystems") ||
                vendor.toLowerCase().contains("oracle corporation")) && 
                !isOpenJDK();
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
     * Currently supported and tested are: Oracle Java, OpenJDK, Apple Java
     */
    public static boolean isSupportedJava() {
        return isOracleJava() || isOpenJDK() || isAppleJava();
    }

    /** Get basic information about current system. Useful for debugging
     * print-outs.
     */
    public static String getSystemInfo() {
        String[] props = new String[]{
            "os.name", "os.version", "os.arch", "user.name", "user.home",
            "user.dir", "java.version", "java.vendor", "java.vm.name",
            "java.vm.version"
        };

        StringBuilder builder = new StringBuilder();
        for (String prop : props) {
            builder.append(prop);
            builder.append("=");
            builder.append(System.getProperty(prop));
            builder.append("; ");
        }
        
        if (isGnomeDesktop()) {
            builder.append("desktop=GNOME; ");
        } else if (isKDEDesktop()) {
            builder.append("desktop=KDE; ");
        }
        
        builder.append("language=").append(Locale.getDefault().getLanguage()).append("; ");
        
        // delete the trailing semicolon
        builder.delete(builder.length()-2, builder.length());

        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    /** Sorts options provided to dialog as a buttons according to current look
     * and feel. Some L&Fs are reversing order of the buttons, which is an
     * unwanted behaviour. This method reverts it again for such L&Fs, so options
     * are in original order. In every case be sure never rely on exact
     * option position.<br>
     * <br>
     * This method is intended to be used only for dialogs created with
     * JOptionPane methods (works around its own option swapping). For custom
     * dialogs use {@link #sortOptions(java.lang.Object[])}.<br>
     * <br>
     * More detailed explanation:<br>
     * There are two ways to display options:<br>
     * 1. Standard (default option rightmost): Cancel | Action<br>
     * 2. Windows-like (default option leftmost): Action | Cancel<br>
     * <br>
     * In Esmska all dialogs are written using the standard notation.<br>
     * <br>
     * Desktop environments are divided by the way of displaying options:<br>
     * 1. Standard: Gnome, Mac OS<br>
     * 2. Windows like: Windows, KDE<br>
     * <br>
     * In following LAFs and OSs Java displays the options in wrong order:<br>
     * 1. Metal: Windows, KDE<br>
     * 2. GTK: Gnome, Mac OS<br>
     * 3. Windows: Windows (available nowhere else)<br>
     * 4. Aqua: Mac OS (available nowhere else)<br>
     * 5. JGoodies: Windows, KDE<br>
     * 6. Substance: Windows, KDE<br>
     * 7. Nimbus: Windows, Gnome, Mac<br>
     * <br>
     * In these cases this method will revert the button order to display
     * it like native application.
     *
     * @param options options written in the standard notation (default option rightmost)
     * @return options adjusted to current environment and LaF;
     * null if <code>options</code> was null
     */
    public static <T> T[] sortDialogOptions(T... options) {
        T[] reversed = (T[]) ArrayUtils.clone(options);
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

    @SuppressWarnings("unchecked")
    /** Sorts options provided to custom dialog as a buttons according to current
     * desktop environment.<br>
     * <br>
     * For dialogs created by JOptionPane use
     * {@link #sortDialogOptions(java.lang.Object[])}.<br>
     * <br>
     * More detailed explanation:<br>
     * There are two ways to display options:<br>
     * 1. Standard (default option rightmost): Cancel | Action<br>
     * 2. Windows-like (default option leftmost): Action | Cancel<br>
     * <br>
     * In Esmska all dialogs are written using the standard notation.<br>
     * <br>
     * Desktop environments are divided by the way of displaying options:<br>
     * 1. Standard: Gnome, Mac OS<br>
     * 2. Windows like: Windows, KDE<br>
     * <br>
     * In the second case this method will revert the button order to display
     * it like native application.
     */
    public static <T> T[] sortOptions(T... options) {
        if (isWindows() || isKDEDesktop()) {
            //revert options
            T[] reversed = (T[]) ArrayUtils.clone(options);
            ArrayUtils.reverse(reversed);
            return reversed;
        }

        //no change needed
        return options;
    }

    /** Set dialog to be document modal and set property so it will look as
     * native modal dialog on Mac (has no effect on other platforms).
     * Also notifies Integration adapter about this event.
     *
     * @param dialog dialog which should be document modal and Mac native
     * looking (only on Mac)
     */
    public static void setDocumentModalDialog(JDialog dialog) {
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.getRootPane().putClientProperty("apple.awt.documentModalSheet",
                Boolean.TRUE);
       IntegrationAdapter.getInstance().registerModalSheet(dialog);
    }
}
