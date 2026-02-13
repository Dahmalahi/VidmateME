import javax.microedition.io.*;
import java.io.*;

public class HttpUtils {
    // User-Agents variés pour contourner les blocages
    private static final String[] USER_AGENTS = {
        "Nokia6230i/2.0 (04.44) Profile/MIDP-2.0 Configuration/CLDC-1.1",
        "Mozilla/5.0 (SymbianOS/9.1; U; en-us) AppleWebKit/413",
        "Nokia5320/10.0.006 Profile/MIDP-2.1 Configuration/CLDC-1.1",
        "SonyEricssonK800i/R1JG Browser/NetFront/3.3",
        "MOT-RAZRV3/08.BD.43R MIB/2.2.1 Profile/MIDP-2.0 Configuration/CLDC-1.1"
    };
    private static int userAgentIndex = 0;
    private static final int MAX_RETRIES = 3;
    
    /**
     * Fetch HTTP content avec gestion automatique des erreurs 403
     * Force HTTP (pas HTTPS) pour compatibilité J2ME
     */
    public static String fetch(String url) throws IOException {
        // Forcer HTTP si HTTPS est détecté
        if (url.startsWith("https://")) {
            url = "http://" + url.substring(8);
        }
        
        IOException lastError = null;
        
        // Essayer avec différents User-Agents
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                String userAgent = USER_AGENTS[userAgentIndex];
                String result = fetchInternal(url, userAgent);
                return result;
            } catch (IOException e) {
                String msg = e.getMessage();
                
                // Si 403, essayer avec un autre User-Agent
                if (msg != null && msg.indexOf("403") != -1) {
                    userAgentIndex = (userAgentIndex + 1) % USER_AGENTS.length;
                    lastError = e;
                    
                    // Petite pause avant retry
                    try { Thread.sleep(500); } catch (Exception ex) {}
                    continue;
                }
                
                // Autres erreurs HTTP
                throw e;
            }
        }
        
        throw lastError != null ? lastError : new IOException("Echec apres " + MAX_RETRIES + " tentatives");
    }
    
    private static String fetchInternal(String url, String userAgent) throws IOException {
        HttpConnection conn = null;
        InputStream is = null;
        ByteArrayOutputStream baos = null;
        
        try {
            conn = (HttpConnection) Connector.open(url, Connector.READ, true);
            conn.setRequestMethod(HttpConnection.GET);
            
            // Headers pour simuler un vrai navigateur mobile
            conn.setRequestProperty("User-Agent", userAgent);
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,*/*");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            conn.setRequestProperty("Connection", "close");
            conn.setRequestProperty("Cache-Control", "no-cache");
            
            int rc = conn.getResponseCode();
            
            // Gestion des codes HTTP
            if (rc == 403) {
                throw new IOException("HTTP 403 Forbidden");
            } else if (rc == 404) {
                throw new IOException("HTTP 404 Not Found");
            } else if (rc == 503) {
                throw new IOException("HTTP 503 Service Unavailable");
            } else if (rc != HttpConnection.HTTP_OK && rc != HttpConnection.HTTP_PARTIAL) {
                throw new IOException("HTTP error: " + rc);
            }
            
            // Lecture du contenu
            is = conn.openInputStream();
            baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            int len;
            
            while ((len = is.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            
            // Détecter l'encodage si possible
            String contentType = conn.getHeaderField("Content-Type");
            String encoding = "UTF-8";
            
            if (contentType != null && contentType.indexOf("charset=") != -1) {
                int start = contentType.indexOf("charset=") + 8;
                int end = contentType.indexOf(";", start);
                if (end == -1) end = contentType.length();
                encoding = contentType.substring(start, end).trim();
            }
            
            return new String(baos.toByteArray(), encoding);
            
        } finally {
            try { if (baos != null) baos.close(); } catch (Exception e) {}
            try { if (is != null) is.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }
    
    /**
     * Télécharger un fichier avec support de reprise
     */
    public static boolean downloadFile(String url, String destPath, long startPos) throws IOException {
        // Forcer HTTP
        if (url.startsWith("https://")) {
            url = "http://" + url.substring(8);
        }
        
        HttpConnection conn = null;
        InputStream is = null;
        OutputStream os = null;
        
        try {
            conn = (HttpConnection) Connector.open(url, Connector.READ, true);
            conn.setRequestMethod(HttpConnection.GET);
            conn.setRequestProperty("User-Agent", USER_AGENTS[userAgentIndex]);
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Connection", "close");
            
            // Support de reprise
            if (startPos > 0) {
                conn.setRequestProperty("Range", "bytes=" + startPos + "-");
            }
            
            int rc = conn.getResponseCode();
            
            // Vérifier le code de réponse
            if ((startPos == 0 && rc != HttpConnection.HTTP_OK) || 
                (startPos > 0 && rc != HttpConnection.HTTP_PARTIAL && rc != HttpConnection.HTTP_OK)) {
                throw new IOException("HTTP error: " + rc);
            }
            
            is = conn.openInputStream();
            os = StorageManager.openOutputStream(destPath, startPos > 0);
            
            byte[] buffer = new byte[4096];
            int len;
            
            while ((len = is.read(buffer)) > 0) {
                os.write(buffer, 0, len);
                os.flush();
            }
            
            return true;
            
        } finally {
            try { if (os != null) { os.flush(); os.close(); } } catch (Exception e) {}
            try { if (is != null) is.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }
    
    /**
     * Vérifier si une URL est accessible
     */
    public static boolean isUrlAccessible(String url) {
        try {
            fetch(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
