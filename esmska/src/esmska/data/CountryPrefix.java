/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.data;

import java.util.HashMap;

/** Class containing list of all telephone country prefixes (as defined in
 *  <a href="http://en.wikipedia.org/wiki/List_of_country_calling_codes">List of country calling codes</a>) 
 *  and some helper methods.
 *
 * @author ripper
 */
public class CountryPrefix {
    private static final HashMap<String, String> map = new HashMap<String, String>();
    static {
        map.put("AC", "+247");
        map.put("AD", "+376");
        map.put("AE", "+971");
        map.put("AF", "+93");
        map.put("AG", "+1");
        map.put("AI", "+1");
        map.put("AL", "+355");
        map.put("AM", "+374");
        map.put("AN", "+599");
        map.put("AO", "+244");
        map.put("AQ", "+672");
        map.put("AR", "+54");
        map.put("AS", "+1");
        map.put("AT", "+43");
        map.put("AU", "+61");
        map.put("AW", "+297");
        map.put("AX", "+358");
        map.put("AZ", "+994");
        map.put("BA", "+387");
        map.put("BB", "+1");
        map.put("BD", "+880");
        map.put("BE", "+32");
        map.put("BF", "+226");
        map.put("BG", "+359");
        map.put("BH", "+973");
        map.put("BI", "+257");
        map.put("BJ", "+229");
        map.put("BM", "+1");
        map.put("BN", "+673");
        map.put("BO", "+591");
        map.put("BR", "+55");
        map.put("BS", "+1");
        map.put("BT", "+975");
        map.put("BW", "+267");
        map.put("BY", "+375");
        map.put("BZ", "+501");
        map.put("CA", "+1");
        map.put("CC", "+61");
        map.put("CD", "+243");
        map.put("CF", "+236");
        map.put("CG", "+242");
        map.put("CI", "+225");
        map.put("CK", "+682");
        map.put("CL", "+56");
        map.put("CM", "+237");
        map.put("CN", "+86");
        map.put("CO", "+57");
        map.put("CR", "+506");
        map.put("CU", "+53");
        map.put("CV", "+238");
        map.put("CX", "+61");
        map.put("CY", "+357");
        map.put("CZ", "+420");
        map.put("DE", "+49");
        map.put("DJ", "+253");
        map.put("DK", "+45");
        map.put("DM", "+1");
        map.put("DO", "+1");
        map.put("DZ", "+213");
        map.put("EA", "+34");
        map.put("EC", "+593");
        map.put("EE", "+372");
        map.put("EG", "+20");
        map.put("EH", "+212");
        map.put("ER", "+291");
        map.put("ES", "+34");
        map.put("ET", "+251");
        map.put("EU", "+388");
        map.put("FI", "+358");
        map.put("FJ", "+679");
        map.put("FK", "+500");
        map.put("FM", "+691");
        map.put("FO", "+298");
        map.put("FR", "+33");
        map.put("GA", "+241");
        map.put("GB", "+44");
        map.put("GD", "+1");
        map.put("GE", "+995");
        map.put("GF", "+594");
        map.put("GG", "+44");
        map.put("GH", "+233");
        map.put("GI", "+350");
        map.put("GL", "+299");
        map.put("GM", "+220");
        map.put("GN", "+224");
        map.put("GP", "+590");
        map.put("GQ", "+240");
        map.put("GR", "+30");
        map.put("GT", "+502");
        map.put("GU", "+1");
        map.put("GW", "+245");
        map.put("GY", "+592");
        map.put("HK", "+852");
        map.put("HN", "+504");
        map.put("HR", "+385");
        map.put("HT", "+509");
        map.put("HU", "+36");
        map.put("CH", "+41");
        map.put("IC", "+34");
        map.put("ID", "+62");
        map.put("IE", "+353");
        map.put("IL", "+972");
        map.put("IM", "+44");
        map.put("IN", "+91");
        map.put("IO", "+246");
        map.put("IQ", "+964");
        map.put("IR", "+98");
        map.put("IS", "+354");
        map.put("IT", "+39");
        map.put("JE", "+44");
        map.put("JM", "+1");
        map.put("JO", "+962");
        map.put("JP", "+81");
        map.put("KE", "+254");
        map.put("KG", "+996");
        map.put("KH", "+855");
        map.put("KI", "+686");
        map.put("KM", "+269");
        map.put("KN", "+1");
        map.put("KP", "+850");
        map.put("KR", "+82");
        map.put("KW", "+965");
        map.put("KY", "+1");
        map.put("KZ", "+7");
        map.put("LA", "+856");
        map.put("LB", "+961");
        map.put("LC", "+1");
        map.put("LI", "+423");
        map.put("LK", "+94");
        map.put("LR", "+231");
        map.put("LS", "+266");
        map.put("LT", "+370");
        map.put("LU", "+352");
        map.put("LV", "+371");
        map.put("LY", "+218");
        map.put("MA", "+212");
        map.put("MC", "+377");
        map.put("MD", "+373");
        map.put("ME", "+382");
        map.put("MG", "+261");
        map.put("MH", "+692");
        map.put("MK", "+389");
        map.put("ML", "+223");
        map.put("MM", "+95");
        map.put("MN", "+976");
        map.put("MO", "+853");
        map.put("MP", "+1");
        map.put("MQ", "+596");
        map.put("MR", "+222");
        map.put("MS", "+1");
        map.put("MT", "+356");
        map.put("MU", "+230");
        map.put("MV", "+960");
        map.put("MW", "+265");
        map.put("MX", "+52");
        map.put("MY", "+60");
        map.put("MZ", "+258");
        map.put("NA", "+264");
        map.put("NC", "+687");
        map.put("NE", "+227");
        map.put("NF", "+672");
        map.put("NG", "+234");
        map.put("NI", "+505");
        map.put("NL", "+31");
        map.put("NO", "+47");
        map.put("NP", "+977");
        map.put("NR", "+674");
        map.put("NU", "+683");
        map.put("NZ", "+64");
        map.put("OM", "+968");
        map.put("PA", "+507");
        map.put("PE", "+51");
        map.put("PF", "+689");
        map.put("PG", "+675");
        map.put("PH", "+63");
        map.put("PK", "+92");
        map.put("PL", "+48");
        map.put("PM", "+508");
        map.put("PN", "+872");
        map.put("PR", "+1");
        map.put("PS", "+970");
        map.put("PS", "+972");
        map.put("PT", "+351");
        map.put("PW", "+680");
        map.put("PY", "+595");
        map.put("QA", "+974");
        map.put("QN", "+374");
        map.put("QN", "+994");
        map.put("QS", "+252");
        map.put("QY", "+90");
        map.put("RE", "+262");
        map.put("RO", "+40");
        map.put("RS", "+381");
        map.put("RW", "+250");
        map.put("SA", "+966");
        map.put("SB", "+677");
        map.put("SC", "+248");
        map.put("SD", "+249");
        map.put("SE", "+46");
        map.put("SG", "+65");
        map.put("SH", "+290");
        map.put("SI", "+386");
        map.put("SJ", "+47");
        map.put("SK", "+421");
        map.put("SL", "+232");
        map.put("SM", "+378");
        map.put("SN", "+221");
        map.put("SO", "+252");
        map.put("SR", "+597");
        map.put("ST", "+239");
        map.put("SV", "+503");
        map.put("SY", "+963");
        map.put("SZ", "+268");
        map.put("TA", "+290");
        map.put("TC", "+1");
        map.put("TD", "+235");
        map.put("TF", "+262");
        map.put("TG", "+228");
        map.put("TH", "+66");
        map.put("TJ", "+992");
        map.put("TK", "+690");
        map.put("TL", "+670");
        map.put("TM", "+993");
        map.put("TN", "+216");
        map.put("TO", "+676");
        map.put("TR", "+90");
        map.put("TT", "+1");
        map.put("TV", "+688");
        map.put("TW", "+886");
        map.put("TZ", "+255");
        map.put("UA", "+380");
        map.put("UG", "+256");
        map.put("US", "+1");
        map.put("UY", "+598");
        map.put("UZ", "+998");
        map.put("VA", "+379");
        map.put("VA", "+39");
        map.put("VC", "+1");
        map.put("VE", "+58");
        map.put("VG", "+1");
        map.put("VI", "+1");
        map.put("VN", "+84");
        map.put("VU", "+678");
        map.put("WF", "+681");
        map.put("WS", "+685");
        map.put("XC", "+991");
        map.put("XD", "+888");
        map.put("XE", "+871");
        map.put("XF", "+872");
        map.put("XG", "+881");
        map.put("XI", "+873");
        map.put("XL", "+883");
        map.put("XN", "+870");
        map.put("XP", "+878");
        map.put("XR", "+979");
        map.put("XS", "+808");
        map.put("XT", "+800");
        map.put("XV", "+882");
        map.put("XW", "+874");
        map.put("YE", "+967");
        map.put("YT", "+262");
        map.put("ZA", "+27");
        map.put("ZM", "+260");
        map.put("ZW", "+263");
    }
    
    /** Get telephone country prefix for country with country code.
     * 
     * @param countryCode two letter country code as defined in
     *  <a href="http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2">ISO 3166-1 alpha-2</a>.
     * @return corresponding telephone country prefix or empty string if country wasn't found
     */
    public static String getCountryPrefix(String countryCode) {
        String prefix = map.get(countryCode.toUpperCase());
        return (prefix != null ? prefix : "");
    }
    
    /** Get country code for country with specified telephone country prefix.
     * 
     * @param countryPrefix telephone country prefix as defined in
     *  <a href="http://en.wikipedia.org/wiki/List_of_country_calling_codes">List of country calling codes</a>.
     * @return corresponding two letter country code or null if country prefix wasn't found
     */
    public static String getCountryCode(String countryPrefix) {
        for (String cc : map.keySet()) {
            if (map.get(cc).equals(countryPrefix)) {
                return cc;
            }
        }
        
        return null;
    }
}
