import javax.microedition.io.*;
import java.io.*;

public class HttpUtils {
    // ✅ CHANGED: Updated User-Agents for better compatibility with modern APIs
    private static final String[] USER_AGENTS = {
        "Mozilla/5.0 (SymbianOS/9.1; U; en-us) AppleWebKit/413 (KHTML, like Gecko) Safari/413",
        "Nokia6230i/2.0 (04.44) Profile/MIDP-2.0 Configuration/CLDC-1.1",
        "Nokia5320/10.0.006 Profile/MIDP-2.1 Configuration/CLDC-1.1",
        "SonyEricssonK800i/R1JG Browser/NetFront/3.3",
        "MOT-RAZRV3/08.BD.43R MIB/2.2.1 Profile/MIDP-2.0 Configuration/CLDC-1.1",
        "UniMedia/2.1 (J2ME; MIDP-2.0)"
    };
    private static int userAgentIndex = 0;
    private static final int MAX_RETRIES = 3;
    private static final int TIMEOUT = 15000;
    
    public static String fetch(String url) throws IOException {
        boolean isDashtubeApi = url.indexOf("dashtube-api") != -1;
        
        if (url.startsWith("https://") && !isDashtubeApi) {
            url = "http://" + url.substring(8);
        }
        
        IOException lastError = null;
        
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                String userAgent = USER_AGENTS[userAgentIndex];
                String result = fetchInternal(url, userAgent, isDashtubeApi);
                return result;
            } catch (IOException e) {
                String msg = e.getMessage();
                
                if (msg != null && msg.indexOf("403") != -1) {
                    userAgentIndex = (userAgentIndex + 1) % USER_AGENTS.length;
                    lastError = e;
                    try { Thread.sleep(500); } catch (Exception ex) {}
                    continue;
                }
                
                if (msg != null && msg.indexOf("429") != -1) {
                    try { Thread.sleep(2000); } catch (Exception ex) {}
                    lastError = e;
                    continue;
                }
                
                if (msg != null && (msg.indexOf("timeout") != -1 || msg.indexOf("timed out") != -1)) {
                    lastError = new IOException("Request timeout - check connection");
                    throw lastError;
                }
                
                throw e;
            }
        }
        
        throw lastError != null ? lastError : new IOException("Echec apres " + MAX_RETRIES + " tentatives");
    }
    
    private static String fetchInternal(String url, String userAgent, boolean isJsonApi) throws IOException {
        HttpConnection conn = null;
        InputStream is = null;
        ByteArrayOutputStream baos = null;
        
        try {
            conn = (HttpConnection) Connector.open(url, Connector.READ, true);
            conn.setRequestMethod(HttpConnection.GET);
            
            conn.setRequestProperty("User-Agent", userAgent);
            
            if (isJsonApi) {
                conn.setRequestProperty("Accept", "application/json, text/plain, */*");
                conn.setRequestProperty("Content-Type", "application/json");
            } else {
                conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,*/*");
            }
            
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            conn.setRequestProperty("Connection", "close");
            conn.setRequestProperty("Cache-Control", "no-cache");
            
            if (!isJsonApi) {
                String referer = extractDomain(url);
                if (referer != null) {
                    conn.setRequestProperty("Referer", referer);
                }
            }
            
            int rc = conn.getResponseCode();
            
            if (rc == 403) {
                throw new IOException("HTTP 403 Forbidden - Access denied");
            } else if (rc == 404) {
                throw new IOException("HTTP 404 Not Found - Resource missing");
            } else if (rc == 429) {
                throw new IOException("HTTP 429 Too Many Requests - Rate limited");
            } else if (rc == 500) {
                throw new IOException("HTTP 500 Server Error - Try again later");
            } else if (rc == 503) {
                throw new IOException("HTTP 503 Service Unavailable - Server down");
            } else if (rc != HttpConnection.HTTP_OK && rc != HttpConnection.HTTP_PARTIAL) {
                throw new IOException("HTTP error: " + rc);
            }
            
            is = conn.openInputStream();
            baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            int len;
            int totalRead = 0;
            
            final int MAX_RESPONSE_SIZE = 512 * 1024;
            
            while ((len = is.read(buffer)) > 0) {
                totalRead += len;
                if (totalRead > MAX_RESPONSE_SIZE) {
                    throw new IOException("Response too large (>512 KB)");
                }
                baos.write(buffer, 0, len);
            }
            
            if (baos.size() == 0) {
                throw new IOException("Empty response received");
            }
            
            String contentType = conn.getHeaderField("Content-Type");
            String encoding = "UTF-8";
            
            if (contentType != null && contentType.indexOf("charset=") != -1) {
                int start = contentType.indexOf("charset=") + 8;
                int end = contentType.indexOf(";", start);
                if (end == -1) end = contentType.length();
                encoding = contentType.substring(start, end).trim();
            }
            
            return new String(baos.toByteArray(), encoding);
            
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Unexpected error: " + e.getMessage());
        } finally {
            try { if (baos != null) baos.close(); } catch (Exception e) {}
            try { if (is != null) is.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }
    
    private static String extractDomain(String url) {
        try {
            int start = url.indexOf("://");
            if (start == -1) return null;
            start += 3;
            
            int end = url.indexOf("/", start);
            if (end == -1) end = url.length();
            
            String domain = url.substring(0, end);
            return domain;
        } catch (Exception e) {
            return null;
        }
    }
    
    public static boolean downloadFile(String url, String destPath, long startPos) throws IOException {
        return downloadFileWithProgress(url, destPath, startPos, null);
    }
    
    // ✅ FIXED: Changed Exception to IOException in signature
    public static boolean downloadFileWithProgress(String url, String destPath, long startPos, DownloadProgressListener listener) throws IOException {
        boolean isDashtubeApi = url.indexOf("dashtube-api") != -1;
        if (url.startsWith("https://") && !isDashtubeApi) {
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
            
            if (startPos > 0) {
                conn.setRequestProperty("Range", "bytes=" + startPos + "-");
            }
            
            int rc = conn.getResponseCode();
            
            if ((startPos == 0 && rc != HttpConnection.HTTP_OK) || 
                (startPos > 0 && rc != HttpConnection.HTTP_PARTIAL && rc != HttpConnection.HTTP_OK)) {
                throw new IOException("HTTP error: " + rc);
            }
            
            long totalSize = conn.getLength();
            if (listener != null && totalSize > 0) {
                listener.onStart(totalSize);
            }
            
            is = conn.openInputStream();
            
            // ✅ FIXED: Wrap in try-catch to handle Exception from StorageManager
            try {
                os = StorageManager.openOutputStream(destPath, startPos > 0);
            } catch (Exception e) {
                throw new IOException("Cannot open output stream: " + e.getMessage());
            }
            
            byte[] buffer = new byte[4096];
            int len;
            long downloaded = startPos;
            long lastProgressUpdate = System.currentTimeMillis();
            
            while ((len = is.read(buffer)) > 0) {
                os.write(buffer, 0, len);
                downloaded += len;
                
                if (listener != null) {
                    long now = System.currentTimeMillis();
                    if (now - lastProgressUpdate > 500) {
                        listener.onProgress(downloaded, totalSize);
                        lastProgressUpdate = now;
                    }
                }
                
                os.flush();
            }
            
            if (listener != null) {
                listener.onComplete();
            }
            
            return true;
            
        } catch (IOException e) {
            if (listener != null) {
                listener.onError(e.getMessage());
            }
            throw e;
        } finally {
            try { if (os != null) { os.flush(); os.close(); } } catch (Exception e) {}
            try { if (is != null) is.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }
    
    public static boolean isUrlAccessible(String url) {
        return isUrlAccessible(url, 5000);
    }
    
    public static boolean isUrlAccessible(String url, int timeoutMs) {
        HttpConnection conn = null;
        try {
            if (url.startsWith("https://") && url.indexOf("dashtube-api") == -1) {
                url = "http://" + url.substring(8);
            }
            
            conn = (HttpConnection) Connector.open(url, Connector.READ, true);
            conn.setRequestMethod(HttpConnection.HEAD);
            conn.setRequestProperty("User-Agent", USER_AGENTS[0]);
            
            int rc = conn.getResponseCode();
            return (rc == HttpConnection.HTTP_OK || rc == HttpConnection.HTTP_PARTIAL);
        } catch (Exception e) {
            return false;
        } finally {
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }
    
    public static String getCurrentUserAgent() {
        return USER_AGENTS[userAgentIndex];
    }
    
    public static void rotateUserAgent() {
        userAgentIndex = (userAgentIndex + 1) % USER_AGENTS.length;
    }
    
    public static void resetUserAgent() {
        userAgentIndex = 0;
    }
}

/**
 * ✅ Interface for download progress callbacks
 */
interface DownloadProgressListener {
    void onStart(long totalSize);
    void onProgress(long downloaded, long total);
    void onComplete();
    void onError(String message);
}