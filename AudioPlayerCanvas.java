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
    
    // CORRECTION : declaration du champ filePath pour acces dans run()
    private final String filePath;
    
    private final Command backCmd = new Command("Retour", Command.BACK, 1);
    private final Command pauseCmd = new Command("Pause", Command.OK, 2);
    
    // Constructeur avec filePath final + stockage dans le champ de classe
    public AudioPlayerCanvas(VidmateME m, final String filePathParam, final String name) {
        midlet = m;
        fileName = name;
        filePath = filePathParam; // Stockage dans le champ final
        setCommandListener(this);
        addCommand(backCmd);
        
        // Chargement asynchrone
        new Thread(this).start();
    }
    
    public void run() {
        try {
            // CORRECTION : utilisation du champ filePath (accessible maintenant)
            String locator = filePath.startsWith("file://") ? filePath : "file:///" + filePath;
            player = Manager.createPlayer(locator);
            player.realize();
            
            volumeControl = (VolumeControl) player.getControl("VolumeControl");
            if (volumeControl != null) volumeControl.setLevel(volumeLevel);
            
            player.start();
            isPlaying = true;
            status = "Lecture";
            repaint();
        } catch (Exception e) {
            status = "Erreur: " + e.getMessage();
            repaint();
            try { Thread.sleep(3000); } catch (Exception ex) {}
            midlet.backToMenu();
        }
    }
    
    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        
        // Fond
        g.setColor(0x111111);
        g.fillRect(0, 0, w, h);
        
        // Titre
        g.setColor(0x00CCAA);
        g.fillRect(0, 0, w, 50);
        g.setColor(0xFFFFFF);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE));
        g.drawString("Lecteur Audio", w/2, 15, Graphics.HCENTER | Graphics.TOP);
        
        // Nom du fichier
        g.setColor(0xFFFFFF);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM));
        String displayName = fileName;
        if (displayName.length() > 25) {
            displayName = displayName.substring(0, 22) + "...";
        }
        g.drawString(displayName, w/2, 60, Graphics.HCENTER | Graphics.TOP);
        
        // Barre de progression
        int barW = w - 60;
        int barH = 30;
        int barX = 30;
        int barY = 100;
        
        g.setColor(0x333333);
        g.fillRoundRect(barX, barY, barW, barH, 10, 10);
        
        if (isPlaying && player != null) {
            try {
                duration = player.getDuration() / 1000;
                currentTime = player.getMediaTime() / 1000000;
                
                if (duration > 0) {
                    int progress = (int)((currentTime * 100) / duration);
                    int fillW = (barW * progress) / 100;
                    
                    g.setColor(0x00AA00);
                    g.fillRoundRect(barX, barY, fillW, barH, 10, 10);
                    
                    // Temps
                    g.setColor(0xFFFFFF);
                    g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
                    String timeStr = formatTime(currentTime) + " / " + formatTime(duration);
                    g.drawString(timeStr, w/2, barY + barH + 10, Graphics.HCENTER | Graphics.TOP);
                }
            } catch (Exception e) {}
        }
        
        // Statut
        g.setColor(0xAAAAAA);
        g.drawString("Statut: " + status, w/2, 150, Graphics.HCENTER | Graphics.TOP);
        
        // Controles
        g.setColor(0x00CCAA);
        g.drawString("5:Play/Pause  4/6:Vol  0:Quitter", w/2, 180, Graphics.HCENTER | Graphics.TOP);
        
        // Volume
        int volW = 100;
        int volH = 10;
        int volX = (w - volW) / 2;
        int volY = 210;
        
        g.setColor(0x444444);
        g.fillRect(volX, volY, volW, volH);
        g.setColor(0x00AA00);
        g.fillRect(volX, volY, (volW * volumeLevel) / 100, volH);
        g.setColor(0x00CCAA);
        g.drawString("Volume: " + volumeLevel + "%", w/2, volY + volH + 5, Graphics.HCENTER | Graphics.TOP);
    }
    
    protected void keyPressed(int keyCode) {
        if (keyCode == Canvas.KEY_NUM5) {
            togglePlayPause();
        } else if (keyCode == Canvas.KEY_NUM0) {
            stopPlayer();
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
                status = "Pause";
            } else {
                player.start();
                isPlaying = true;
                isPaused = false;
                status = "Lecture";
            }
            repaint();
        } catch (Exception e) {
            status = "Erreur";
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
            volumeControl = null;
        }
    }
    
    private String formatTime(long seconds) {
        long mins = seconds / 60;
        long secs = seconds % 60;
        return (mins < 10 ? "0" : "") + mins + ":" + (secs < 10 ? "0" : "") + secs;
    }
    
    public void commandAction(Command c, Displayable d) {
        if (c == backCmd) {
            stopPlayer();
            midlet.backToMenu();
        } else if (c == pauseCmd) {
            togglePlayPause();
        }
    }
}