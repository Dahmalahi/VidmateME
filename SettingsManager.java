import javax.microedition.rms.*;
import java.io.*;

public class SettingsManager {
    private static SettingsManager instance;
    private RecordStore settingsStore;
    
    // ✅ CHANGED: Default path uses VidmateME (will be auto-detected)
    private String storagePath = ""; // Empty = auto-detect on first access
    private String currentProxy = "Direct";
    private String currentApi = "Dashtube";
    private boolean showThumbnails = true;
    private boolean downloadAudio = false;
    private String defaultQuality = "360p";
    private int speedLimitKBps = 0;
    
    private static final int SETTINGS_VERSION = 2;
    
    private SettingsManager() {
        loadSettings();
        
        // ✅ NEW: Auto-detect storage if empty
        if (storagePath == null || storagePath.length() == 0) {
            storagePath = StorageManager.getDownloadPath();
            saveSettings();
        }
    }
    
    public static synchronized SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }
        return instance;
    }
    
    private void loadSettings() {
        try {
            settingsStore = RecordStore.openRecordStore("UniMediaSettings", true);
            
            if (settingsStore.getNumRecords() > 0) {
                byte[] data = settingsStore.getRecord(1);
                DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
                
                int version = 1;
                try {
                    version = dis.readInt();
                } catch (Exception e) {
                    dis.close();
                    dis = new DataInputStream(new ByteArrayInputStream(data));
                }
                
                if (version == 1) {
                    loadV1Settings(dis);
                    saveSettings();
                } else if (version >= 2) {
                    loadV2Settings(dis);
                }
                
                dis.close();
            } else {
                migrateOldSettings();
            }
        } catch (Exception e) {
        }
    }
    
    private void loadV1Settings(DataInputStream dis) throws IOException {
        storagePath = dis.readUTF();
        currentProxy = dis.readUTF();
        currentApi = dis.readUTF();
        showThumbnails = dis.readBoolean();
        downloadAudio = dis.readBoolean();
        defaultQuality = dis.readUTF();
        
        // ✅ CHANGED: Don't change UniMedia to VidmateME
        // Let auto-detection handle it
        
        if (currentApi.equals("Asepharyana")) {
            currentApi = "Dashtube";
        }
    }
    
    private void loadV2Settings(DataInputStream dis) throws IOException {
        storagePath = dis.readUTF();
        currentProxy = dis.readUTF();
        currentApi = dis.readUTF();
        showThumbnails = dis.readBoolean();
        downloadAudio = dis.readBoolean();
        defaultQuality = dis.readUTF();
        speedLimitKBps = dis.readInt();
    }
    
    private void migrateOldSettings() {
        RecordStore oldStore = null;
        try {
            oldStore = RecordStore.openRecordStore("VidmateSettings", false);
            if (oldStore.getNumRecords() > 0) {
                byte[] data = oldStore.getRecord(1);
                DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
                loadV1Settings(dis);
                dis.close();
                
                saveSettings();
                
                oldStore.closeRecordStore();
                RecordStore.deleteRecordStore("VidmateSettings");
            }
        } catch (RecordStoreNotFoundException e) {
        } catch (Exception e) {
        } finally {
            try {
                if (oldStore != null) oldStore.closeRecordStore();
            } catch (Exception e) {}
        }
    }
    
    public void saveSettings() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            
            dos.writeInt(SETTINGS_VERSION);
            dos.writeUTF(storagePath);
            dos.writeUTF(currentProxy);
            dos.writeUTF(currentApi);
            dos.writeBoolean(showThumbnails);
            dos.writeBoolean(downloadAudio);
            dos.writeUTF(defaultQuality);
            dos.writeInt(speedLimitKBps);
            
            dos.close();
            
            byte[] data = baos.toByteArray();
            if (settingsStore.getNumRecords() == 0) {
                settingsStore.addRecord(data, 0, data.length);
            } else {
                settingsStore.setRecord(1, data, 0, data.length);
            }
        } catch (Exception e) {
        }
    }
    
    private static String replaceString(String str, String pattern, String replacement) {
        if (str == null || pattern == null || replacement == null) return str;
        
        StringBuffer result = new StringBuffer();
        int s = 0, e;
        
        while ((e = str.indexOf(pattern, s)) >= 0) {
            result.append(str.substring(s, e));
            result.append(replacement);
            s = e + pattern.length();
        }
        result.append(str.substring(s));
        
        return result.toString();
    }
    
    public String getStoragePath() { 
        if (storagePath == null || storagePath.length() == 0) {
            storagePath = StorageManager.getDownloadPath();
        }
        return storagePath; 
    }
    
    public void setStoragePath(String path) { 
        storagePath = path; 
    }
    
    public String getCurrentProxy() { return currentProxy; }
    public void setCurrentProxy(String proxy) { currentProxy = proxy; }
    
    public String getCurrentApi() { return currentApi; }
    public void setCurrentApi(String api) { currentApi = api; }
    
    public boolean isShowThumbnails() { return showThumbnails; }
    public void setShowThumbnails(boolean show) { showThumbnails = show; }
    
    public boolean isDownloadAudio() { return downloadAudio; }
    public void setDownloadAudio(boolean audio) { downloadAudio = audio; }
    
    public String getDefaultQuality() { return defaultQuality; }
    public void setDefaultQuality(String quality) { defaultQuality = quality; }
    
    public int getSpeedLimitKBps() { return speedLimitKBps; }
    public void setSpeedLimitKBps(int kbps) {
        if (kbps < 0) kbps = 0;
        this.speedLimitKBps = kbps;
    }
    
    public String getSpeedLimitDisplay() {
        if (speedLimitKBps == 0) {
            return "Illimitee";
        } else {
            return speedLimitKBps + " KB/s";
        }
    }
    
    public void resetToDefaults() {
        storagePath = StorageManager.getDownloadPath(); // ✅ Auto-detect
        currentProxy = "Direct";
        currentApi = "Dashtube";
        showThumbnails = true;
        downloadAudio = false;
        defaultQuality = "360p";
        speedLimitKBps = 0;
        saveSettings();
    }
    
    public String getSettingsSummary() {
        StringBuffer sb = new StringBuffer();
        sb.append("Storage: ").append(getStoragePath()).append("\n");
        sb.append("Proxy: ").append(currentProxy).append("\n");
        sb.append("API: ").append(currentApi).append("\n");
        sb.append("Quality: ").append(defaultQuality).append("\n");
        sb.append("Speed Limit: ").append(getSpeedLimitDisplay()).append("\n");
        sb.append("Thumbnails: ").append(showThumbnails ? "Yes" : "No").append("\n");
        sb.append("Audio Mode: ").append(downloadAudio ? "Yes" : "No").append("\n");
        return sb.toString();
    }
    
    public void close() {
        try {
            if (settingsStore != null) {
                settingsStore.closeRecordStore();
            }
        } catch (Exception e) {
        }
    }
}