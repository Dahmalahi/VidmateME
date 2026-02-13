import javax.microedition.lcdui.*;
import java.util.Vector;

public class ConvertUrlCanvas extends Form implements CommandListener, Runnable {
    private VidmateME midlet;
    private TextField urlField;
    private final Command convertCmd = new Command("Convertir", Command.SCREEN, 1);
    private final Command backCmd = new Command("Retour", Command.BACK, 2);
    private Thread workerThread;
    
    public ConvertUrlCanvas(VidmateME m) {
        super("Convertir Lien YouTube");
        midlet = m;
        urlField = new TextField("Lien YouTube:", "", 256, TextField.URL);
        append(urlField);
        append("\nFormats supportes:\n- https://youtu.be/ID\n- https://youtube.com/watch?v=ID\n- ID seul (11 caracteres)");
        addCommand(convertCmd);
        addCommand(backCmd);
        setCommandListener(this);
    }
    
    public void commandAction(Command c, Displayable d) {
        if (c == backCmd) {
            midlet.backToMenu();
        } else if (c == convertCmd) {
            String input = urlField.getString().trim();
            if (input.length() == 0) {
                showAlert("Erreur", "Veuillez entrer un lien YouTube");
                return;
            }
            
            Form progress = new Form("Conversion en cours...");
            progress.append(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING));
            midlet.getDisplay().setCurrent(progress);
            
            if (workerThread != null && workerThread.isAlive()) {
                workerThread.interrupt();
            }
            workerThread = new Thread(this);
            workerThread.start();
        }
    }
    
    public void run() {
        try {
            String input = urlField.getString().trim();
            String videoId = UrlConverter.extractVideoId(input);
            
            if (videoId == null) {
                showAlert("Erreur", "Lien YouTube invalide ou ID non trouve");
                return;
            }
            
            Vector results = Ytfinder.searchById(videoId);
            if (results.size() == 0) {
                VideoItem item = new VideoItem();
                item.videoId = videoId;
                item.title = "Video " + videoId;
                item.author = "YouTube";
                item.duration = "Inconnue";
                item.thumbnailUrl = "http://i.ytimg.com/vi/" + videoId + "/mqdefault.jpg";
                results.addElement(item);
            }
            
            VideoItem item = (VideoItem) results.elementAt(0);
            showVideoDetails(item);
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null) errorMsg = "Erreur inconnue";
            
            showAlert("Erreur", "Echec: " + errorMsg + "\n\nConseil: Essayez avec S60Tube direct");
        }
    }
    
    private void showVideoDetails(final VideoItem item) {
        midlet.getDisplay().callSerially(new Runnable() {
            public void run() {
                Form details = new Form("Details Video");
                details.append("Titre: " + item.title + "\n");
                details.append("ID: " + item.videoId + "\n\n");
                
                // CORRECTION: Bouton miniature avec retour vers ConvertUrlCanvas
                if (item.thumbnailUrl != null && item.thumbnailUrl.length() > 0) {
                    final Command showThumbCmd = new Command("Voir Miniature", Command.SCREEN, 1);
                    details.addCommand(showThumbCmd);
                }
                
                // Qualité étendue
                final ChoiceGroup quality = new ChoiceGroup("Qualite:", Choice.EXCLUSIVE);
                quality.append("144p (Economique)", null);
                quality.append("240p (Basse)", null);
                quality.append("360p (Standard)", null);
                quality.append("480p (SD)", null);
                quality.append("720p (HD)", null);
                quality.append("1080p (Full HD)", null);
                quality.setSelectedIndex(2, true);
                details.append(quality);
                
                // Format étendu
                final ChoiceGroup format = new ChoiceGroup("Format:", Choice.EXCLUSIVE);
                format.append("VIDEO - MP4", null);
                format.append("VIDEO - 3GP (mobile)", null);
                format.append("AUDIO - MP3", null);
                format.append("AUDIO - AAC", null);
                format.append("AUDIO - WAV", null);
                details.append(format);
                
                final Command dlCmd = new Command("Telecharger", Command.OK, 2);
                final Command cancelCmd = new Command("Annuler", Command.BACK, 3);
                details.addCommand(dlCmd);
                details.addCommand(cancelCmd);
                
                details.setCommandListener(new CommandListener() {
                    public void commandAction(Command c, Displayable d) {
                        if (c.getLabel().indexOf("Miniature") != -1) {
                            // CORRECTION: Passer ConvertUrlCanvas comme écran de retour
                            ThumbnailViewer viewer = new ThumbnailViewer(midlet, item.videoId, item.title, ConvertUrlCanvas.this);
                            midlet.getDisplay().setCurrent(viewer);
                        } else if (c == dlCmd) {
                            int qualityIdx = quality.getSelectedIndex();
                            int formatIdx = format.getSelectedIndex();
                            startDownload(item, qualityIdx, formatIdx);
                        } else if (c == cancelCmd || c.getCommandType() == Command.BACK) {
                            midlet.getDisplay().setCurrent(ConvertUrlCanvas.this);
                        }
                    }
                });
                
                midlet.getDisplay().setCurrent(details);
            }
        });
    }
    
    private void startDownload(final VideoItem item, final int qualityIdx, final int formatIdx) {
        final Form progress = new Form("PREPARATION...");
        progress.append(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING));
        progress.append("\nRecuperation du lien de telechargement...");
        midlet.getDisplay().setCurrent(progress);
        
        new Thread(new Runnable() {
            public void run() {
                try {
                    String q = "360";
                    switch (qualityIdx) {
                        case 0: q = "144"; break;
                        case 1: q = "240"; break;
                        case 2: q = "360"; break;
                        case 3: q = "480"; break;
                        case 4: q = "720"; break;
                        case 5: q = "1080"; break;
                    }
                    
                    String fileFormat = "mp4";
                    boolean audioOnly = false;
                    
                    switch (formatIdx) {
                        case 0:
                            fileFormat = "mp4";
                            audioOnly = false;
                            break;
                        case 1:
                            fileFormat = "3gp";
                            audioOnly = false;
                            break;
                        case 2:
                            fileFormat = "mp3";
                            audioOnly = true;
                            break;
                        case 3:
                            fileFormat = "aac";
                            audioOnly = true;
                            break;
                        case 4:
                            fileFormat = "wav";
                            audioOnly = true;
                            break;
                    }
                    
                    item.downloadUrl = APIManager.getDownloadUrl(item.videoId, q, audioOnly, fileFormat);
                    item.fileFormat = fileFormat;
                    DownloadManager.getInstance().queueDownload(item);
                    
                    showAlert("Succes", 
                        "Telechargement en file d'attente!\n\n" +
                        "Qualite: " + q + "p\n" +
                        "Format: " + fileFormat.toUpperCase());
                    
                    midlet.backToMenu();
                    
                } catch (Exception e) {
                    showAlert("Erreur 403", 
                        "Echec telechargement:\n" + e.getMessage() + 
                        "\n\nSolution:\n1. Allez dans Parametres\n2. Activez les proxys\n3. Reessayez");
                }
            }
        }).start();
    }
    
    private void showAlert(final String title, final String msg) {
        final Alert a = new Alert(title, msg, null, AlertType.INFO);
        a.setTimeout(4000);
        midlet.getDisplay().callSerially(new Runnable() {
            public void run() {
                midlet.getDisplay().setCurrent(a, ConvertUrlCanvas.this);
            }
        });
    }
}
