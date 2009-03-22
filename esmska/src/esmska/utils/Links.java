/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.utils;

import java.util.Locale;

/** Class containing links to program websites
 *
 * @author ripper
 */
public class Links {
    /** program homepage */
    public static final String HOMEPAGE = "http://esmska.googlecode.com/";
    /** program download page */
    public static final String DOWNLOAD = "http://code.google.com/p/esmska/wiki/Download?tm=2";
    /** program issue tracker */
    public static final String ISSUES = "http://code.google.com/p/esmska/wiki/Issues";
    /** program FAQ */
    public static final String FAQ = "http://code.google.com/p/esmska/wiki/FAQ";
    /** program support forum */
    public static final String FORUM = "https://answers.launchpad.net/esmska";
    /** program translations */
    public static final String TRANSLATE = "https://translations.launchpad.net/esmska";
    /** program donations and other support */
    public static String DONATE = "http://code.google.com/p/esmska/wiki/Support";

    //change some locale-specific pages
    static {
        //Czech language
        if ("cs".equals(Locale.getDefault().getLanguage())) {
            DONATE = "http://code.google.com/p/esmska/wiki/Podporte";
        }
    }
}
