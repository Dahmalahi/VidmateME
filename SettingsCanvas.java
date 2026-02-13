import javax.microedition.lcdui.*;

public class SettingsCanvas extends Form implements CommandListener {
    private VidmateME midlet;
    private SettingsManager settings;
    
    // Elements UI
    private StringItem pathDisplay;
    private ChoiceGroup proxyChoice;
    private ChoiceGroup apiChoice;
    private ChoiceGroup qualityChoice;
    private ChoiceGroup thumbnailChoice;
    private ChoiceGroup audioChoice;
    
    private Command saveCmd = new Command("Sauvegarder", Command.SCREEN, 1);
    private Command backCmd = new Command("Retour", Command.BACK, 2);
    private Command changePathCmd = new Command("Changer", Command.ITEM, 3);
    
    public SettingsCanvas(VidmateME m) {
        super("Parametres");
        midlet = m;
        settings = SettingsManager.getInstance();
        
        // Chemin de stockage
        pathDisplay = new StringItem("Chemin actuel: ", settings.getStoragePath());
        append(pathDisplay);
        changePathCmd = new Command("Changer", Command.ITEM, 1);
        pathDisplay.setDefaultCommand(changePathCmd);
        pathDisplay.setItemCommandListener(new ItemCommandListener() {
            public void commandAction(Command c, Item item) {
                showPathSelector();
            }
        });
        
        append("\n");
        
        // Choix du Proxy
        proxyChoice = new ChoiceGroup("Proxy:", Choice.EXCLUSIVE);
        proxyChoice.append("Direct (aucun)", null);
        proxyChoice.append("Glype (nnp.nnchan.ru)", null);
        proxyChoice.append("Cloudflare (aged-darkness)", null);
        proxyChoice.append("William's Mobile", null);
        
        // Selection actuelle
        String currentProxy = settings.getCurrentProxy();
        if (currentProxy.equals("Direct")) proxyChoice.setSelectedIndex(0, true);
        else if (currentProxy.equals("Glype")) proxyChoice.setSelectedIndex(1, true);
        else if (currentProxy.equals("Cloudflare")) proxyChoice.setSelectedIndex(2, true);
        else if (currentProxy.equals("William")) proxyChoice.setSelectedIndex(3, true);
        
        append(proxyChoice);
        
        append("\n");
        
        // Choix de l'API
        apiChoice = new ChoiceGroup("API de recherche:", Choice.EXCLUSIVE);
        apiChoice.append("S60Tube (HTML)", null);
        apiChoice.append("API Asepharyana (JSON)", null);
        
        String currentApi = settings.getCurrentApi();
        if (currentApi.equals("S60Tube")) apiChoice.setSelectedIndex(0, true);
        else apiChoice.setSelectedIndex(1, true);
        
        append(apiChoice);
        
        append("\n");
        
        // Qualite par defaut
        qualityChoice = new ChoiceGroup("Qualite par defaut:", Choice.EXCLUSIVE);
        qualityChoice.append("144p (bas)", null);
        qualityChoice.append("240p (moyen)", null);
        qualityChoice.append("360p (haut)", null);
        qualityChoice.append("480p (HD)", null);
        
        String currentQuality = settings.getDefaultQuality();
        if (currentQuality.equals("144p")) qualityChoice.setSelectedIndex(0, true);
        else if (currentQuality.equals("240p")) qualityChoice.setSelectedIndex(1, true);
        else if (currentQuality.equals("360p")) qualityChoice.setSelectedIndex(2, true);
        else if (currentQuality.equals("480p")) qualityChoice.setSelectedIndex(3, true);
        
        append(qualityChoice);
        
        append("\n");
        
        // Thumbnails
        thumbnailChoice = new ChoiceGroup("Afficher thumbnails:", Choice.EXCLUSIVE);
        thumbnailChoice.append("Oui", null);
        thumbnailChoice.append("Non", null);
        
        thumbnailChoice.setSelectedIndex(settings.isShowThumbnails() ? 0 : 1, true);
        append(thumbnailChoice);
        
        append("\n");
        
        // Mode audio
        audioChoice = new ChoiceGroup("Mode audio par defaut:", Choice.EXCLUSIVE);
        audioChoice.append("Video", null);
        audioChoice.append("Audio seulement", null);
        
        audioChoice.setSelectedIndex(settings.isDownloadAudio() ? 1 : 0, true);
        append(audioChoice);
        
        // Commandes
        addCommand(saveCmd);
        addCommand(backCmd);
        setCommandListener(this);
    }
    
