import javax.microedition.rms.*;
import java.io.*;

public class SettingsManager {
    private static SettingsManager instance;
    private RecordStore settingsStore;
    
    // Parametres par defaut
    private String storagePath = "file:///E:/VidmateME/";
    private String currentProxy = "Direct";
    private String currentApi = "S60Tube";
    private boolean showThumbnails = true;
    private boolean downloadAudio = false;
    private String defaultQuality = "360p";
    
    private SettingsManager() {
        loadSettings();
    }
    
    public static synchronized SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }
        return instance;
    }
    
    private void loadSettings() {
        try {
            settingsStore = RecordStore.openRecordStore("VidmateSettings", true);
            if (settingsStore.getNumRecords() > 0) {
                byte[] data = settingsStore.getRecord(1);
                DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
                storagePath = dis.readUTF();
                currentProxy = dis.readUTF();
                currentApi = dis.readUTF();
                showThumbnails = dis.readBoolean();
                downloadAudio = dis.readBoolean();
                defaultQuality = dis.readUTF();
                dis.close();
            }
        } catch (Exception e) {
            // Utiliser les valeurs par defaut si erreur
        }
    }
    
    public void saveSettings() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeUTF(storagePath);
            dos.writeUTF(currentProxy);
            dos.writeUTF(currentApi);
            dos.writeBoolean(showThumbnails);
            dos.writeBoolean(downloadAudio);
            dos.writeUTF(defaultQuality);
            dos.close();
            
            byte[] data = baos.toByteArray();
            if (settingsStore.getNumRecords() == 0) {
                settingsStore.addRecord(data, 0, data.length);
            } else {
                settingsStore.setRecord(1, data, 0, data.length);
            }
        } catch (Exception e) {
            // Ignorer les erreurs de sauvegarde
        }
    }
    
    // Getters et Setters
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String path) { storagePath = path; }
    
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
    
    public void close() {
        try {
            if (settingsStore != null) {
                settingsStore.closeRecordStore();
            }
        } catch (Exception e) {}
    }
}