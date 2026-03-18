import javax.microedition.lcdui.*;
import javax.microedition.media.*;
import javax.microedition.media.control.*;

public class AudioPlayerCanvas extends Canvas implements CommandListener, Runnable {
    private VidmateME midlet;
    private Player player;
    private VolumeControl volumeControl;
    private String status = "Chargement...";
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private boolean running = true;
    private int volumeLevel = 75;
    private String fileName = "";
    private long duration = 0;
    private long currentTime = 0;
    
    // ✅ Store file path as final field
    private final String filePath;
    
    // ✅ NEW: Animation frame for waveform
    private int animFrame = 0;
    private Thread animThread;
    
    // ✅ NEW: File size info
    private long fileSize = 0;
    
    // ✅ CHANGED: Updated color scheme for UniMedia v2.1
    private static final int COLOR_BG = 0x0A0E27;        // Deep blue-black
    private static final int COLOR_PRIMARY = 0x00D9FF;   // Cyan
    private static final int COLOR_ACCENT = 0xFF6B35;    // Orange
    private static final int COLOR_TEXT = 0xFFFFFF;
    private static final int COLOR_MUTED = 0x8E8E93;
    private static final int COLOR_SUCCESS = 0x30D158;   // Green for playing
    
    private final Command backCmd = new Command("Retour", Command.BACK, 1);
    private final Command pauseCmd = new Command("Pause/Play", Command.OK, 2);
    // ✅ NEW: File info command
    private final Command infoCmd = new Command("Infos", Command.SCREEN, 3);
    
    public AudioPlayerCanvas(VidmateME m, final String filePathParam, final String name) {
        midlet = m;
        fileName = name;
        filePath = filePathParam;
        
        setCommandListener(this);
        addCommand(backCmd);
        addCommand(pauseCmd);
        addCommand(infoCmd);
        
        // ✅ NEW: Get file size
        try {
            fileSize = StorageManager.getFileSize(filePath);
        } catch (Exception e) {
            fileSize = 0;
        }
        
        // Start player loading
        new Thread(this).start();
        
        // ✅ NEW: Start animation thread
        animThread = new Thread(new Runnable() {
            public void run() {
                while (running) {
                    try {
                        animFrame = (animFrame + 1) % 60;
                        repaint();
                        Thread.sleep(50); // 20 FPS
                    } catch (Exception e) {
                        break;
                    }
                }
            }
        });
        animThread.start();
    }
    
    public void run() {
        try {
            status = "Initialisation...";
            repaint();
            
            // Ensure proper file:/// format
            String locator = filePath;
            if (!locator.startsWith("file:///")) {
                if (locator.startsWith("file://")) {
                    locator = "file:///" + locator.substring(7);
                } else {
                    locator = "file:///" + locator;
                }
            }
            
            status = "Chargement fichier...";
            repaint();
            
            player = Manager.createPlayer(locator);
            
            status = "Preparation...";
            repaint();
            
            player.realize();
            
            volumeControl = (VolumeControl) player.getControl("VolumeControl");
            if (volumeControl != null) {
                volumeControl.setLevel(volumeLevel);
            }
            
            player.start();
            isPlaying = true;
            status = "Lecture en cours";
            repaint();
            
        } catch (Exception e) {
            status = "Erreur: " + e.getMessage();
            repaint();
            
            Alert error = new Alert("Erreur Lecture",
                "Impossible de lire le fichier:\n" + 
                e.getMessage() + "\n\n" +
                "Verifiez que le fichier existe\n" +
                "dans: " + StorageManager.getDownloadPath() + "audios/",
                null, AlertType.ERROR);
            error.setTimeout(5000);
            midlet.getDisplay().setCurrent(error, this);
            
            try { Thread.sleep(3000); } catch (Exception ex) {}
            cleanup();
            midlet.backToMenu();
        }
    }
    
    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        
        // Background
        g.setColor(COLOR_BG);
        g.fillRect(0, 0, w, h);
        
