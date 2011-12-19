package esmska.update;

import esmska.data.Config;
import esmska.data.Contacts;
import esmska.data.History;
import esmska.data.History.Record;
import esmska.data.Links;
import esmska.data.Signature;
import esmska.data.Signatures;
import esmska.utils.RuntimeUtils;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang.time.DateUtils;

/** Class handling everything needed about collecting and posting program usage statistics.
 */
public class Statistics {

    private static final Logger logger = Logger.getLogger(Statistics.class.getName());
    private static final Config config = Config.getInstance();

    /** If the UUID hasn't already been changed this month, changes it. Otherwise
     * does nothing.
     */
    public static void refreshUUID() {
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);

        boolean needRegen = StringUtils.isEmpty(config.getUUID())
                || config.getUUIDMonth() != currentMonth;

        if (needRegen) {
            logger.fine("Regenerating a new UUID");
            config.setUUID(UUID.randomUUID().toString());
            config.setUUIDMonth(currentMonth);
        }
    }
    
    /** Collect program usage info and return it as a JSON object. */
    public static JSONObject collectUsageInfo() {
        JSONObject info = new JSONObject();
        // program info
        info.put("uuid", config.getUUID());
        info.put("version", Config.getLatestVersion());
        info.put("stable", Config.isStableVersion());
        info.put("webstart", RuntimeUtils.isRunAsWebStart());
        
        // system info
        info.put("os", WordUtils.capitalizeFully(RuntimeUtils.detectOS().name()));
        String desktop = null;
        if (RuntimeUtils.isGnomeDesktop()) {
            desktop = "Gnome";
        } else if (RuntimeUtils.isKDEDesktop()) {
            desktop = "KDE";
        } else {
            desktop = "other";
        }
        info.put("desktop", desktop);
        String java = null;
        if (RuntimeUtils.isOracleJava()) {
            java = "Oracle";
        } else if (RuntimeUtils.isOpenJDK()) {
            java = "OpenJDK";
        } else if (RuntimeUtils.isAppleJava()) {
            java = "Apple";
        } else {
            java = "other";
        }
        info.put("java", java);
        info.put("language", Locale.getDefault().getLanguage());
        
        // user configuration
        info.put("lookAndFeel", WordUtils.capitalizeFully(config.getLookAndFeel().name()));
        info.put("countryPrefix", config.getCountryPrefix());
        info.put("useProxy", config.isUseProxy());
        info.put("notificationIcon", config.isNotificationIconVisible());
        info.put("advancedSettings", config.isShowAdvancedSettings());
        info.put("customSignatures", Signatures.getInstance().getAll().size());
        Signature defaultSig = Signatures.getInstance().get(Signature.DEFAULT.getProfileName());
        info.put("defaultSenderNumber", StringUtils.isNotEmpty(defaultSig.getUserNumber()));
        info.put("defaultSenderName", StringUtils.isNotEmpty(defaultSig.getUserName()));
        
        // user data
        info.put("contacts", Contacts.getInstance().size());
        info.put("history", History.getInstance().getRecords().size());
        info.put("usedGateways", JSONArray.fromObject(getUsedGateways()));
        
        return info;
    }
    
    /** Get set of gateway names that were successfully used in the last
     *  three months.
     */
    private static TreeSet<String> getUsedGateways() {
        List<Record> records = History.getInstance().getRecords();
        // take only last 1000 history records
        records = records.subList(Math.max(0, records.size()-1000), records.size());
        // take only history records in the last 90 days
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -90);
        Date historyLimit = cal.getTime();
        
        TreeSet<String> gateways = new TreeSet<String>();
        for (Record record : records) {
            if (record.getDate().before(historyLimit)) {
                continue;
            }
            gateways.add(record.getGateway());
        }

        return gateways;
    }
    
    /** Send program usage info to Esmska server if appropriate
     * (check with {@link #shouldSend()})
     */
    public static void sendUsageInfo() {
        if (!shouldSend()) {
            logger.fine("Not sending usage info");
            return;
        }
        
        logger.fine("Sending usage info");
        
        JSONObject info = collectUsageInfo();
        final String data = info.toString(2);
        
        try {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL(Links.SEND_STATS);
                        URLConnection conn = url.openConnection();
                        conn.setDoOutput(true);
                        conn.setUseCaches(false);
                        OutputStream out = conn.getOutputStream();
                        out.write(data.getBytes("UTF-8"));
                        out.flush();
                        InputStream in = conn.getInputStream();
                        in.close();
                        
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                config.setLastStatsSent(new Date());
                            }
                        });
                        
                        logger.finer("Usage info sent");
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, "Could not send usage info", ex);
                    }
                }
            });
            t.setDaemon(true);
            t.start();
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Could not execute sending usage info", ex);
        }
    }
    
    /** Decide whether to send statistics to Esmska server or not. We don't want to
     * send stats too often so that the server is not overloaded. The current decision
     * algorithm:
     * <ul>
     * <li>If this is the first time Esmska was ever started, don't send anything.</li>
     * <li>If this is the first time Esmska was started in current month, always send.</li>
     * <li>If today is 20th or earlier day in the current month, send stats at maximum
     * once per 5 days (check last submission date and decide).</li>
     * <li>If today is 21th or later day in the current month, send stats at maximum
     * once per 3 days (check last submission date and decide).</li>
     * </ul>
     * @return whether it is appropriate to send stats now or not
     */
    public static boolean shouldSend() {
        // first run
        if (config.isFirstRun()) {
            logger.fine("Shouldn't send usage info, first program run");
            return false;
        }
        
        // no stored date
        if (config.getLastStatsSent() == null) {
            logger.fine("Last usage info submission date unknown, should send usage info");
            return true;
        }
        
        Calendar last = DateUtils.toCalendar(config.getLastStatsSent());
        Calendar now = Calendar.getInstance();
        
        // compute time difference
        long diff = now.getTimeInMillis() - last.getTimeInMillis();
        double diffDays = diff / (24 * 60 * 60 * 1000.0);
        
        DateFormat df = DateFormat.getDateTimeInstance();
        logger.log(Level.FINE, "Last usage info sent on {0} ({1,number} days ago)", 
                new Object[]{df.format(last.getTime()), diffDays});
        
        // first run in current month
        if (last.get(Calendar.MONTH) != now.get(Calendar.MONTH) ||
            last.get(Calendar.YEAR) != now.get(Calendar.YEAR)) {
            logger.fine("Should send usage info, first program run in the current month");
            return true;
        }
        
        int limit = 0;
        
        // today is 20th or earlier day
        if (now.get(Calendar.DAY_OF_MONTH) <= 20) {
            limit = 5;
        } else {
            // today is 21st or later day
            limit = 3;
        }
        
        if (diffDays >= limit) {
            logger.fine("Should send usage info");
            return true;
        } else {
            logger.fine("Shouldn't send usage info");
            return false;
        }
    }
}
