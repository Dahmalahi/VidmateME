import javax.microedition.lcdui.*;
import java.util.Vector;

public class DownloadsCanvas extends Canvas implements CommandListener, Runnable {
    private VidmateME midlet;
    private final Command refreshCmd = new Command("Actualiser", Command.SCREEN, 1);
    private final Command backCmd = new Command("Retour", Command.BACK, 2);
    private final Command cancelCmd = new Command("Annuler", Command.ITEM, 3);
    
    private Vector activeDownloads = new Vector();
    private Vector queuedDownloads = new Vector();
    private Vector completedDownloads = new Vector();
    
    private Thread refreshThread;
    private boolean running = true;
    private int selectedIndex = 0;
    
    // Couleurs
    private static final int COLOR_BG = 0x000000;
    private static final int COLOR_HEADER = 0xFF3B30;
    private static final int COLOR_TEXT = 0xFFFFFF;
    private static final int COLOR_PROGRESS_BG = 0x333333;
    private static final int COLOR_PROGRESS_FILL = 0x00D9FF;
    private static final int COLOR_SUCCESS = 0x30D158;
    private static final int COLOR_MUTED = 0x8E8E93;
    
    public DownloadsCanvas(VidmateME m) {
        midlet = m;
        addCommand(refreshCmd);
        addCommand(backCmd);
        addCommand(cancelCmd);
        setCommandListener(this);
        
        // Thread pour rafraîchir automatiquement
        refreshThread = new Thread(this);
        refreshThread.start();
        
        refresh();
    }
    
    public void refresh() {
        activeDownloads = DownloadManager.getInstance().getActiveDownloads();
        queuedDownloads = DownloadManager.getInstance().getQueuedDownloads();
        completedDownloads = DownloadManager.getInstance().getCompletedDownloads();
        repaint();
    }
    
    public void run() {
        while (running) {
            try {
                refresh();
                Thread.sleep(500); // Rafraîchir toutes les 500ms
            } catch (Exception e) {
                break;
            }
        }
    }
    
    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        
        // Fond
        g.setColor(COLOR_BG);
        g.fillRect(0, 0, w, h);
        