        // ✅ CHANGED: Top header bar
        g.setColor(COLOR_PRIMARY);
        g.fillRect(0, 0, w, 45);
        g.setColor(COLOR_TEXT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE));
        g.drawString("Lecteur Audio", w/2, 10, Graphics.HCENTER | Graphics.TOP);
        
        // ✅ NEW: Status indicator
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        if (isPlaying) {
            g.setColor(COLOR_SUCCESS);
            g.drawString("[PLAYING]", w/2, 28, Graphics.HCENTER | Graphics.TOP);
        } else if (isPaused) {
            g.setColor(COLOR_ACCENT);
            g.drawString("[PAUSED]", w/2, 28, Graphics.HCENTER | Graphics.TOP);
        } else {
            g.setColor(COLOR_MUTED);
            g.drawString("[STOPPED]", w/2, 28, Graphics.HCENTER | Graphics.TOP);
        }
        
        // ✅ CHANGED: File name with ellipsis
        g.setColor(COLOR_TEXT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
        String displayName = fileName;
        if (displayName.length() > 28) {
            displayName = displayName.substring(0, 25) + "...";
        }
        g.drawString(displayName, w/2, 55, Graphics.HCENTER | Graphics.TOP);
        
        // ✅ NEW: File size
        if (fileSize > 0) {
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
            g.setColor(COLOR_MUTED);
            g.drawString(formatSize(fileSize), w/2, 72, Graphics.HCENTER | Graphics.TOP);
        }
        
        // ✅ NEW: Fake waveform visualization
        if (isPlaying) {
            drawWaveform(g, w, 95);
        }
        
        // Progress bar
        int barW = w - 40;
        int barH = 25;
        int barX = 20;
        int barY = 140;
        
        // Bar background
        g.setColor(0x1A1A2E);
        g.fillRoundRect(barX, barY, barW, barH, 8, 8);
        
        // Progress fill
        if (player != null) {
            try {
                duration = player.getDuration();
                if (duration > 0) {
                    currentTime = player.getMediaTime();
                    
                    long durationSec = duration / 1000000;
                    long currentSec = currentTime / 1000000;
                    
                    if (durationSec > 0) {
                        int progress = (int)((currentSec * barW) / durationSec);
                        
                        g.setColor(COLOR_PRIMARY);
                        g.fillRoundRect(barX, barY, progress, barH, 8, 8);
                        
                        // ✅ NEW: Progress percentage
                        int percent = (int)((currentSec * 100) / durationSec);
                        g.setColor(COLOR_TEXT);
                        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
                        g.drawString(percent + "%", w/2, barY + 7, Graphics.HCENTER | Graphics.TOP);
                    }
                    
                    // Time display
                    g.setColor(COLOR_TEXT);
                    g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM));
                    String timeStr = formatTime(currentSec) + " / " + formatTime(durationSec);
                    g.drawString(timeStr, w/2, barY + barH + 8, Graphics.HCENTER | Graphics.TOP);
                }
            } catch (Exception e) {}
        }
        
        // Status
        g.setColor(COLOR_MUTED);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        g.drawString(status, w/2, 195, Graphics.HCENTER | Graphics.TOP);
        
        // Volume control
        int volW = 120;
        int volH = 12;
        int volX = (w - volW) / 2;
        int volY = 220;
        
        g.setColor(0x1A1A2E);
        g.fillRoundRect(volX, volY, volW, volH, 6, 6);
        
        int volFill = (volW * volumeLevel) / 100;
        g.setColor(COLOR_ACCENT);
        g.fillRoundRect(volX, volY, volFill, volH, 6, 6);
        
        // Volume icon and percentage
        g.setColor(COLOR_TEXT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        g.drawString("♪", volX - 15, volY + 1, Graphics.LEFT | Graphics.TOP);
        g.drawString(volumeLevel + "%", volX + volW + 5, volY + 1, Graphics.LEFT | Graphics.TOP);
        
        // ✅ CHANGED: Better controls display
        g.setColor(COLOR_PRIMARY);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        int ctrlY = 245;
        g.drawString("5: Play/Pause", w/2, ctrlY, Graphics.HCENTER | Graphics.TOP);
        g.drawString("4: Vol-    6: Vol+", w/2, ctrlY + 15, Graphics.HCENTER | Graphics.TOP);
        g.drawString("0: Quitter", w/2, ctrlY + 30, Graphics.HCENTER | Graphics.TOP);
        
        // ✅ NEW: Storage location
        g.setColor(COLOR_MUTED);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        g.drawString("UniMedia v2.1 Audio Player", w/2, h - 15, Graphics.HCENTER | Graphics.TOP);
    }
    
    /**
     * ✅ NEW: Draw animated waveform visualization
     */
    private void drawWaveform(Graphics g, int w, int y) {
        int barCount = 12;
        int barWidth = 4;
        int barSpacing = 8;
        int totalWidth = (barCount * (barWidth + barSpacing)) - barSpacing;
        int startX = (w - totalWidth) / 2;
        int maxHeight = 25;
        
        g.setColor(COLOR_PRIMARY);
        
        for (int i = 0; i < barCount; i++) {
            // Pseudo-random height based on animation frame
            int seed = (animFrame + i * 7) % 30;
            int height = 5 + (seed % maxHeight);
            
            int x = startX + (i * (barWidth + barSpacing));
            int barY = y + (maxHeight - height) / 2;
            
            // Alternate colors
            if (i % 2 == 0) {
                g.setColor(COLOR_PRIMARY);
            } else {
                g.setColor(COLOR_ACCENT);
            }
            
            g.fillRect(x, barY, barWidth, height);
        }
    }
    
    /**
     * ✅ NEW: Format file size
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }
    
    protected void keyPressed(int keyCode) {
        if (keyCode == Canvas.KEY_NUM5) {
            togglePlayPause();
        } else if (keyCode == Canvas.KEY_NUM0) {
            cleanup();
            midlet.backToMenu();
        } else if (keyCode == Canvas.KEY_NUM4 && volumeControl != null) {
            volumeLevel = Math.max(0, volumeLevel - 10);
            volumeControl.setLevel(volumeLevel);
            repaint();
        } else if (keyCode == Canvas.KEY_NUM6 && volumeControl != null) {
            volumeLevel = Math.min(100, volumeLevel + 10);
            volumeControl.setLevel(volumeLevel);
            repaint();
        }
    }
    
    private void togglePlayPause() {
        if (player == null) return;
        try {
            if (isPlaying) {
                player.stop();
                isPlaying = false;
                isPaused = true;
                status = "En pause";
            } else {
                player.start();
                isPlaying = true;
                isPaused = false;
                status = "Lecture en cours";
            }
            repaint();
        } catch (Exception e) {
            status = "Erreur: " + e.getMessage();
            repaint();
        }
    }
    
    /**
     * ✅ CHANGED: Renamed from stopPlayer to cleanup
     */
    private void cleanup() {
        running = false;
        
        if (animThread != null) {
            animThread.interrupt();
        }
        
        if (player != null) {
            try {
                player.stop();
                player.deallocate();
                player.close();
            } catch (Exception e) {}
            player = null;
            volumeControl = null;
        }
    }
    
    private String formatTime(long seconds) {
        long mins = seconds / 60;
        long secs = seconds % 60;
        return (mins < 10 ? "0" : "") + mins + ":" + (secs < 10 ? "0" : "") + secs;
    }
    
    /**
     * ✅ NEW: Show file information
     */
    private void showFileInfo() {
        Form infoForm = new Form("Informations Fichier");
        
        infoForm.append("Nom:\n" + fileName + "\n\n");
        
        if (fileSize > 0) {
            infoForm.append("Taille: " + formatSize(fileSize) + "\n\n");
        }
        
        infoForm.append("Chemin:\n" + filePath + "\n\n");
        
        if (player != null) {
            try {
                long dur = player.getDuration() / 1000000;
                infoForm.append("Duree: " + formatTime(dur) + "\n\n");
            } catch (Exception e) {}
        }
        
        infoForm.append("Stockage:\n" + StorageManager.getDownloadPath() + "audios/\n\n");
        
        infoForm.append("Format: Audio");
        
        infoForm.addCommand(new Command("OK", Command.OK, 1));
        infoForm.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                midlet.getDisplay().setCurrent(AudioPlayerCanvas.this);
            }
        });
        
        midlet.getDisplay().setCurrent(infoForm);
    }
    
    public void commandAction(Command c, Displayable d) {
        if (c == backCmd) {
            cleanup();
            midlet.backToMenu();
        } else if (c == pauseCmd) {
            togglePlayPause();
        } else if (c == infoCmd) {
            showFileInfo();
        }
    }
}