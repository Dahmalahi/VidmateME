import javax.microedition.io.file.FileConnection;
import javax.microedition.io.Connector;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Enumeration;

public class StorageManager {
    private static String basePath = null;
    private static final String APP_FOLDER = "VidmateME"; // ✅ CHANGED: Back to VidmateME
    
    /**
     * ✅ NEW: Auto-detect best available storage path
     * Priority: E:/ → C:/ → TFCard → Other roots
     */
    public static String getDownloadPath() {
        if (basePath != null) {
            return basePath;
        }
        
        // Try to get from settings first
        try {
            String settingsPath = SettingsManager.getInstance().getStoragePath();
            if (settingsPath != null && settingsPath.length() > 0) {
                // Extract root from settings path
                if (testAndCreatePath(settingsPath)) {
                    basePath = settingsPath;
                    return basePath;
                }
            }
        } catch (Exception e) {
            // Settings not available, continue auto-detection
        }
        
        // ✅ NEW: Auto-detect available roots
        String detectedPath = autoDetectStoragePath();
        if (detectedPath != null) {
            basePath = detectedPath;
            saveToSettings(basePath);
            return basePath;
        }
        
        // Fallback to default
        basePath = "file:///E:/" + APP_FOLDER + "/";
        saveToSettings(basePath);
        return basePath;
    }
    
    /**
     * ✅ NEW: Auto-detect best storage path
     */
    private static String autoDetectStoragePath() {
        String[] priorityPaths = {
            "file:///E:/",           // SD card (most common)
            "file:///C:/",           // Internal memory
            "file:///TFCard/",       // Some devices
            "file:///MemoryCard/",   // Alternative name
            "file:///SDCard/",       // Some Android-based
            "file:///F:/",           // Alternative drive letter
            "file:///D:/"            // Another alternative
        };
        
        // Try each path in priority order
        for (int i = 0; i < priorityPaths.length; i++) {
            String testPath = priorityPaths[i] + APP_FOLDER + "/";
            if (testAndCreatePath(testPath)) {
                return testPath;
            }
        }
        
        // ✅ NEW: Try to enumerate file system roots
        try {
            String roots = System.getProperty("fileconn.dir.roots");
            if (roots != null && roots.length() > 0) {
                // Parse roots (usually comma-separated)
                int start = 0;
                while (start < roots.length()) {
                    int end = roots.indexOf(',', start);
                    if (end == -1) end = roots.length();
                    
                    String root = roots.substring(start, end).trim();
                    if (root.length() > 0) {
                        if (!root.startsWith("file://")) {
                            root = "file:///" + root;
                        }
                        if (!root.endsWith("/")) {
                            root += "/";
                        }
                        
                        String testPath = root + APP_FOLDER + "/";
                        if (testAndCreatePath(testPath)) {
                            return testPath;
                        }
                    }
                    
                    start = end + 1;
                }
            }
        } catch (Exception e) {
            // Root enumeration failed
        }
        
        return null; // No valid path found
    }
    
    /**
     * ✅ NEW: Test if path is accessible and create VidmateME folder
     */
    private static boolean testAndCreatePath(String path) {
        FileConnection fc = null;
        try {
            // Ensure path ends with /
            if (!path.endsWith("/")) {
                path += "/";
            }
            
            fc = (FileConnection) Connector.open(path, Connector.READ_WRITE);
            
            // Create directory if it doesn't exist
            if (!fc.exists()) {
                fc.mkdir();
            }
            
            // Test if we can write
            if (fc.canWrite()) {
                // Create subdirectories
                createSubfolders(path);
                fc.close();
                return true;
            }
            
            fc.close();
            return false;
            
        } catch (Exception e) {
            try { if (fc != null) fc.close(); } catch (Exception ex) {}
            return false;
        }
    }
    
    /**
     * ✅ NEW: Create VidmateME subdirectories (videos, audios, thumbnails)
     */
    private static void createSubfolders(String basePath) {
        String[] subfolders = {"videos/", "audios/", "thumbnails/"};
        
        for (int i = 0; i < subfolders.length; i++) {
            FileConnection fc = null;
            try {
                String fullPath = basePath + subfolders[i];
                fc = (FileConnection) Connector.open(fullPath, Connector.READ_WRITE);
                
                if (!fc.exists()) {
                    fc.mkdir();
                }
                
                fc.close();
            } catch (Exception e) {
                try { if (fc != null) fc.close(); } catch (Exception ex) {}
            }
        }
    }
    
