import javax.microedition.lcdui.*;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.Connector;
import java.util.Vector;

public class StatsCanvas extends Form implements CommandListener {
    private VidmateME midlet;
    private Displayable previousScreen;
    private Command backCmd = new Command("Retour", Command.BACK, 1);
    private Command refreshCmd = new Command("Actualiser", Command.SCREEN, 2);
    
    public StatsCanvas(VidmateME m, Displayable prev) {
        super("STATISTIQUES - UniMedia v2.1");
        midlet = m;
        previousScreen = prev;
        
        loadStatistics();
        
        addCommand(backCmd);
        addCommand(refreshCmd);
        setCommandListener(this);
    }
    
    public void commandAction(Command c, Displayable d) {
        if (c == backCmd) {
            midlet.getDisplay().setCurrent(previousScreen);
        } else if (c == refreshCmd) {
            deleteAll();
            loadStatistics();
        }
    }
    
    private void loadStatistics() {
        append(new Gauge("Chargement...", false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING));
        
        new Thread(new Runnable() {
            public void run() {
                final StatsData stats = calculateStats();
                
                Display.getDisplay(midlet).callSerially(new Runnable() {
                    public void run() {
                        deleteAll();
                        displayStats(stats);
                    }
                });
            }
        }).start();
    }
    
    private StatsData calculateStats() {
        StatsData stats = new StatsData();
        String basePath = StorageManager.getDownloadPath();
        
        // Count videos
        try {
            String videoPath = basePath + "videos/";
            FileConnection fc = (FileConnection) Connector.open(videoPath, Connector.READ);
            if (fc.exists() && fc.isDirectory()) {
                java.util.Enumeration files = fc.list();
                while (files.hasMoreElements()) {
                    String filename = (String) files.nextElement();
                    if (!filename.endsWith("/")) {
                        stats.videoCount++;
                        try {
                            FileConnection file = (FileConnection) Connector.open(videoPath + filename, Connector.READ);
                            stats.videoSize += file.fileSize();
                            file.close();
                        } catch (Exception e) {}
                    }
                }
            }
            fc.close();
        } catch (Exception e) {}
        
        // Count audio files
        try {
            String audioPath = basePath + "audios/";
            FileConnection fc = (FileConnection) Connector.open(audioPath, Connector.READ);
            if (fc.exists() && fc.isDirectory()) {
                java.util.Enumeration files = fc.list();
                while (files.hasMoreElements()) {
                    String filename = (String) files.nextElement();
                    if (!filename.endsWith("/")) {
                        stats.audioCount++;
                        try {
                            FileConnection file = (FileConnection) Connector.open(audioPath + filename, Connector.READ);
                            stats.audioSize += file.fileSize();
                            file.close();
                        } catch (Exception e) {}
                    }
                }
            }
            fc.close();
        } catch (Exception e) {}
        
        // Download queue stats
        Vector active = DownloadManager.getInstance().getActiveDownloads();
        Vector queued = DownloadManager.getInstance().getQueuedDownloads();
        Vector completed = DownloadManager.getInstance().getCompletedDownloads();
        
        stats.activeDownloads = active.size();
        stats.queuedDownloads = queued.size();
        stats.completedDownloads = completed.size();
        
        return stats;
    }
    
    private void displayStats(StatsData stats) {
        // Header
        StringItem header = new StringItem("", "=== STATISTIQUES GLOBALES ===\n\n");
        header.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
        append(header);
        
        // Library stats
        append(new Spacer(getWidth(), 5));
        StringItem libraryHeader = new StringItem("", ">> BIBLIOTHEQUE:\n");
        libraryHeader.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
        append(libraryHeader);
        
        append("Videos telechargees: " + stats.videoCount + "\n");
        append("Taille videos: " + formatSize(stats.videoSize) + "\n");
        append("Fichiers audio: " + stats.audioCount + "\n");
        append("Taille audio: " + formatSize(stats.audioSize) + "\n");
        append("TOTAL FICHIERS: " + (stats.videoCount + stats.audioCount) + "\n");
        append("TAILLE TOTALE: " + formatSize(stats.videoSize + stats.audioSize) + "\n");
        
        // Download queue stats
        append(new Spacer(getWidth(), 10));
        StringItem queueHeader = new StringItem("", ">> FILE DE TELECHARGEMENT:\n");
        queueHeader.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
        append(queueHeader);
        
        append("En cours: " + stats.activeDownloads + "\n");
        append("En attente: " + stats.queuedDownloads + "\n");
        append("Termines: " + stats.completedDownloads + "\n");
        
        // Settings info
        append(new Spacer(getWidth(), 10));
        StringItem settingsHeader = new StringItem("", ">> CONFIGURATION ACTUELLE:\n");
        settingsHeader.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
        append(settingsHeader);
        
        SettingsManager settings = SettingsManager.getInstance();
        append("Chemin: " + settings.getStoragePath() + "\n");
        append("Proxy: " + settings.getCurrentProxy() + "\n");
        append("API: " + settings.getCurrentApi() + "\n");
        append("Qualite: " + settings.getDefaultQuality() + "\n");
        
        int speedLimit = settings.getSpeedLimitKBps();
        append("Limite vitesse: " + (speedLimit == 0 ? "Illimitee" : speedLimit + " KB/s") + "\n");
        
        // App info
        append(new Spacer(getWidth(), 10));
        StringItem appHeader = new StringItem("", ">> APPLICATION:\n");
        appHeader.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
        append(appHeader);
        
        append("Version: UniMedia v2.1\n");
        append("Backend: Dashtube API (primary)\n");
        append("Memoire libre: " + (Runtime.getRuntime().freeMemory() / 1024) + " KB\n");
        append("Memoire totale: " + (Runtime.getRuntime().totalMemory() / 1024) + " KB\n");
        
        // Footer
        append(new Spacer(getWidth(), 10));
        StringItem footer = new StringItem("", 
            "=========================\n" +
            "Appuyez sur 'Actualiser' pour\n" +
            "mettre a jour les statistiques.\n");
        footer.setLayout(Item.LAYOUT_NEWLINE_AFTER);
        append(footer);
    }
    
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / 1024 / 1024) + " MB";
    }
}

class StatsData {
    public int videoCount = 0;
    public int audioCount = 0;
    public long videoSize = 0;
    public long audioSize = 0;
    public int activeDownloads = 0;
    public int queuedDownloads = 0;
    public int completedDownloads = 0;
}