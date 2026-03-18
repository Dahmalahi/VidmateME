import javax.microedition.lcdui.*;
import javax.microedition.media.*;
import javax.microedition.media.control.*;

public class PlayerCanvas extends Canvas implements CommandListener {
    private VidmateME midlet;
    private Player player;
    private VideoControl videoControl;
    private VolumeControl volumeControl;
    private String status = "Chargement...";
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private boolean showControls = true;
    private int volumeLevel = 75;
    private String fileName = "";
    private String filePath = "";
    private long fileSize = 0;
    private long startTime = 0;
    private long playedTime = 0;
    
    private static final int COLOR_BG = 0x0A0E27;
    private static final int COLOR_CONTROL_BG = 0x1C1C1E;
    private static final int COLOR_PRIMARY = 0x00D9FF;
    private static final int COLOR_ACCENT = 0xFF6B35;
    private static final int COLOR_TEXT = 0xFFFFFF;
    private static final int COLOR_MUTED = 0x8E8E93;
    private static final int COLOR_SUCCESS = 0x30D158;
    
    private final Command backCmd = new Command("Retour", Command.BACK, 1);
    private final Command helpCmd = new Command("Aide", Command.HELP, 2);
    private final Command infoCmd = new Command("Infos", Command.SCREEN, 3);
    
    public PlayerCanvas(VidmateME m, final String filePathParam, final String name) {
        midlet = m;
        fileName = name;
        filePath = filePathParam;
        
        setCommandListener(this);
        addCommand(backCmd);
        addCommand(helpCmd);
        addCommand(infoCmd);
        
        startTime = System.currentTimeMillis();
        
        try {
            fileSize = StorageManager.getFileSize(filePath);
        } catch (Exception e) {
            fileSize = 0;
        }
        
        new Thread(new Runnable() {
            public void run() {
                loadPlayer(filePathParam);
            }
        }).start();
    }
    
    private void loadPlayer(String path) {
        try {
            status = "Initialisation...";
            repaint();
            
            String locator = path;
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
            
            videoControl = (VideoControl) player.getControl("VideoControl");
            volumeControl = (VolumeControl) player.getControl("VolumeControl");
            
            if (volumeControl != null) {
                volumeControl.setLevel(volumeLevel);
            }
            
            if (videoControl != null) {
                try {
                    videoControl.initDisplayMode(VideoControl.USE_DIRECT_VIDEO, PlayerCanvas.this);
                    
                    int videoHeight = getHeight() - 90;
                    videoControl.setDisplayLocation(2, 2);
                    videoControl.setDisplaySize(getWidth() - 4, videoHeight);
                    videoControl.setVisible(true);
                    
                } catch (Exception e) {
                    useFallbackPlayer();
                    return;
                }
            }
            
            player.start();
            isPlaying = true;
            status = "Lecture en cours";
            repaint();
            
        } catch (Exception e) {
            status = "Erreur: " + e.getMessage();
            repaint();
            
            Alert error = new Alert("Erreur Lecture Video",
                "Impossible de lire le fichier:\n" + 
                e.getMessage() + "\n\n" +
                "Verifiez que le fichier existe\n" +
                "dans: " + StorageManager.getDownloadPath() + "videos/\n\n" +
                "Formats supportes:\n" +
                "MP4, 3GP, AVI",
                null, AlertType.ERROR);
            error.setTimeout(5000);
            midlet.getDisplay().setCurrent(error, PlayerCanvas.this);
            
            try { Thread.sleep(3000); } catch (Exception ex) {}
            cleanup();
            midlet.backToMenu();
        }
    }
    
    private void useFallbackPlayer() {
        try {
            Item videoItem = (Item) videoControl.initDisplayMode(VideoControl.USE_GUI_PRIMITIVE, null);
            Form fallback = new Form("Lecteur Video - UniMedia v2.1");
            fallback.append(videoItem);
            
            StringItem info = new StringItem("", 
                "\n[VIDEO] " + fileName + "\n\n" +
                "Le lecteur natif est actif.\n" +
                "Utilisez les controles ci-dessus.\n\n" +
                "Stockage:\n" +
                StorageManager.getDownloadPath() + "videos/\n");
            fallback.append(info);
            
            if (fileSize > 0) {
                info = new StringItem("Taille: ", formatSize(fileSize) + "\n");
                fallback.append(info);
            }
            
            fallback.addCommand(new Command("Retour", Command.BACK, 1));
            fallback.setCommandListener(new CommandListener() {
                public void commandAction(Command c, Displayable d) {
                    cleanup();
                    midlet.backToMenu();
                }
            });
            midlet.getDisplay().setCurrent(fallback);
            
        } catch (Exception e) {
            status = "Erreur lecteur natif";
            repaint();
        }
    }
    
    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        
        g.setColor(COLOR_BG);
        g.fillRect(0, 0, w, h);
        
