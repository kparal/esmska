/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.transfer;

/** Sets system-wide internet proxies.
 * 
 * @author ripper
 */
public class ProxyManager {

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
    }
}
