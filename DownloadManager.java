import javax.microedition.lcdui.*;
import java.util.Vector;

public class DownloadManager implements Runnable {
    private static DownloadManager instance;
    private Vector activeDownloads = new Vector();
    private Vector queuedDownloads = new Vector();
    private Vector completedDownloads = new Vector();
    private Thread downloadThread;
    private boolean running = true;
    
    private DownloadManager() {
        downloadThread = new Thread(this);
        downloadThread.start();
    }
    
    public static synchronized DownloadManager getInstance() {
        if (instance == null) {
            instance = new DownloadManager();
        }
        return instance;
    }
    
    public void queueDownload(VideoItem item) {
        synchronized (queuedDownloads) {
            // CORRECTION: Utiliser le format de fichier correct
            String extension = ".mp4"; // Par défaut
            String folderName = "videos/";
            
            if (item.fileFormat != null && item.fileFormat.length() > 0) {
                extension = "." + item.fileFormat;
                
                // Déterminer le dossier selon le type
                if (item.fileFormat.equals("mp3") || item.fileFormat.equals("aac") || item.fileFormat.equals("wav")) {
                    folderName = "audios/";
                } else {
                    folderName = "videos/";
                }
            }
            
            String filename = sanitize(item.title) + extension;
            String downloadPath = StorageManager.getDownloadPath() + folderName;
            
            // Créer le dossier si nécessaire
            try {
                javax.microedition.io.file.FileConnection dir = 
                    (javax.microedition.io.file.FileConnection) 
                    javax.microedition.io.Connector.open(downloadPath, 
                        javax.microedition.io.Connector.READ_WRITE);
                if (!dir.exists()) dir.mkdir();
                dir.close();
            } catch (Exception e) {}
            
            String filePath = downloadPath + filename;
            DownloadItem dlItem = new DownloadItem(item.videoId, item.title, item.downloadUrl, filePath);
            queuedDownloads.addElement(dlItem);
            queuedDownloads.notifyAll();
        }
    }
    
    public void run() {
        while (running) {
            DownloadItem item = null;
            synchronized (queuedDownloads) {
                while (queuedDownloads.isEmpty() && running) {
                    try { queuedDownloads.wait(); } catch (InterruptedException e) { return; }
                }
                if (!queuedDownloads.isEmpty()) {
                    item = (DownloadItem) queuedDownloads.elementAt(0);
                    queuedDownloads.removeElementAt(0);
                    activeDownloads.addElement(item);
                }
            }
            
            if (item != null) {
                downloadFile(item);
            }
        }
    }
    
