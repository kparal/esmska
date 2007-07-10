/*
 * O2.java
 *
 * Created on 7. červenec 2007, 18:14
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package operators;

import esmska.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author ripper
 */
public class O2 implements Operator {
    private final int MAX_CHARS = 60;
    private final int SMS_CHARS = 60;
    
    /**
     * Creates a new instance of O2
     */
    public O2() {
    }
    
    public URL getSecurityImage() {
        URL url = null;
        try {
            url = new URL("http://www2.cz.o2.com/sms/SMSGWChargingClient?action=image");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return url;
    }
    
    public boolean send(SMS sms) {
        return true;
//        try {
//            URL url = new URL("http://www2.cz.o2.com/sms/SMSGWChargingClient?action=edit");
//            HttpURLConnection con = (HttpURLConnection) url.openConnection();
//            con.setDoOutput(true);
//            con.setUseCaches(false);
//            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
//            OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream(), "UTF-8");
//            wr.write("action=" + URLEncoder.encode("confirm","UTF-8")
//            + "&msgText=" + URLEncoder.encode(sms.getText()!=null?sms.getText():"","UTF-8")
//            + "&sn=" + URLEncoder.encode(sms.getNumber()!=null?sms.getNumber():"","UTF-8")
//            + "&code=" + URLEncoder.encode(sms.getImageCode()!=null?sms.getImageCode():"","UTF-8")
//            + "&reply=" + URLEncoder.encode("0","UTF-8")
//            + "&intruder=" + URLEncoder.encode("0","UTF-8")
//            + "&lang=" + URLEncoder.encode("cs","UTF-8")
//            );
//            wr.flush();
//            wr.close();
//            
//            BufferedReader br = new BufferedReader(
//                    new InputStreamReader(con.getInputStream(), "UTF-8"));
//            
//            String content = "";
//            String s = "";
//            while ((s = br.readLine()) != null) {
//                content += s + "\n";
//            }
//            
//            br.close();
//            con.disconnect();
//            
//            Pattern p = Pattern.compile("<div id=\"thanks\">");
//            Matcher m = p.matcher(content);
//            boolean ok = m.find();
//            
//            p = Pattern.compile("<div id=\"errmsg\">\n<p>(.*?)</p>");
//            m = p.matcher(content);
//            if (m.find())
//                sms.setErrMsg(m.group(1));
//            
//            return ok;
//            
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return false;
    }
    
    public String toString() {
        return "O2";
    }
    
    public int getMaxChars() {
        return MAX_CHARS;
    }
    
    public int getSMSCount(int chars) {
        return (int)Math.ceil((double)chars / SMS_CHARS);
    }
    
}
