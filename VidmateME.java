import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;

public class VidmateME extends MIDlet implements CommandListener {
    public static VidmateME instance;
    private Display display;
    private List mainMenu;
    
    public SearchCanvas searchCanvas;
    public ConvertUrlCanvas convertUrlCanvas;
    public DownloadsCanvas downloadsCanvas;
    public LibraryCanvas libraryCanvas;
    public SettingsCanvas settingsCanvas;
    
    private Command exitCmd = new Command("Quitter", Command.EXIT, 1);
    private Command aboutCmd = new Command("A propos", Command.HELP, 2);
    
    public VidmateME() {
        instance = this;
        display = Display.getDisplay(this);
        
        try {
            SettingsManager.getInstance();
            // ✅ NEW: Initialize storage on first launch
            StorageManager.getDownloadPath();
        } catch (Exception e) {
        }
        
        mainMenu = new List("UniMedia v2.1", List.IMPLICIT);
        
        mainMenu.append("[1] Rechercher Videos", null);
        mainMenu.append("[2] Convertir Lien YouTube", null);
        mainMenu.append("[3] Telechargements", null);
        mainMenu.append("[4] Ma Bibliotheque", null);
        mainMenu.append("[5] Parametres", null);
        mainMenu.append("[6] Diagnostic", null); // ✅ CHANGED: More general name
        
        mainMenu.addCommand(exitCmd);
        mainMenu.addCommand(aboutCmd);
        mainMenu.setCommandListener(this);
        
        searchCanvas = new SearchCanvas(this);
        convertUrlCanvas = new ConvertUrlCanvas(this);
        downloadsCanvas = new DownloadsCanvas(this);
        libraryCanvas = new LibraryCanvas(this);
        settingsCanvas = new SettingsCanvas(this);
    }
    
    public void startApp() {
        SplashCanvas splash = new SplashCanvas(this);
        display.setCurrent(splash);
        splash.start();
    }
    
    public void pauseApp() {}
    
    public void destroyApp(boolean unconditional) {
        try {
            if (downloadsCanvas != null) {
                downloadsCanvas.stop();
            }
            DownloadManager.getInstance().shutdown();
        } catch (Exception e) {}
        
        try {
            SettingsManager.getInstance().close();
        } catch (Exception e) {}
    }
    
    public void commandAction(Command c, Displayable d) {
        if (c == exitCmd) {
            Alert confirm = new Alert("Quitter?", 
                "Voulez-vous vraiment quitter UniMedia?", 
                null, AlertType.CONFIRMATION);
            confirm.setTimeout(Alert.FOREVER);
            confirm.addCommand(new Command("Oui", Command.OK, 1));
            confirm.addCommand(new Command("Non", Command.CANCEL, 2));
            confirm.setCommandListener(new CommandListener() {
                public void commandAction(Command c, Displayable d) {
                    if (c.getCommandType() == Command.OK) {
                        destroyApp(true);
                        notifyDestroyed();
                    } else {
                        display.setCurrent(mainMenu);
                    }
                }
            });
            display.setCurrent(confirm);
        } else if (c == aboutCmd) {
            showAboutScreen();
        } 
        else if (d == mainMenu && (c == List.SELECT_COMMAND || c.getCommandType() == Command.SCREEN)) {
            int idx = mainMenu.getSelectedIndex();
            switch (idx) {
                case 0: 
                    display.setCurrent(searchCanvas); 
                    break;
                case 1: 
                    display.setCurrent(convertUrlCanvas); 
                    break;
                case 2: 
                    downloadsCanvas.refresh(); 
                    display.setCurrent(downloadsCanvas); 
                    break;
                case 3: 
                    libraryCanvas.refresh(); 
                    display.setCurrent(libraryCanvas); 
                    break;
                case 4: 
                    display.setCurrent(settingsCanvas); 
                    break;
                case 5:
                    showDiagnosticScreen();
                    break;
            }
        }
    }
    
    private void showAboutScreen() {
        Alert about = new Alert("A PROPOS DE UNIMEDIA",
            "UniMedia v2.1\n" +
            "Powered by Dashtube API\n\n" +
            "YouTube Downloader pour J2ME\n\n" +
            "FONCTIONNALITES:\n" +
            "* Recherche ultra-rapide (Dashtube)\n" +
            "* Conversion de liens\n" +
            "* Telechargement video/audio\n" +
            "* Lecteur integre\n" +
            "* Support multi-API (4 backends)\n" +
            "* Stockage auto-detecte\n" + // ✅ NEW
            "* Pagination intelligente\n" +
            "* Formats: MP4, 3GP, MP3, AAC, WAV\n" +
            "* Qualites: 144p-1080p\n\n" +
            "Stockage: VidmateME/\n" + // ✅ NEW
            "  - videos/\n" +
            "  - audios/\n" +
            "  - thumbnails/\n\n" +
            "Developpe pour les telephones\n" +
            "S60v3, S60v5 et compatibles\n\n" +
            "(c) 2024 UniMedia Project\n" +
            "Version 2.1 - Edition Dashtube",
            null, AlertType.INFO);
        about.setTimeout(Alert.FOREVER);
        about.addCommand(new Command("OK", Command.OK, 1));
        about.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                display.setCurrent(mainMenu);
            }
        });
        display.setCurrent(about);
    }
    
    // ✅ CHANGED: Enhanced diagnostic with storage info
    private void showDiagnosticScreen() {
        final Form diag = new Form("DIAGNOSTIC SYSTEME");
        
        diag.append("Test en cours...\n\n");
        Gauge gauge = new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING);
        diag.append(gauge);
        
        diag.addCommand(new Command("Retour", Command.BACK, 1));
        diag.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                backToMenu();
            }
        });
        
        display.setCurrent(diag);
        
        new Thread(new Runnable() {
            public void run() {
                final StringBuffer report = new StringBuffer();
                
                // ✅ NEW: Test storage first
                report.append("=== STOCKAGE ===\n\n");
                try {
                    report.append(StorageManager.getStorageInfo());
                } catch (Exception e) {
                    report.append("Erreur: ").append(e.getMessage());
                }
                report.append("\n\n");
                
                // Test APIs
                report.append("=== APIs ===\n\n");
                report.append(APIManager.testApis());
                
                display.callSerially(new Runnable() {
                    public void run() {
                        diag.deleteAll();
                        
                        StringItem results = new StringItem("", 
                            ">> RESULTATS:\n\n" + report.toString() + "\n\n" +
                            ">> CONSEIL:\n" +
                            "Si stockage invalide:\n" +
                            "Parametres > Changer chemin\n" +
                            "> Auto-detecter\n\n" +
                            "Si APIs echouent:\n" +
                            "Verifiez connexion Internet\n" +
                            "ou activez un proxy");
                        diag.append(results);
                    }
                });
            }
        }).start();
    }
    
    public Display getDisplay() {
        return display;
    }
    
    public void backToMenu() {
        display.callSerially(new Runnable() {
            public void run() {
                display.setCurrent(mainMenu);
            }
        });
    }
    
    public void showToast(String title, String message, AlertType type) {
        Alert toast = new Alert(title, message, null, type);
        toast.setTimeout(3000);
        display.setCurrent(toast, display.getCurrent());
    }
}