        if (showControls) {
            paintControls(g, w, h);
        } else {
            paintMiniStatus(g, w, h);
        }
    }
    
    private void paintControls(Graphics g, int w, int h) {
        int barHeight = 90;
        int barY = h - barHeight;
        
        g.setColor(COLOR_CONTROL_BG);
        g.fillRect(0, barY, w, barHeight);
        
        g.setColor(COLOR_PRIMARY);
        g.fillRect(0, barY, w, 3);
        
        g.setColor(COLOR_TEXT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
        String displayName = fileName;
        if (displayName.length() > 25) {
            displayName = displayName.substring(0, 22) + "...";
        }
        g.drawString("[VIDEO] " + displayName, 10, barY + 8, Graphics.LEFT | Graphics.TOP);
        
        if (fileSize > 0) {
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
            g.setColor(COLOR_MUTED);
            g.drawString(formatSize(fileSize), 10, barY + 25, Graphics.LEFT | Graphics.TOP);
        }
        
        g.setColor(isPlaying ? COLOR_SUCCESS : COLOR_ACCENT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        String statusIcon = isPlaying ? "[PLAYING]" : isPaused ? "[PAUSED]" : "[STOPPED]";
        g.drawString(statusIcon + " " + status, 10, barY + 40, Graphics.LEFT | Graphics.TOP);
        
        if (isPlaying || isPaused) {
            long elapsed = (System.currentTimeMillis() - startTime + playedTime) / 1000;
            String timeStr = formatTime(elapsed);
            g.setColor(COLOR_TEXT);
            g.drawString("Temps: " + timeStr, 10, barY + 56, Graphics.LEFT | Graphics.TOP);
        }
        
        paintVolumeBar(g, w, barY);
        
        g.setColor(COLOR_PRIMARY);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        g.drawString("5:Play/Pause  4/6:Vol  7:Cacher  0:Menu", 
            w/2, barY + 72, Graphics.HCENTER | Graphics.TOP);
    }
    
    private void paintVolumeBar(Graphics g, int w, int barY) {
        int volW = 110;
        int volH = 14;
        int volX = w - volW - 15;
        int volY = barY + 10;
        
        g.setColor(0x2C2C2E);
        g.fillRoundRect(volX, volY, volW, volH, 7, 7);
        
        int fillW = (volW * volumeLevel) / 100;
        if (fillW > 0) {
            if (volumeLevel < 30) {
                g.setColor(COLOR_ACCENT);
            } else if (volumeLevel < 70) {
                g.setColor(COLOR_PRIMARY);
            } else {
                g.setColor(COLOR_SUCCESS);
            }
            g.fillRoundRect(volX, volY, fillW, volH, 7, 7);
        }
        
        g.setColor(COLOR_MUTED);
        g.drawRoundRect(volX, volY, volW, volH, 7, 7);
        
        g.setColor(COLOR_TEXT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        // ✅ FIXED: ASCII characters only
        String volIcon = volumeLevel == 0 ? "[MUTE]" : volumeLevel < 50 ? "[VOL-]" : "[VOL+]";
        g.drawString(volIcon + " " + volumeLevel + "%", volX + volW + 5, volY + 2, Graphics.LEFT | Graphics.TOP);
    }
    
    private void paintMiniStatus(Graphics g, int w, int h) {
        int indicatorW = 70;
        int indicatorH = 22;
        int x = w - indicatorW - 5;
        int y = 5;
        
        g.setColor(COLOR_CONTROL_BG);
        g.fillRoundRect(x, y, indicatorW, indicatorH, 11, 11);
        
        g.setColor(isPlaying ? COLOR_SUCCESS : COLOR_ACCENT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
        // ✅ FIXED: ASCII characters only
        String icon = isPlaying ? "> PLAY" : "|| PAUSE";
        g.drawString(icon, x + indicatorW/2, y + 6, Graphics.HCENTER | Graphics.TOP);
    }
    
    protected void keyPressed(int keyCode) {
        int gameKey = getGameAction(keyCode);
        
        if (keyCode == Canvas.KEY_NUM5 || gameKey == Canvas.FIRE) {
            togglePlayPause();
        } else if (keyCode == Canvas.KEY_NUM0) {
            cleanup();
            midlet.backToMenu();
        } else if (keyCode == Canvas.KEY_NUM7) {
            showControls = !showControls;
            repaint();
        } else if (keyCode == Canvas.KEY_NUM4 || gameKey == Canvas.LEFT) {
            if (volumeControl != null) {
                volumeLevel = Math.max(0, volumeLevel - 10);
                volumeControl.setLevel(volumeLevel);
                repaint();
            }
        } else if (keyCode == Canvas.KEY_NUM6 || gameKey == Canvas.RIGHT) {
            if (volumeControl != null) {
                volumeLevel = Math.min(100, volumeLevel + 10);
                volumeControl.setLevel(volumeLevel);
                repaint();
            }
        }
    }
    
    private void togglePlayPause() {
        if (player == null) return;
        try {
            if (isPlaying) {
                player.stop();
                playedTime += System.currentTimeMillis() - startTime;
                isPlaying = false;
                isPaused = true;
                status = "En pause";
            } else {
                player.start();
                startTime = System.currentTimeMillis();
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
    
    private void cleanup() {
        if (player != null) {
            try {
                player.stop();
                player.deallocate();
                player.close();
            } catch (Exception e) {}
            player = null;
            videoControl = null;
            volumeControl = null;
        }
    }
    
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }
    
    private String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        if (hours > 0) {
            return hours + ":" + 
                   (minutes < 10 ? "0" : "") + minutes + ":" + 
                   (secs < 10 ? "0" : "") + secs;
        } else {
            return minutes + ":" + (secs < 10 ? "0" : "") + secs;
        }
    }
    
    private void showFileInfo() {
        Form infoForm = new Form("Informations Video");
        
        infoForm.append("Nom:\n" + fileName + "\n\n");
        
        if (fileSize > 0) {
            infoForm.append("Taille: " + formatSize(fileSize) + "\n\n");
        }
        
        infoForm.append("Chemin:\n" + filePath + "\n\n");
        
        if (player != null) {
            try {
                long dur = player.getDuration() / 1000000;
                infoForm.append("Duree: " + formatTime(dur) + "\n\n");
            } catch (Exception e) {
                infoForm.append("Duree: Inconnue\n\n");
            }
        }
        
        infoForm.append("Stockage:\n" + 
            StorageManager.getDownloadPath() + "videos/\n\n");
        
        infoForm.append("Format: Video\n");
        infoForm.append("Lecteur: UniMedia v2.1");
        
        infoForm.addCommand(new Command("OK", Command.OK, 1));
        infoForm.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                midlet.getDisplay().setCurrent(PlayerCanvas.this);
            }
        });
        
        midlet.getDisplay().setCurrent(infoForm);
    }
    
    public void commandAction(Command c, Displayable d) {
        if (c == backCmd) {
            cleanup();
            midlet.backToMenu();
        } else if (c == helpCmd) {
            showHelp();
        } else if (c == infoCmd) {
            showFileInfo();
        }
    }
    
    private void showHelp() {
        Alert help = new Alert("Aide - Lecteur Video",
            "UniMedia v2.1 Video Player\n\n" +
            "CONTROLES:\n\n" +
            ">> Touche 5 / Joystick OK:\n" +
            "   Play / Pause la video\n\n" +
            // ✅ FIXED: ASCII characters only
            ">> Touches 4 et 6 / < >:\n" +
            "   Volume - / Volume +\n\n" +
            ">> Touche 7:\n" +
            "   Afficher/Cacher controles\n" +
            "   (mode plein ecran)\n\n" +
            ">> Touche 0:\n" +
            "   Retour au menu principal\n\n" +
            ">> Menu Infos:\n" +
            "   Details du fichier\n\n" +
            "STOCKAGE:\n" +
            StorageManager.getDownloadPath() + "videos/",
            null, AlertType.INFO);
        help.setTimeout(Alert.FOREVER);
        help.addCommand(new Command("OK", Command.OK, 1));
        help.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                midlet.getDisplay().setCurrent(PlayerCanvas.this);
            }
        });
        midlet.getDisplay().setCurrent(help);
    }
}