    private void downloadFile(DownloadItem item) {
        javax.microedition.io.HttpConnection conn = null;
        java.io.InputStream is = null;
        java.io.OutputStream os = null;
        
        try {
            item.setStatus("DOWNLOADING");
            
            conn = (javax.microedition.io.HttpConnection) 
                javax.microedition.io.Connector.open(item.getDownloadUrl(), 
                    javax.microedition.io.Connector.READ, true);
            conn.setRequestMethod(javax.microedition.io.HttpConnection.GET);
            conn.setRequestProperty("User-Agent", 
                "Mozilla/5.0 (S60V3; U; en) AppleWebKit/413");
            
            int rc = conn.getResponseCode();
            if (rc != javax.microedition.io.HttpConnection.HTTP_OK) {
                throw new java.io.IOException("HTTP error: " + rc);
            }
            
            long totalSize = conn.getLength();
            item.setTotalSize(totalSize);
            
            os = StorageManager.openOutputStream(item.getFilePath(), false);
            is = conn.openInputStream();
            
            byte[] buffer = new byte[1024];
            int len;
            long downloaded = 0;
            long lastUpdate = System.currentTimeMillis();
            
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
                downloaded += len;
                
                long now = System.currentTimeMillis();
                if (now - lastUpdate > 500) {
                    item.updateProgress(downloaded, totalSize);
                    lastUpdate = now;
                }
            }
            
            os.flush();
            os.close();
            is.close();
            conn.close();
            
            item.setStatus("COMPLETED");
            synchronized (activeDownloads) {
                activeDownloads.removeElement(item);
                completedDownloads.addElement(item);
            }
            
            notifyUser("Succes", "Telechargement termine: " + item.getVideoTitle());
            
        } catch (Exception e) {
            item.setStatus("FAILED");
            synchronized (activeDownloads) {
                activeDownloads.removeElement(item);
            }
            notifyUser("Erreur", "Echec: " + e.getMessage());
        } finally {
            try { if (os != null) os.close(); } catch (Exception e) {}
            try { if (is != null) is.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }
    
    public Vector getActiveDownloads() {
        synchronized (activeDownloads) {
            Vector clone = new Vector();
            for (int i = 0; i < activeDownloads.size(); i++) {
                clone.addElement(activeDownloads.elementAt(i));
            }
            return clone;
        }
    }
    
    public Vector getQueuedDownloads() {
        synchronized (queuedDownloads) {
            Vector clone = new Vector();
            for (int i = 0; i < queuedDownloads.size(); i++) {
                clone.addElement(queuedDownloads.elementAt(i));
            }
            return clone;
        }
    }
    
    public Vector getCompletedDownloads() {
        synchronized (completedDownloads) {
            Vector clone = new Vector();
            for (int i = 0; i < completedDownloads.size(); i++) {
                clone.addElement(completedDownloads.elementAt(i));
            }
            return clone;
        }
    }
    
    public void pauseDownload(DownloadItem item) {
        item.setStatus("PAUSED");
    }
    
    public void resumeDownload(DownloadItem item) {
        if ("PAUSED".equals(item.getStatus())) {
            queueDownloadFromItem(item);
        }
    }
    
    private void queueDownloadFromItem(DownloadItem item) {
        synchronized (queuedDownloads) {
            queuedDownloads.addElement(item);
            queuedDownloads.notifyAll();
        }
    }
    
    public void cancelDownload(DownloadItem item) {
        item.setStatus("CANCELLED");
        synchronized (activeDownloads) {
            activeDownloads.removeElement(item);
        }
        synchronized (queuedDownloads) {
            queuedDownloads.removeElement(item);
        }
        try {
            javax.microedition.io.file.FileConnection fc = 
                (javax.microedition.io.file.FileConnection) 
                javax.microedition.io.Connector.open(item.getFilePath());
            if (fc.exists()) fc.delete();
            fc.close();
        } catch (Exception e) {}
    }
    
    private String sanitize(String s) {
        StringBuffer clean = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || 
                (c >= '0' && c <= '9') || c == '_' || c == '-' || c == ' ') {
                clean.append(c);
            } else if (clean.length() > 0 && clean.charAt(clean.length() - 1) != '_') {
                clean.append('_');
            }
        }
        if (clean.length() == 0) clean.append("video");
        if (clean.length() > 30) clean.setLength(30);
        return clean.toString();
    }
    
    private void notifyUser(final String title, final String msg) {
        Display.getDisplay(VidmateME.instance).callSerially(new Runnable() {
            public void run() {
                Alert a = new Alert(title, msg, null, AlertType.INFO);
                a.setTimeout(3000);
                Display.getDisplay(VidmateME.instance).setCurrent(a);
            }
        });
    }
    
    public void shutdown() {
        running = false;
        synchronized (queuedDownloads) {
            queuedDownloads.notifyAll();
        }
        try { downloadThread.join(); } catch (Exception e) {}
    }
}

class DownloadItem {
    private String videoId;
    private String videoTitle;
    private String downloadUrl;
    private String filePath;
    private String status;
    private long totalSize;
    private long downloadedSize;
    private int progress;
    
    public DownloadItem(String videoId, String videoTitle, String downloadUrl, String filePath) {
        this.videoId = videoId;
        this.videoTitle = videoTitle;
        this.downloadUrl = downloadUrl;
        this.filePath = filePath;
        this.status = "QUEUED";
        this.totalSize = 0;
        this.downloadedSize = 0;
        this.progress = 0;
    }
    
    public boolean isDownloading() {
        return "DOWNLOADING".equals(status);
    }
    
    public boolean isPaused() {
        return "PAUSED".equals(status);
    }
    
    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }
    
    public boolean isCancelled() {
        return "CANCELLED".equals(status);
    }
    
    public void updateProgress(long downloaded, long total) {
        this.downloadedSize = downloaded;
        this.totalSize = total;
        if (total > 0) {
            this.progress = (int)((downloaded * 100) / total);
        }
    }
    
    public String getVideoTitle() {
        return videoTitle;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public int getProgress() {
        return progress;
    }
    
    public long getTotalSize() {
        return totalSize;
    }
    
    public void setTotalSize(long size) {
        this.totalSize = size;
    }
    
    public long getDownloadedSize() {
        return downloadedSize;
    }
    
    public long getSpeed() {
        return 0;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getStatus() {
        return status;
    }
    
    public String getDownloadUrl() {
        return downloadUrl;
    }
}
