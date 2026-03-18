import java.util.Vector;

public class ProxyManager {
    private static ProxyManager instance;
    private Vector proxyList = new Vector();
    private Vector proxyNames = new Vector(); // ✅ NEW: Friendly names
    private int currentIndex = 0;
    
    private ProxyManager() {
        // ✅ CHANGED: Updated proxy list with friendly names
        addProxy("Direct", "Direct"); // No proxy
        addProxy("Glype", "http://nnp.nnchan.ru/glype/browse.php?u=");
        addProxy("Dashproxy", "http://aged-darkness-5be4.granbind20.workers.dev/?url=");
        addProxy("William", "http://williamsmobile.co.uk/proxy.php?url=");
        // ✅ NEW: Additional proxy options
        addProxy("CroxyProxy", "http://www.croxyproxy.com/go?url=");
    }
    
    /**
     * ✅ NEW: Helper method to add proxy with name
     */
    private void addProxy(String name, String url) {
        proxyNames.addElement(name);
        proxyList.addElement(url);
    }
    
    public static synchronized ProxyManager getInstance() {
        if (instance == null) {
            instance = new ProxyManager();
        }
        return instance;
    }
    
    public String getCurrentProxy() {
        return (String) proxyList.elementAt(currentIndex);
    }
    
    /**
     * ✅ NEW: Get current proxy friendly name
     */
    public String getCurrentProxyName() {
        return (String) proxyNames.elementAt(currentIndex);
    }
    
    /**
     * ✅ CHANGED: Enhanced proxy selection by name
     */
    public void setProxy(String proxyName) {
        // Try exact match first
        for (int i = 0; i < proxyNames.size(); i++) {
            if (proxyNames.elementAt(i).equals(proxyName)) {
                currentIndex = i;
                return;
            }
        }
        
        // Fallback to partial match in URL
        for (int i = 0; i < proxyList.size(); i++) {
            String proxyUrl = (String) proxyList.elementAt(i);
            if (proxyUrl.indexOf(proxyName) != -1) {
                currentIndex = i;
                return;
            }
        }
        
        // If not found, keep current proxy
    }
    
    /**
     * ✅ NEW: Set proxy by index
     */
    public void setProxyByIndex(int index) {
        if (index >= 0 && index < proxyList.size()) {
            currentIndex = index;
        }
    }
    
    /**
     * ✅ CHANGED: Smart URL wrapping with API detection
     */
    public String wrapUrl(String targetUrl) {
        String proxy = getCurrentProxy();
        
        // ✅ NEW: Never proxy Dashtube API (it requires HTTPS)
        if (targetUrl.indexOf("dashtube-api") != -1) {
            return targetUrl;
        }
        
        // Mode direct
        if (proxy.equals("Direct")) {
            return targetUrl;
        }
        
        // ✅ NEW: Don't proxy if target is already HTTPS and proxy is HTTP
        if (targetUrl.startsWith("https://") && proxy.startsWith("http://")) {
            // Downgrade to HTTP or use direct
            if (targetUrl.indexOf("dashtube-api") == -1) {
                // Safe to downgrade for non-API requests
                targetUrl = "http://" + targetUrl.substring(8);
            } else {
                // Keep HTTPS for APIs, use direct connection
                return targetUrl;
            }
        }
        
        // Encodage URL pour les proxys
        String encoded = encodeUrl(targetUrl);
        return proxy + encoded;
    }
    
    /**
     * ✅ NEW: Check if URL should bypass proxy
     */
    public boolean shouldBypassProxy(String url) {
        // Always bypass for Dashtube API
        if (url.indexOf("dashtube-api") != -1) {
            return true;
        }
        
        // Bypass for local/file URLs
        if (url.startsWith("file://")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * ✅ CHANGED: Enhanced URL encoding (preserve :// in http/https)
     */
    private String encodeUrl(String url) {
        StringBuffer result = new StringBuffer();
        char[] hex = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        
        // Special handling for protocol
        if (url.startsWith("http://")) {
            result.append("http://");
            url = url.substring(7);
        } else if (url.startsWith("https://")) {
            result.append("https://");
            url = url.substring(8);
        }
        
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || 
                (c >= '0' && c <= '9') || c == '-' || c == '_' || 
                c == '.' || c == '~' || c == '/' || c == ':') {
                result.append(c);
            } else {
                result.append('%');
                result.append(hex[(c >> 4) & 0xF]);
                result.append(hex[c & 0xF]);
            }
        }
        return result.toString();
    }
    
    /**
     * ✅ NEW: Get proxy name by index
     */
    public String getProxyName(int index) {
        if (index >= 0 && index < proxyNames.size()) {
            return (String) proxyNames.elementAt(index);
        }
        return "Unknown";
    }
    
    /**
     * ✅ CHANGED: Return friendly names instead of URLs
     */
    public Vector getAvailableProxies() {
        return proxyNames;
    }
    
    /**
     * ✅ NEW: Get proxy URLs (for diagnostic)
     */
    public Vector getProxyUrls() {
        return proxyList;
    }
    
    /**
     * ✅ NEW: Rotate to next proxy
     */
    public void nextProxy() {
        currentIndex = (currentIndex + 1) % proxyList.size();
    }
    
    /**
     * ✅ NEW: Reset to direct (no proxy)
     */
    public void reset() {
        currentIndex = 0; // Direct
    }
    
    /**
     * ✅ NEW: Test if current proxy is working
     */
    public boolean testCurrentProxy() {
        String testUrl = "http://www.google.com";
        String proxiedUrl = wrapUrl(testUrl);
        
        try {
            // Try to fetch test URL through proxy
            HttpUtils.fetch(proxiedUrl);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * ✅ NEW: Find first working proxy
     */
    public boolean findWorkingProxy() {
        // Try current proxy first
        if (testCurrentProxy()) {
            return true;
        }
        
        // Try all other proxies
        int startIndex = currentIndex;
        for (int i = 0; i < proxyList.size(); i++) {
            nextProxy();
            if (currentIndex == startIndex) {
                break; // Looped back, none working
            }
            
            if (testCurrentProxy()) {
                return true;
            }
        }
        
        // No working proxy found, reset to Direct
        reset();
        return false;
    }
    
    /**
     * ✅ NEW: Get proxy status summary
     */
    public String getProxyStatus() {
        StringBuffer status = new StringBuffer();
        status.append("Current: ").append(getCurrentProxyName()).append("\n");
        status.append("Status: ");
        
        if (getCurrentProxyName().equals("Direct")) {
            status.append("Direct connection");
        } else {
            status.append(testCurrentProxy() ? "Working" : "Failed");
        }
        
        return status.toString();
    }
    
    /**
     * ✅ NEW: Get detailed proxy info for diagnostic
     */
    public String getProxyInfo() {
        StringBuffer info = new StringBuffer();
        info.append("=== PROXY INFORMATION ===\n\n");
        
        for (int i = 0; i < proxyNames.size(); i++) {
            String name = (String) proxyNames.elementAt(i);
            String url = (String) proxyList.elementAt(i);
            
            info.append((i + 1)).append(". ").append(name).append("\n");
            
            if (name.equals("Direct")) {
                info.append("   URL: No proxy\n");
            } else {
                info.append("   URL: ").append(url).append("\n");
            }
            
            if (i == currentIndex) {
                info.append("   [ACTIVE]\n");
            }
            
            info.append("\n");
        }
        
        info.append("=========================\n");
        info.append("Note: Dashtube API always\n");
        info.append("uses direct connection (HTTPS)");
        
        return info.toString();
    }
}