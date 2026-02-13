import javax.microedition.lcdui.*;
import java.util.Vector;

public class SearchCanvas extends Form implements CommandListener, Runnable {
    private VidmateME midlet;
    private TextField searchField;
    private ChoiceGroup quickSearches;
    private final Command searchCmd = new Command("Rechercher", Command.SCREEN, 1);
    private final Command backCmd = new Command("Retour", Command.BACK, 2);
    private Thread searchThread;
    
    private Vector allResults = new Vector();
    private int currentPage = 1;
    private int resultsPerPage = 10;
    
    public SearchCanvas(VidmateME m) {
        super("RECHERCHER VIDEOS");
        midlet = m;
        
        searchField = new TextField("Recherche YouTube:", "", 100, TextField.ANY);
        append(searchField);
        
        append(new Spacer(getWidth(), 5));
        
        StringItem suggestions = new StringItem("", ">> SUGGESTIONS:\n");
        suggestions.setLayout(Item.LAYOUT_NEWLINE_AFTER);
        append(suggestions);
        
        quickSearches = new ChoiceGroup("", Choice.EXCLUSIVE);
        quickSearches.append("Musique populaire", null);
        quickSearches.append("Films recents", null);
        quickSearches.append("Tutoriels", null);
        quickSearches.append("Gaming", null);
        quickSearches.append("Series TV", null);
        append(quickSearches);
        
        append(new Spacer(getWidth(), 5));
        
        StringItem info = new StringItem("", 
            ">> CONSEILS:\n" +
            "* Soyez precis dans vos recherches\n" +
            "* Utilisez des mots-cles courts\n" +
            "* Essayez plusieurs APIs si echec\n" +
            "* Pagination disponible (10 par page)\n");
        info.setLayout(Item.LAYOUT_NEWLINE_AFTER);
        append(info);
        
        addCommand(searchCmd);
        addCommand(backCmd);
        setCommandListener(this);
    }
    
    public void commandAction(Command c, Displayable d) {
        if (c == backCmd) {
            midlet.backToMenu();
        } else if (c == searchCmd) {
            String query = searchField.getString().trim();
            
            if (query.length() == 0) {
                int selected = quickSearches.getSelectedIndex();
                switch (selected) {
                    case 0: query = "musique 2024"; break;
                    case 1: query = "films"; break;
                    case 2: query = "tutoriel"; break;
                    case 3: query = "gaming"; break;
                    case 4: query = "series"; break;
                    default: query = ""; break;
                }
            }
            
            if (query.length() == 0) {
                showAlert("ERREUR", "Entrez un terme de recherche", AlertType.ERROR);
                return;
            }
            
            showProgressScreen("Recherche en cours...", "Recherche: " + query);
            
            if (searchThread != null && searchThread.isAlive()) {
                searchThread.interrupt();
            }
            searchThread = new Thread(this);
            searchThread.start();
        }
    }
    
