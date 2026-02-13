import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import javax.microedition.io.file.*;
import java.io.*;

public class ThumbnailViewer extends Canvas implements CommandListener, Runnable {
    private VidmateME midlet;
    private Image thumbnail = null;
    private String videoTitle = "";
    private String status = "Chargement de la miniature...";
    private Thread loaderThread;
    private Displayable returnScreen; // Ecran de retour
    
    private final String videoId;
    
    private final Command backCmd = new Command("Retour", Command.BACK, 1);
    private final Command saveCmd = new Command("Sauvegarder", Command.SCREEN, 2);
    
    // Constructeur avec écran de retour
    public ThumbnailViewer(VidmateME m, String videoIdParam, String title, Displayable returnTo) {
        midlet = m;
        videoId = videoIdParam;
        videoTitle = title;
        returnScreen = returnTo;
        setCommandListener(this);
        addCommand(backCmd);
        addCommand(saveCmd);
        
        loaderThread = new Thread(this);
        loaderThread.start();
    }
    
    // Constructeur avec boolean (ancienne compatibilité)
    public ThumbnailViewer(VidmateME m, String videoIdParam, String title, boolean fromSearch) {
        this(m, videoIdParam, title, fromSearch ? (Displayable)m.searchCanvas : (Displayable)m.convertUrlCanvas);
    }
    
    public void run() {
        try {
            String thumbUrl = "http://i.ytimg.com/vi/" + videoId + "/mqdefault.jpg";
            
            HttpConnection conn = (HttpConnection) Connector.open(thumbUrl);
            conn.setRequestMethod(HttpConnection.GET);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (S60V3; U; en) AppleWebKit/413");
            
            InputStream is = conn.openInputStream();
            thumbnail = Image.createImage(is);
            is.close();
            conn.close();
            
            status = "Miniature chargee - OK";
        } catch (Exception e) {
            status = "Erreur: " + e.getMessage();
        }
        repaint();
    }
    
    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        
        g.setColor(0x000000);
        g.fillRect(0, 0, w, h);
        
        // Titre
        g.setColor(0xFFFFFF);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
        String shortTitle = videoTitle;
        if (shortTitle.length() > 25) {
            shortTitle = shortTitle.substring(0, 22) + "...";
        }
        g.drawString(shortTitle, w/2, 10, Graphics.HCENTER | Graphics.TOP);
        
        // Miniature ou message
        if (thumbnail != null) {
            int imgW = thumbnail.getWidth();
            int imgH = thumbnail.getHeight();
            int x = (w - imgW) / 2;
            int y = 50;
            g.drawImage(thumbnail, x, y, Graphics.TOP | Graphics.LEFT);
            
            g.setColor(0xAAAAAA);
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
            g.drawString(imgW + "x" + imgH + " pixels", w/2, y + imgH + 10, Graphics.HCENTER | Graphics.TOP);
            
            // Instructions
            g.setColor(0x00D9FF);
            g.drawString("Appuyez sur 'Sauvegarder'", w/2, h - 30, Graphics.HCENTER | Graphics.TOP);
            g.drawString("pour enregistrer l'image", w/2, h - 15, Graphics.HCENTER | Graphics.TOP);
        } else {
            g.setColor(0xFF6600);
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM));
            g.drawString(status, w/2, h/2, Graphics.HCENTER | Graphics.TOP);
        }
    }
    
    protected void keyPressed(int keyCode) {
        if (keyCode == Canvas.KEY_NUM5 || keyCode == Canvas.KEY_NUM0) {
            goBack();
        } else if (keyCode == Canvas.KEY_NUM1 && thumbnail != null) {
            saveThumbnail();
        }
    }
    
    public void commandAction(Command c, Displayable d) {
        if (c == backCmd) {
            goBack();
        } else if (c == saveCmd && thumbnail != null) {
            saveThumbnail();
        }
    }
    
    private void goBack() {
        if (returnScreen != null) {
            midlet.getDisplay().setCurrent(returnScreen);
        } else {
            midlet.backToMenu();
        }
    }
    
    private void saveThumbnail() {
        if (thumbnail == null) {
            showAlert("Erreur", "Aucune miniature a sauvegarder", AlertType.ERROR);
            return;
        }
        
        new Thread(new Runnable() {
            public void run() {
                try {
                    // Créer le dossier thumbnails
                    String thumbPath = StorageManager.getDownloadPath() + "thumbnails/";
                    FileConnection dir = (FileConnection) Connector.open(thumbPath);
                    if (!dir.exists()) {
                        dir.mkdir();
                    }
                    dir.close();
                    
                    // Nom du fichier
                    String filename = sanitizeFilename(videoTitle) + "_" + videoId + ".png";
                    String fullPath = thumbPath + filename;
                    
                    // Sauvegarder l'image
                    FileConnection fc = (FileConnection) Connector.open(fullPath);
                    if (fc.exists()) {
                        fc.delete();
                    }
                    fc.create();
                    
                    OutputStream os = fc.openOutputStream();
                    
                    // Encoder l'image en PNG (simple)
                    byte[] imageData = encodePNG(thumbnail);
                    os.write(imageData);
                    os.flush();
                    os.close();
                    fc.close();
                    
                    showAlert("Succes!", 
                        "Miniature sauvegardee dans:\n" + thumbPath + "\n\n" +
                        "Nom: " + filename, 
                        AlertType.CONFIRMATION);
                    
                } catch (Exception e) {
                    showAlert("Erreur", 
                        "Impossible de sauvegarder:\n" + e.getMessage(), 
                        AlertType.ERROR);
                }
            }
        }).start();
    }
    
    private byte[] encodePNG(Image img) throws IOException {
        // Pour J2ME, on utilise une méthode simple
        // On va télécharger l'image directement depuis YouTube
        try {
            String thumbUrl = "http://i.ytimg.com/vi/" + videoId + "/mqdefault.jpg";
            HttpConnection conn = (HttpConnection) Connector.open(thumbUrl);
            conn.setRequestMethod(HttpConnection.GET);
            
            InputStream is = conn.openInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            is.close();
            conn.close();
            
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IOException("Erreur encodage: " + e.getMessage());
        }
    }
    
    private String sanitizeFilename(String s) {
        StringBuffer clean = new StringBuffer();
        for (int i = 0; i < s.length() && i < 20; i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || 
                (c >= '0' && c <= '9') || c == '_' || c == '-') {
                clean.append(c);
            } else if (c == ' ') {
                clean.append('_');
            }
        }
        if (clean.length() == 0) return "thumbnail";
        return clean.toString();
    }
    
    private void showAlert(final String title, final String msg, final AlertType type) {
        midlet.getDisplay().callSerially(new Runnable() {
            public void run() {
                Alert a = new Alert(title, msg, null, type);
                a.setTimeout(3000);
                midlet.getDisplay().setCurrent(a, ThumbnailViewer.this);
            }
        });
    }
}