        // Header
        g.setColor(COLOR_HEADER);
        g.fillRect(0, 0, w, 30);
        g.setColor(COLOR_TEXT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
        g.drawString("TELECHARGEMENTS", w/2, 8, Graphics.HCENTER | Graphics.TOP);
        
        int y = 35;
        
        // En cours
        if (activeDownloads.size() > 0) {
            g.setColor(COLOR_TEXT);
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
            g.drawString(">> EN COURS:", 5, y, Graphics.LEFT | Graphics.TOP);
            y += 20;
            
            for (int i = 0; i < activeDownloads.size(); i++) {
                DownloadItem item = (DownloadItem) activeDownloads.elementAt(i);
                y = paintDownloadItem(g, item, y, w, true);
            }
        }
        
        // En attente
        if (queuedDownloads.size() > 0) {
            g.setColor(COLOR_TEXT);
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
            g.drawString(">> EN ATTENTE:", 5, y, Graphics.LEFT | Graphics.TOP);
            y += 20;
            
            for (int i = 0; i < queuedDownloads.size() && i < 3; i++) {
                DownloadItem item = (DownloadItem) queuedDownloads.elementAt(i);
                y = paintQueuedItem(g, item, y, w);
            }
            
            if (queuedDownloads.size() > 3) {
                g.setColor(COLOR_MUTED);
                g.drawString("... +" + (queuedDownloads.size() - 3) + " autres", 10, y, Graphics.LEFT | Graphics.TOP);
                y += 15;
            }
        }
        
        // Terminés (derniers 3)
        if (completedDownloads.size() > 0) {
            g.setColor(COLOR_TEXT);
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
            g.drawString(">> TERMINES:", 5, y, Graphics.LEFT | Graphics.TOP);
            y += 20;
            
            int start = Math.max(0, completedDownloads.size() - 3);
            for (int i = start; i < completedDownloads.size(); i++) {
                DownloadItem item = (DownloadItem) completedDownloads.elementAt(i);
                y = paintCompletedItem(g, item, y, w);
            }
        }
        
        // Message si vide
        if (activeDownloads.size() == 0 && queuedDownloads.size() == 0 && completedDownloads.size() == 0) {
            g.setColor(COLOR_MUTED);
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM));
            g.drawString("Aucun telechargement", w/2, h/2 - 20, Graphics.HCENTER | Graphics.TOP);
            g.drawString("Allez dans 'Rechercher' ou", w/2, h/2, Graphics.HCENTER | Graphics.TOP);
            g.drawString("'Convertir lien'", w/2, h/2 + 20, Graphics.HCENTER | Graphics.TOP);
        }
        
        // Instructions
        g.setColor(COLOR_MUTED);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        g.drawString("Auto-refresh actif", w/2, h - 15, Graphics.HCENTER | Graphics.TOP);
    }
    
    private int paintDownloadItem(Graphics g, DownloadItem item, int y, int w, boolean showProgress) {
        // Nom du fichier
        g.setColor(COLOR_TEXT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        String title = item.getVideoTitle();
        if (title.length() > 30) {
            title = title.substring(0, 27) + "...";
        }
        g.drawString(title, 10, y, Graphics.LEFT | Graphics.TOP);
        y += 15;
        
        // Barre de progression
        if (showProgress) {
            int barW = w - 20;
            int barH = 15;
            int barX = 10;
            
            // Fond
            g.setColor(COLOR_PROGRESS_BG);
            g.fillRoundRect(barX, y, barW, barH, 5, 5);
            
            // Remplissage
            int progress = item.getProgress();
            int fillW = (barW * progress) / 100;
            if (fillW > 0) {
                g.setColor(COLOR_PROGRESS_FILL);
                g.fillRoundRect(barX, y, fillW, barH, 5, 5);
            }
            
            // Pourcentage
            g.setColor(COLOR_TEXT);
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
            g.drawString(progress + "%", w/2, y + 3, Graphics.HCENTER | Graphics.TOP);
            
            y += 20;
            
            // Taille
            long downloaded = item.getDownloadedSize();
            long total = item.getTotalSize();
            if (total > 0) {
                g.setColor(COLOR_MUTED);
                g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
                String sizeStr = formatSize(downloaded) + " / " + formatSize(total);
                g.drawString(sizeStr, 10, y, Graphics.LEFT | Graphics.TOP);
                y += 15;
            }
        }
        
        y += 5;
        return y;
    }
    
    private int paintQueuedItem(Graphics g, DownloadItem item, int y, int w) {
        g.setColor(COLOR_MUTED);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        String title = item.getVideoTitle();
        if (title.length() > 30) {
            title = title.substring(0, 27) + "...";
        }
        g.drawString("- " + title, 10, y, Graphics.LEFT | Graphics.TOP);
        return y + 15;
    }
    
    private int paintCompletedItem(Graphics g, DownloadItem item, int y, int w) {
        g.setColor(COLOR_SUCCESS);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        String title = item.getVideoTitle();
        if (title.length() > 30) {
            title = title.substring(0, 27) + "...";
        }
        g.drawString("[OK] " + title, 10, y, Graphics.LEFT | Graphics.TOP);
        return y + 15;
    }
    
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }
    
    protected void keyPressed(int keyCode) {
        if (keyCode == Canvas.KEY_NUM0) {
            stop();
            midlet.backToMenu();
        }
    }
    
    public void commandAction(Command c, Displayable d) {
        if (c == backCmd) {
            stop();
            midlet.backToMenu();
        } else if (c == refreshCmd) {
            refresh();
        } else if (c == cancelCmd) {
            // Annuler le téléchargement sélectionné
            if (activeDownloads.size() > 0) {
                DownloadItem item = (DownloadItem) activeDownloads.elementAt(0);
                DownloadManager.getInstance().cancelDownload(item);
                refresh();
            }
        }
    }
    
    public void stop() {
        running = false;
        if (refreshThread != null) {
            refreshThread.interrupt();
        }
    }
}