    public void commandAction(Command c, Displayable d) {
        if (c == backCmd) {
            midlet.backToMenu();
        } else if (c == saveCmd) {
            saveSettings();
        }
    }
    
    private void saveSettings() {
        // Sauvegarder Proxy
        switch (proxyChoice.getSelectedIndex()) {
            case 0: settings.setCurrentProxy("Direct"); break;
            case 1: settings.setCurrentProxy("Glype"); break;
            case 2: settings.setCurrentProxy("Cloudflare"); break;
            case 3: settings.setCurrentProxy("William"); break;
        }
        
        // Sauvegarder API
        settings.setCurrentApi(apiChoice.getSelectedIndex() == 0 ? "S60Tube" : "Asepharyana");
        
        // Sauvegarder Qualite
        switch (qualityChoice.getSelectedIndex()) {
            case 0: settings.setDefaultQuality("144p"); break;
            case 1: settings.setDefaultQuality("240p"); break;
            case 2: settings.setDefaultQuality("360p"); break;
            case 3: settings.setDefaultQuality("480p"); break;
        }
        
        // Sauvegarder Thumbnails
        settings.setShowThumbnails(thumbnailChoice.getSelectedIndex() == 0);
        
        // Sauvegarder Mode Audio
        settings.setDownloadAudio(audioChoice.getSelectedIndex() == 1);
        
        // Sauvegarder dans RMS
        settings.saveSettings();
        
        // Confirmation
        Alert alert = new Alert("Sauvegarde", "Parametres enregistres avec succes!", null, AlertType.CONFIRMATION);
        alert.setTimeout(2000);
        midlet.getDisplay().setCurrent(alert, this);
    }
    
    private void showPathSelector() {
        List pathList = new List("Choisir emplacement", List.IMPLICIT);
        pathList.append("Carte SD (E:/)", null);
        pathList.append("Memoire interne (C:/)", null);
        pathList.append("TFCard", null);
        pathList.append("Personaliser...", null);
        pathList.addCommand(new Command("Retour", Command.BACK, 1));
        pathList.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                if (c.getCommandType() == Command.BACK) {
                    midlet.getDisplay().setCurrent(SettingsCanvas.this);
                } else {
                    List list = (List) d;
                    int idx = list.getSelectedIndex();
                    String newPath = "";
                    
                    switch (idx) {
                        case 0: newPath = "file:///E:/VidmateME/"; break;
                        case 1: newPath = "file:///C:/VidmateME/"; break;
                        case 2: newPath = "file:///TFCard/VidmateME/"; break;
                        case 3:
                            // Personaliser
                            final TextBox customPath = new TextBox("Chemin personnalisé", 
                                settings.getStoragePath(), 100, TextField.URL);
                            customPath.addCommand(new Command("OK", Command.OK, 1));
                            customPath.addCommand(new Command("Annuler", Command.BACK, 2));
                            customPath.setCommandListener(new CommandListener() {
                                public void commandAction(Command c, Displayable d) {
                                    if (c.getCommandType() == Command.OK) {
                                        settings.setStoragePath(customPath.getString());
                                        pathDisplay.setText(settings.getStoragePath());
                                        settings.saveSettings();
                                    }
                                    midlet.getDisplay().setCurrent(SettingsCanvas.this);
                                }
                            });
                            midlet.getDisplay().setCurrent(customPath);
                            return;
                    }
                    
                    if (!newPath.equals("")) {
                        settings.setStoragePath(newPath);
                        pathDisplay.setText(newPath);
                        settings.saveSettings();
                    }
                    midlet.getDisplay().setCurrent(SettingsCanvas.this);
                }
            }
        });
        midlet.getDisplay().setCurrent(pathList);
    }
}