// ========== Splash Canvas - Updated for v2.1 ==========
class SplashCanvas extends Canvas implements Runnable {
    private VidmateME midlet;
    private Thread animThread;
    private int frame = 0;
    private boolean running = true;
    
    private static final int COLOR_BG = 0x0A0E27;
    private static final int COLOR_PRIMARY = 0x00D9FF;
    private static final int COLOR_ACCENT = 0xFF6B35;
    private static final int COLOR_TEXT = 0xFFFFFF;
    private static final int COLOR_MUTED = 0x8E8E93;
    
    public SplashCanvas(VidmateME m) {
        midlet = m;
    }
    
    public void start() {
        animThread = new Thread(this);
        animThread.start();
    }
    
    public void run() {
        try {
            for (int i = 0; i < 30 && running; i++) {
                frame = i;
                repaint();
                serviceRepaints();
                Thread.sleep(100);
            }
            midlet.backToMenu();
        } catch (Exception e) {
            midlet.backToMenu();
        }
    }
    
    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        
        g.setColor(COLOR_BG);
        g.fillRect(0, 0, w, h);
        
        g.setColor(COLOR_PRIMARY);
        g.fillRect(0, 0, w, 40);
        
        g.setColor(COLOR_ACCENT);
        g.fillRect(0, h - 40, w, 40);
        
        g.setColor(COLOR_TEXT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE));
        g.drawString("UniMedia", w/2, 60, Graphics.HCENTER | Graphics.TOP);
        
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        g.drawString("Version 2.1", w/2, 85, Graphics.HCENTER | Graphics.TOP);
        
        g.setColor(COLOR_PRIMARY);
        g.drawString("Powered by Dashtube", w/2, 100, Graphics.HCENTER | Graphics.TOP);
        
        // ✅ NEW: Show storage detection status
        if (frame > 25) {
            g.setColor(COLOR_MUTED);
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
            g.drawString("Initialisation stockage...", w/2, 115, Graphics.HCENTER | Graphics.TOP);
        }
        
        int rectSize = 80;
        int rectX = (w - rectSize) / 2;
        int rectY = 135;
        
        int offset = (frame * 3) % 360;
        g.setColor(COLOR_PRIMARY);
        g.drawRect(rectX + offset % 10 - 5, rectY, rectSize, rectSize);
        g.setColor(COLOR_ACCENT);
        g.drawRect(rectX - offset % 10 + 5, rectY + 5, rectSize, rectSize);
        
        g.setColor(COLOR_TEXT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE));
        g.drawString("YT", w/2, rectY + 35, Graphics.HCENTER | Graphics.TOP);
        
        g.setColor(COLOR_TEXT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        int textY = 235;
        
        if (frame > 5) {
            g.drawString(">> YouTube Downloader", w/2, textY, Graphics.HCENTER | Graphics.TOP);
            textY += 15;
        }
        if (frame > 10) {
            g.drawString(">> Stockage: VidmateME/", w/2, textY, Graphics.HCENTER | Graphics.TOP);
            textY += 15;
        }
        if (frame > 15) {
            g.drawString(">> Formats: MP4, 3GP, MP3", w/2, textY, Graphics.HCENTER | Graphics.TOP);
            textY += 15;
        }
        if (frame > 20) {
            g.drawString(">> Auto-detection active", w/2, textY, Graphics.HCENTER | Graphics.TOP);
        }
        
        int barW = w - 40;
        int barH = 10;
        int barX = 20;
        int barY = h - 60;
        
        g.setColor(0x333333);
        g.fillRect(barX, barY, barW, barH);
        
        int progress = (frame * barW) / 30;
        g.setColor(COLOR_PRIMARY);
        g.fillRect(barX, barY, progress, barH);
        
        g.setColor(COLOR_MUTED);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        g.drawString("(c) 2024 UniMedia Project", w/2, h - 20, Graphics.HCENTER | Graphics.TOP);
    }
    
    protected void keyPressed(int keyCode) {
        running = false;
        midlet.backToMenu();
    }
}