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
    private long startTime = 0;
    private long playedTime = 0;
    
    // Couleurs améliorées
    private static final int COLOR_BG = 0x000000;
    private static final int COLOR_CONTROL_BG = 0x1C1C1E;
    private static final int COLOR_PRIMARY = 0xFF3B30;
    private static final int COLOR_ACCENT = 0x00D9FF;
    private static final int COLOR_TEXT = 0xFFFFFF;
    private static final int COLOR_MUTED = 0x8E8E93;
    private static final int COLOR_SUCCESS = 0x30D158;
    
    private final Command backCmd = new Command("Retour", Command.BACK, 1);
    private final Command helpCmd = new Command("Aide", Command.HELP, 2);
    
    public PlayerCanvas(VidmateME m, final String filePath, final String name) {
        midlet = m;
        fileName = name;
        setCommandListener(this);
        addCommand(backCmd);
        addCommand(helpCmd);
        
        startTime = System.currentTimeMillis();
        
        // Chargement asynchrone du lecteur
        new Thread(new Runnable() {
            public void run() {
                try {
                    String locator = filePath.startsWith("file://") ? filePath : "file:///" + filePath;
                    player = Manager.createPlayer(locator);
                    player.realize();
                    
                    videoControl = (VideoControl) player.getControl("VideoControl");
                    volumeControl = (VolumeControl) player.getControl("VolumeControl");
                    if (volumeControl != null) volumeControl.setLevel(volumeLevel);
                    
                    if (videoControl != null) {
                        try {
                            videoControl.initDisplayMode(VideoControl.USE_DIRECT_VIDEO, PlayerCanvas.this);
                            int videoHeight = getHeight() - 80;
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
                    status = "LECTURE";
                    repaint();
                    
                } catch (Exception e) {
                    status = "ERREUR: " + e.getMessage();
                    repaint();
                    try { Thread.sleep(3000); } catch (Exception ex) {}
                    midlet.backToMenu();
                }
            }
        }).start();
    }
    
    private void useFallbackPlayer() {
        try {
            Item videoItem = (Item) videoControl.initDisplayMode(VideoControl.USE_GUI_PRIMITIVE, null);
            Form fallback = new Form("Lecteur Video");
            fallback.append(videoItem);
            
            StringItem info = new StringItem("", 
                "\n" + fileName + "\n\n" +
                "Controles:\n" +
                "Le lecteur se trouve ci-dessus\n" +
                "Utilisez les controles natifs\n");
            fallback.append(info);
            
            fallback.addCommand(new Command("Retour", Command.BACK, 1));
            fallback.setCommandListener(new CommandListener() {
                public void commandAction(Command c, Displayable d) {
                    stopPlayer();
                    midlet.backToMenu();
                }
            });
            midlet.getDisplay().setCurrent(fallback);
        } catch (Exception e) {
            status = "ERREUR lecteur";
            repaint();
        }
    }
    
    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        
        // Fond noir pour la vidéo
        g.setColor(COLOR_BG);
        g.fillRect(0, 0, w, h);
        
        // Barre de contrôle moderne (si visible)
        if (showControls) {
            paintControls(g, w, h);
        } else {
            paintMiniStatus(g, w, h);
        }
    }
    
    private void paintControls(Graphics g, int w, int h) {
        int barHeight = 80;
        int barY = h - barHeight;
        
        // Fond pour les contrôles
        g.setColor(COLOR_CONTROL_BG);
        g.fillRect(0, barY, w, barHeight);
        
        // Ligne séparatrice colorée
        g.setColor(COLOR_PRIMARY);
        g.fillRect(0, barY, w, 2);
        
        // Nom du fichier
        g.setColor(COLOR_TEXT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
        String displayName = fileName;
        if (displayName.length() > 22) {
            displayName = displayName.substring(0, 19) + "...";
        }
        g.drawString("[VIDEO] " + displayName, 10, barY + 8, Graphics.LEFT | Graphics.TOP);
        
        // Statut de lecture
        g.setColor(isPlaying ? COLOR_SUCCESS : COLOR_ACCENT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        g.drawString(status, 10, barY + 28, Graphics.LEFT | Graphics.TOP);
        
        // Temps écoulé
        if (isPlaying) {
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            String timeStr = formatTime(elapsed);
            g.setColor(COLOR_MUTED);
            g.drawString("Temps: " + timeStr, 10, barY + 44, Graphics.LEFT | Graphics.TOP);
        }
        
        // Barre de volume
        paintVolumeBar(g, w, barY);
        
        // Instructions
        g.setColor(COLOR_ACCENT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        g.drawString("5:Play/Pause  4/6:Vol  7:Cacher  0:Quitter", 
            w/2, barY + 64, Graphics.HCENTER | Graphics.TOP);
    }
    
    private void paintVolumeBar(Graphics g, int w, int barY) {
        int volW = 100;
        int volH = 12;
        int volX = w - volW - 15;
        int volY = barY + 12;
        
        // Fond de la barre
        g.setColor(0x2C2C2E);
        g.fillRoundRect(volX, volY, volW, volH, 6, 6);
        
        // Remplissage selon le niveau
        int fillW = (volW * volumeLevel) / 100;
        if (fillW > 0) {
            if (volumeLevel < 30) {
                g.setColor(0xFF9500);  // Orange
            } else if (volumeLevel < 70) {
                g.setColor(COLOR_ACCENT);  // Bleu
            } else {
                g.setColor(COLOR_SUCCESS);  // Vert
            }
            g.fillRoundRect(volX, volY, fillW, volH, 6, 6);
        }
        
        // Bordure
        g.setColor(COLOR_MUTED);
        g.drawRoundRect(volX, volY, volW, volH, 6, 6);
        
        // Pourcentage
        g.setColor(COLOR_TEXT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        String volIcon = volumeLevel == 0 ? "[MUTE]" : volumeLevel < 50 ? "[VOL-]" : "[VOL+]";
        g.drawString(volIcon + " " + volumeLevel + "%", volX + volW + 5, volY, Graphics.LEFT | Graphics.TOP);
    }
    
    private void paintMiniStatus(Graphics g, int w, int h) {
        int indicatorW = 60;
        int indicatorH = 20;
        int x = w - indicatorW - 5;
        int y = 5;
        
        g.setColor(COLOR_CONTROL_BG);
        g.fillRoundRect(x, y, indicatorW, indicatorH, 10, 10);
        
        g.setColor(isPlaying ? COLOR_SUCCESS : COLOR_ACCENT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
        String icon = isPlaying ? "PLAY" : "PAUSE";
        g.drawString(icon, x + indicatorW/2, y + 5, Graphics.HCENTER | Graphics.TOP);
    }
    
    protected void keyPressed(int keyCode) {
        int gameKey = getGameAction(keyCode);
        
        if (keyCode == Canvas.KEY_NUM5 || gameKey == Canvas.FIRE) {
            togglePlayPause();
        } else if (keyCode == Canvas.KEY_NUM0) {
            stopPlayer();
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
                isPlaying = false;
                isPaused = true;
                status = "PAUSE";
                playedTime += System.currentTimeMillis() - startTime;
            } else {
                player.start();
                isPlaying = true;
                isPaused = false;
                status = "LECTURE";
                startTime = System.currentTimeMillis();
            }
            repaint();
        } catch (Exception e) {
            status = "ERREUR controle";
            repaint();
        }
    }
    
    private void stopPlayer() {
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
    
    public void commandAction(Command c, Displayable d) {
        if (c == backCmd) {
            stopPlayer();
            midlet.backToMenu();
        } else if (c == helpCmd) {
            showHelp();
        }
    }
    
    private void showHelp() {
        Alert help = new Alert("Aide - Controles",
            "Touches du lecteur video:\n\n" +
            ">> Touche 5 / OK:\n" +
            "   Play / Pause\n\n" +
            ">> Touches 4 et 6:\n" +
            "   Volume - / +\n\n" +
            ">> Touche 7:\n" +
            "   Afficher/Cacher controles\n\n" +
            ">> Touche 0:\n" +
            "   Quitter le lecteur\n\n" +
            ">> Commande Retour:\n" +
            "   Retour au menu",
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