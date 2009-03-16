/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.transfer;

import esmska.data.Config;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.ProxyHost;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/** Sets system-wide internet proxies.
 * 
 * @author ripper
 */
public class ProxyManager {

    public static enum ProxyType {
        HTTP, HTTPS, SOCKS
    }

    private static final Logger logger = Logger.getLogger(ProxyManager.class.getName());
    private static final Config config = Config.getInstance();

    /** Set system-wide proxy.
     * 
     * @param proxy proxy for HTTP, HTTPS and SOCKS in form "host" or "host:port".
     *  Use null or empty string for unsetting the proxy.
     */
    public static void setProxy(String proxy) {
        setProxy(proxy, proxy, proxy);
    }
    
    /** Set system-wide proxy.
     * 
     * @param httpProxy proxy for HTTP in form "host" or "host:port".
     *  Use null or empty string for unsetting the proxy.
     * @param httpsProxy proxy for HTTPS in form "host" or "host:port".
     *  Use null or empty string for unsetting the proxy.
     * @param socksProxy proxy for SOCKS in form "host" or "host:port".
     *  Use null or empty string for unsetting the proxy.
     */
    public static void setProxy(String httpProxy, String httpsProxy, String socksProxy) {
        String[] proxy = (httpProxy == null ? new String[0] : httpProxy.split(":"));
        System.setProperty("http.proxyHost", (proxy.length > 0 ? proxy[0] : ""));
        System.setProperty("http.proxyPort", (proxy.length > 1 ? proxy[1] : ""));
        
        proxy = (httpsProxy == null ? new String[0] : httpsProxy.split(":"));
        System.setProperty("https.proxyHost", (proxy.length > 0 ? proxy[0] : ""));
        System.setProperty("https.proxyPort", (proxy.length > 1 ? proxy[1] : ""));
        
        proxy = (socksProxy == null ? new String[0] : socksProxy.split(":"));
        System.setProperty("socksProxyHost", (proxy.length > 0 ? proxy[0] : ""));
        System.setProperty("socksProxyPort", (proxy.length > 1 ? proxy[1] : ""));

        logger.fine("Network proxy set - httpProxy: " + httpProxy +
                ", httpsProxy: " + httpsProxy + ", socksProxy: " + socksProxy);
    }

    /** Get ProxyHost for current proxy settings for a particular proxy type
     * @param proxyType proxy type, not null
     * @return ProxyHost for current proxy settings or null if no proxy set
     */
    public static ProxyHost getProxyHost(ProxyType proxyType) {
        Validate.notNull(proxyType);

        String host = null;
        int port = -1;

        try {
            if (!config.isUseProxy()) {
                return null;
            }

            switch (proxyType) {
                case HTTP:
                    host = System.getProperty("http.proxyHost");
                    String port_ = System.getProperty("http.proxyPort");
                    port = Integer.parseInt(StringUtils.defaultIfEmpty(port_, "-1"));
                    break;
                case HTTPS:
                    host = System.getProperty("https.proxyHost");
                    port_ = System.getProperty("https.proxyPort");
                    port = Integer.parseInt(StringUtils.defaultIfEmpty(port_, "-1"));
                    break;
                case SOCKS:
                    host = System.getProperty("socksProxyHost");
                    port_ = System.getProperty("socksProxyPort");
                    port = Integer.parseInt(StringUtils.defaultIfEmpty(port_, "-1"));
                    break;
                default:
                    assert false : "Unknown proxy type";
                    return null;
            }

            if (StringUtils.isNotEmpty(host)) {
                return new ProxyHost(host, port);
            } else {
                return null;
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Could not create " + proxyType + " proxy for: "
                    + host + ":" + port, ex);
            return null;
        }
    }
}
