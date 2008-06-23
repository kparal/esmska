/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.utils;

/** Methods for detecting current implementation of Java.
 *
 * @author ripper
 */
public class JavaType {

    private static String vendor = System.getProperty("java.vendor");
    private static String vm = System.getProperty("java.vm.name");
    
    static {
        //handle null values, so methods can rely on non-null values
        if (vendor == null) {
            vendor = "";
        }
        if (vm == null) {
            vm = "";
        }
    }
    
    /** Checks whether the current Java implementation is Sun Java */
    public static boolean isSunJava() {
        return vendor.toLowerCase().contains("sun microsystems");
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
    public static boolean isSupported() {
        return isSunJava() || isOpenJDK() || isAppleJava();
    }
}