    public void run() {
        try {
            String query = searchField.getString().trim();
            
            if (query.length() == 0) {
                int selected = quickSearches.getSelectedIndex();
                switch (selected) {
                    case 0: query = "musique 2024"; break;
                    case 1: query = "films"; break;
                    case 2: query = "tutoriel"; break;
                    case 3: query = "gaming"; break;
                    case 4: query = "series"; break;
                }
            }
            
            allResults = Ytfinder.search(query);
            
            if (allResults.size() == 0) {
                showAlert("AUCUN RESULTAT", 
                    "Aucune video trouvee pour '" + query + "'.\n\n" +
                    "Suggestions:\n" +
                    "* Verifiez l'orthographe\n" +
                    "* Essayez des mots-cles differents\n" +
                    "* Utilisez des termes plus generaux", 
                    AlertType.INFO);
                return;
            }
            
            currentPage = 1;
            showResultsPage();
            
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null) errorMsg = "Erreur inconnue";
            
            showAlert("ERREUR DE RECHERCHE", 
                "Impossible de rechercher:\n" + errorMsg + "\n\n" +
                "Solutions:\n" +
                "* Verifiez votre connexion\n" +
                "* Reessayez dans quelques instants\n" +
                "* Changez de proxy (Parametres)", 
                AlertType.ERROR);
        }
    }
    
    private void showResultsPage() {
        midlet.getDisplay().callSerially(new Runnable() {
            public void run() {
                int totalPages = (allResults.size() + resultsPerPage - 1) / resultsPerPage;
                int startIdx = (currentPage - 1) * resultsPerPage;
                int endIdx = Math.min(startIdx + resultsPerPage, allResults.size());
                
                final List resultList = new List(
                    "PAGE " + currentPage + "/" + totalPages + " (" + allResults.size() + " resultats)", 
                    List.IMPLICIT);
                
                for (int i = startIdx; i < endIdx; i++) {
                    VideoItem item = (VideoItem) allResults.elementAt(i);
                    String displayText = (i + 1) + ". " + item.title;
                    
                    if (displayText.length() > 50) {
                        displayText = displayText.substring(0, 47) + "...";
                    }
                    
                    resultList.append(displayText, null);
                }
                
                resultList.addCommand(new Command("Retour", Command.BACK, 1));
                
                if (currentPage > 1) {
                    resultList.addCommand(new Command("Page Precedente", Command.SCREEN, 2));
                }
                if (currentPage < totalPages) {
                    resultList.addCommand(new Command("Page Suivante", Command.SCREEN, 3));
                }
                
                resultList.setCommandListener(new ResultListener(startIdx));
                midlet.getDisplay().setCurrent(resultList);
            }
        });
    }
    
    private void showProgressScreen(String title, String message) {
        Form progress = new Form(title);
        
        Gauge gauge = new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING);
        progress.append(gauge);
        
        StringItem msg = new StringItem("", "\n" + message + "\n\nVeuillez patienter...");
        progress.append(msg);
        
        midlet.getDisplay().setCurrent(progress);
    }
    
    private void showAlert(String title, String msg, AlertType type) {
        final Alert a = new Alert(title, msg, null, type);
        a.setTimeout(5000);
        midlet.getDisplay().callSerially(new Runnable() {
            public void run() {
                midlet.getDisplay().setCurrent(a, SearchCanvas.this);
            }
        });
    }
    
    class ResultListener implements CommandListener {
        private int pageStartIdx;
        
        public ResultListener(int startIdx) {
            pageStartIdx = startIdx;
        }
        
        public void commandAction(Command c, Displayable d) {
            if (c.getCommandType() == Command.BACK) {
                midlet.getDisplay().setCurrent(SearchCanvas.this);
                return;
            }
            
            if (c.getLabel().equals("Page Precedente")) {
                currentPage--;
                showResultsPage();
                return;
            }
            
            if (c.getLabel().equals("Page Suivante")) {
                currentPage++;
                showResultsPage();
                return;
            }
            
            if (d instanceof List) {
                List list = (List) d;
                int idx = list.getSelectedIndex();
                int actualIdx = pageStartIdx + idx;
                if (actualIdx >= 0 && actualIdx < allResults.size()) {
                    final VideoItem item = (VideoItem) allResults.elementAt(actualIdx);
                    showVideoDetails(item);
                }
            }
        }
        
        private void showVideoDetails(final VideoItem item) {
            Form details = new Form("[VIDEO] " + shortenTitle(item.title, 20));
            
            StringItem info = new StringItem("", 
                ">> Titre: " + item.title + "\n\n" +
                ">> Auteur: " + item.author + "\n" +
                ">> Duree: " + item.duration + "\n" +
                ">> ID: " + item.videoId + "\n");
            details.append(info);
            
            if (item.thumbnailUrl != null && item.thumbnailUrl.length() > 0) {
                final Command showThumbCmd = new Command("Voir Miniature", Command.SCREEN, 1);
                details.addCommand(showThumbCmd);
            }
            
            details.append(new Spacer(details.getWidth(), 10));
            
            final ChoiceGroup quality = new ChoiceGroup(">> QUALITE VIDEO:", Choice.EXCLUSIVE);
            quality.append("144p (Economique)", null);
            quality.append("240p (Basse)", null);
            quality.append("360p (Standard)", null);
            quality.append("480p (SD)", null);
            quality.append("720p (HD)", null);
            quality.append("1080p (Full HD)", null);
            quality.setSelectedIndex(2, true);
            details.append(quality);
            
            details.append(new Spacer(details.getWidth(), 5));
            
            final ChoiceGroup format = new ChoiceGroup(">> FORMAT:", Choice.EXCLUSIVE);
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
                    if (c.getLabel().indexOf("Miniature") != -1 && item.thumbnailUrl != null) {
                        ThumbnailViewer viewer = new ThumbnailViewer(midlet, item.videoId, item.title, SearchCanvas.this);
                        midlet.getDisplay().setCurrent(viewer);
                    } else if (c == dlCmd) {
                        int qualityIdx = quality.getSelectedIndex();
                        int formatIdx = format.getSelectedIndex();
                        startDownload(item, qualityIdx, formatIdx);
                    } else if (c == cancelCmd || c.getCommandType() == Command.BACK) {
                        showResultsPage();
                    }
                }
            });
            
            midlet.getDisplay().setCurrent(details);
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
                        
                        showAlert("SUCCES!", 
                            "Telechargement ajoute a la file d'attente!\n\n" +
                            "VIDEO: " + item.title + "\n" +
                            "QUALITE: " + q + "p\n" +
                            "FORMAT: " + fileFormat.toUpperCase() + "\n\n" +
                            "Consultez 'Telechargements' pour suivre la progression.",
                            AlertType.CONFIRMATION);
                        
                        try { Thread.sleep(2000); } catch (Exception e) {}
                        midlet.backToMenu();
                        
                    } catch (Exception e) {
                        String errorMsg = e.getMessage();
                        if (errorMsg == null) errorMsg = "Erreur inconnue";
                        
                        showAlert("ERREUR DE TELECHARGEMENT",
                            "Impossible de telecharger:\n" + errorMsg + "\n\n" +
                            "Solutions:\n" +
                            "* Activez un proxy (Parametres)\n" +
                            "* Verifiez votre connexion\n" +
                            "* Reessayez avec une autre qualite\n" +
                            "* Utilisez 'Convertir lien' directement",
                            AlertType.ERROR);
                    }
                }
            }).start();
        }
        
        private String shortenTitle(String title, int maxLength) {
            if (title.length() <= maxLength) return title;
            return title.substring(0, maxLength - 3) + "...";
        }
    }
}