    /**
     * ✅ NEW: Save detected path to settings
     */
    private static void saveToSettings(String path) {
        try {
            SettingsManager.getInstance().setStoragePath(path);
            SettingsManager.getInstance().saveSettings();
        } catch (Exception e) {
            // Ignore if settings not available
        }
    }
    
    /**
     * ✅ NEW: Get storage info (for diagnostic)
     */
    public static String getStorageInfo() {
        StringBuffer info = new StringBuffer();
        info.append("=== STORAGE INFO ===\n\n");
        
        String currentPath = getDownloadPath();
        info.append("Current path:\n").append(currentPath).append("\n\n");
        
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(currentPath, Connector.READ);
            
            if (fc.exists()) {
                long totalSize = fc.totalSize();
                long availSize = fc.availableSize();
                long usedSize = totalSize - availSize;
                
                info.append("Total space: ").append(formatSize(totalSize)).append("\n");
                info.append("Used space: ").append(formatSize(usedSize)).append("\n");
                info.append("Free space: ").append(formatSize(availSize)).append("\n");
                info.append("Writable: ").append(fc.canWrite() ? "Yes" : "No").append("\n");
            } else {
                info.append("Status: Path not found\n");
            }
            
            fc.close();
        } catch (Exception e) {
            info.append("Error: ").append(e.getMessage()).append("\n");
        } finally {
            try { if (fc != null) fc.close(); } catch (Exception e) {}
        }
        
        // ✅ NEW: Test alternative paths
        info.append("\n=== AVAILABLE STORAGE ===\n\n");
        String[] testPaths = {
            "file:///E:/",
            "file:///C:/",
            "file:///TFCard/",
            "file:///MemoryCard/"
        };
        
        for (int i = 0; i < testPaths.length; i++) {
            fc = null;
            try {
                fc = (FileConnection) Connector.open(testPaths[i], Connector.READ);
                if (fc.exists()) {
                    long avail = fc.availableSize();
                    info.append("[OK] ").append(testPaths[i]).append(" (")
                        .append(formatSize(avail)).append(" free)\n");
                } else {
                    info.append("[X] ").append(testPaths[i]).append(" (not found)\n");
                }
                fc.close();
            } catch (Exception e) {
                info.append("[X] ").append(testPaths[i]).append(" (error)\n");
            } finally {
                try { if (fc != null) fc.close(); } catch (Exception e) {}
            }
        }
        
        return info.toString();
    }
    
    private static String formatSize(long bytes) {
        if (bytes < 0) return "Unknown";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }
    
    // ========== EXISTING METHODS (kept for compatibility) ==========
    
    public static OutputStream openOutputStream(String filePath, boolean append) throws Exception {
        FileConnection fc = (FileConnection) Connector.open(filePath, Connector.READ_WRITE);
        
        if (!fc.exists()) {
            fc.create();
        }
        
        OutputStream os;
        if (append) {
            os = fc.openOutputStream(fc.fileSize());
        } else {
            os = fc.openOutputStream();
        }
        
        return os;
    }
    
    public static InputStream openInputStream(String filePath) throws Exception {
        FileConnection fc = (FileConnection) Connector.open(filePath, Connector.READ);
        
        if (!fc.exists()) {
            fc.close();
            throw new Exception("File not found: " + filePath);
        }
        
        return fc.openInputStream();
    }
    
    public static boolean fileExists(String filePath) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(filePath, Connector.READ);
            boolean exists = fc.exists();
            fc.close();
            return exists;
        } catch (Exception e) {
            try { if (fc != null) fc.close(); } catch (Exception ex) {}
            return false;
        }
    }
    
    public static void deleteFile(String filePath) throws Exception {
        FileConnection fc = (FileConnection) Connector.open(filePath, Connector.READ_WRITE);
        
        if (fc.exists()) {
            fc.delete();
        }
        
        fc.close();
    }
    
    public static long getFileSize(String filePath) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(filePath, Connector.READ);
            
            if (!fc.exists()) {
                fc.close();
                return 0;
            }
            
            long size = fc.fileSize();
            fc.close();
            return size;
        } catch (Exception e) {
            try { if (fc != null) fc.close(); } catch (Exception ex) {}
            return 0;
        }
    }
    
    /**
     * ✅ NEW: Force re-detection of storage path
     */
    public static void resetStoragePath() {
        basePath = null;
        getDownloadPath(); // Trigger re-detection
    }
    
    /**
     * ✅ NEW: Manually set storage path
     */
    public static void setStoragePath(String path) {
        if (testAndCreatePath(path)) {
            basePath = path;
            saveToSettings(path);
        } else {
            throw new RuntimeException("Invalid storage path: " + path);
        }
    }
}