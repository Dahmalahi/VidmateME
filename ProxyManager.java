import java.util.Vector;

public class ProxyManager {
    private static ProxyManager instance;
    private Vector proxyList = new Vector();
    private int currentIndex = 0;
    
    private ProxyManager() {
        // Tous les proxys disponibles
        proxyList.addElement("Direct"); // Pas de proxy
        proxyList.addElement("http://nnp.nnchan.ru/glype/browse.php?u=");
        proxyList.addElement("http://aged-darkness-5be4.granbind20.workers.dev/?url=");
        proxyList.addElement("http://williamsmobile.co.uk/proxy.php?url=");
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
    
    public void setProxy(String proxyName) {
        for (int i = 0; i < proxyList.size(); i++) {
            if (proxyList.elementAt(i).equals(proxyName) || 
                proxyList.elementAt(i).toString().indexOf(proxyName) != -1) {
                currentIndex = i;
                break;
            }
        }
    }
    
    public String wrapUrl(String targetUrl) {
        String proxy = getCurrentProxy();
        
        // Mode direct
        if (proxy.equals("Direct")) {
            return targetUrl;
        }
        
        // Encodage URL pour les proxys
        String encoded = encodeUrl(targetUrl);
        return proxy + encoded;
    }
    
    private String encodeUrl(String url) {
        StringBuffer result = new StringBuffer();
        char[] hex = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || 
                (c >= '0' && c <= '9') || c == '-' || c == '_' || 
                c == '.' || c == '~' || c == '/') {
                result.append(c);
            } else {
                result.append('%');
                result.append(hex[(c >> 4) & 0xF]);
                result.append(hex[c & 0xF]);
            }
        }
        return result.toString();
    }
    
    public Vector getAvailableProxies() {
        return proxyList;
    }
}