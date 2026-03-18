import javax.microedition.lcdui.*;

public class SettingsCanvas extends Form implements CommandListener {
    private VidmateME midlet;
    private SettingsManager settings;
    
    private StringItem pathDisplay;
    private ChoiceGroup proxyChoice;
    private ChoiceGroup apiChoice;
    private ChoiceGroup qualityChoice;
    private ChoiceGroup thumbnailChoice;
    private ChoiceGroup audioChoice;
    private ChoiceGroup speedLimitChoice;
    
    private Command saveCmd = new Command("Sauvegarder", Command.SCREEN, 1);
    private Command backCmd = new Command("Retour", Command.BACK, 2);
    private Command changePathCmd = new Command("Changer", Command.ITEM, 3);
    private Command statsCmd = new Command("Statistiques", Command.SCREEN, 4);
    
    public SettingsCanvas(VidmateME m) {
        super("Parametres - UniMedia v2.1");
        midlet = m;
        settings = SettingsManager.getInstance();
        
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
        
        proxyChoice = new ChoiceGroup("Proxy:", Choice.EXCLUSIVE);
        proxyChoice.append("Direct (aucun)", null);
        proxyChoice.append("Glype (nnp.nnchan.ru)", null);
        proxyChoice.append("Cloudflare (aged-darkness)", null);
        proxyChoice.append("William's Mobile", null);
        
        String currentProxy = settings.getCurrentProxy();
        if (currentProxy.equals("Direct")) proxyChoice.setSelectedIndex(0, true);
        else if (currentProxy.equals("Glype")) proxyChoice.setSelectedIndex(1, true);
        else if (currentProxy.equals("Cloudflare")) proxyChoice.setSelectedIndex(2, true);
        else if (currentProxy.equals("William")) proxyChoice.setSelectedIndex(3, true);
        
        append(proxyChoice);
        
        append("\n");
        
        apiChoice = new ChoiceGroup("API de recherche:", Choice.EXCLUSIVE);
        apiChoice.append("Dashtube (JSON, rapide)", null);
        apiChoice.append("S60Tube (HTML, fallback)", null);
        
        String currentApi = settings.getCurrentApi();
        if (currentApi.equals("Dashtube")) apiChoice.setSelectedIndex(0, true);
        else apiChoice.setSelectedIndex(1, true);
        
        append(apiChoice);
        
        append("\n");
        
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
        
        speedLimitChoice = new ChoiceGroup("Limite de vitesse:", Choice.EXCLUSIVE);
        speedLimitChoice.append("Illimite (max)", null);
        speedLimitChoice.append("10 KB/s (tres lent)", null);
        speedLimitChoice.append("20 KB/s (lent)", null);
        speedLimitChoice.append("40 KB/s (moyen)", null);
        speedLimitChoice.append("80 KB/s (rapide)", null);
        speedLimitChoice.append("160 KB/s (tres rapide)", null);
        
        int currentSpeedLimit = settings.getSpeedLimitKBps();
        if (currentSpeedLimit == 0) speedLimitChoice.setSelectedIndex(0, true);
        else if (currentSpeedLimit <= 10) speedLimitChoice.setSelectedIndex(1, true);
        else if (currentSpeedLimit <= 20) speedLimitChoice.setSelectedIndex(2, true);
        else if (currentSpeedLimit <= 40) speedLimitChoice.setSelectedIndex(3, true);
        else if (currentSpeedLimit <= 80) speedLimitChoice.setSelectedIndex(4, true);
        else speedLimitChoice.setSelectedIndex(5, true);
        
        append(speedLimitChoice);
        
        StringItem speedInfo = new StringItem("", 
            "Note: Limite la bande passante pour\n" +
            "economiser les donnees mobiles.\n" +
            "'Illimite' = vitesse maximale.\n");
        speedInfo.setLayout(Item.LAYOUT_NEWLINE_AFTER);
        append(speedInfo);
        
        append("\n");
        
        thumbnailChoice = new ChoiceGroup("Afficher thumbnails:", Choice.EXCLUSIVE);
        thumbnailChoice.append("Oui", null);
        thumbnailChoice.append("Non", null);
        
        thumbnailChoice.setSelectedIndex(settings.isShowThumbnails() ? 0 : 1, true);
        append(thumbnailChoice);
        
        append("\n");
        
        audioChoice = new ChoiceGroup("Mode audio par defaut:", Choice.EXCLUSIVE);
        audioChoice.append("Video", null);
        audioChoice.append("Audio seulement", null);
        
        audioChoice.setSelectedIndex(settings.isDownloadAudio() ? 1 : 0, true);
        append(audioChoice);
        
        addCommand(saveCmd);
        addCommand(backCmd);
        addCommand(statsCmd);
        setCommandListener(this);
    }
    
    public void commandAction(Command c, Displayable d) {
        if (c == backCmd) {
            midlet.backToMenu();
        } else if (c == saveCmd) {
            saveSettings();
        } else if (c == statsCmd) {
            showStatsScreen();
        }
    }
    
    private void saveSettings() {
        switch (proxyChoice.getSelectedIndex()) {
            case 0: settings.setCurrentProxy("Direct"); break;
            case 1: settings.setCurrentProxy("Glype"); break;
            case 2: settings.setCurrentProxy("Cloudflare"); break;
            case 3: settings.setCurrentProxy("William"); break;
        }
        
        settings.setCurrentApi(apiChoice.getSelectedIndex() == 0 ? "Dashtube" : "S60Tube");
        
        switch (qualityChoice.getSelectedIndex()) {
            case 0: settings.setDefaultQuality("144p"); break;
            case 1: settings.setDefaultQuality("240p"); break;
            case 2: settings.setDefaultQuality("360p"); break;
            case 3: settings.setDefaultQuality("480p"); break;
        }
        
        int speedLimit = 0;
        switch (speedLimitChoice.getSelectedIndex()) {
            case 0: speedLimit = 0; break;
            case 1: speedLimit = 10; break;
            case 2: speedLimit = 20; break;
            case 3: speedLimit = 40; break;
            case 4: speedLimit = 80; break;
            case 5: speedLimit = 160; break;
        }
        settings.setSpeedLimitKBps(speedLimit);
        
        settings.setShowThumbnails(thumbnailChoice.getSelectedIndex() == 0);
        settings.setDownloadAudio(audioChoice.getSelectedIndex() == 1);
        
        settings.saveSettings();
        
        Alert alert = new Alert("Sauvegarde", 
            "Parametres enregistres avec succes!\n\n" +
            "Stockage: VidmateME/\n" +
            "Limite: " + (speedLimit == 0 ? "Illimitee" : speedLimit + " KB/s"),
            null, AlertType.CONFIRMATION);
        alert.setTimeout(2000);
        midlet.getDisplay().setCurrent(alert, this);
    }
    
    private void showStatsScreen() {
        StatsCanvas stats = new StatsCanvas(midlet, this);
        midlet.getDisplay().setCurrent(stats);
    }
    
    // ✅ CHANGED: Updated path selector with VidmateME folders and auto-detect
    private void showPathSelector() {
        List pathList = new List("Choisir emplacement", List.IMPLICIT);
        
        pathList.append("Auto-detecter (recommande)", null); // ✅ NEW: First option
        pathList.append("Carte SD (E:/VidmateME/)", null);
        pathList.append("Memoire interne (C:/VidmateME/)", null);
        pathList.append("TFCard (TFCard/VidmateME/)", null);
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
                        case 0:
                            // ✅ Auto-detect
                            StorageManager.resetStoragePath();
                            newPath = StorageManager.getDownloadPath();
                            Alert info = new Alert("Auto-detection",
                                "Chemin detecte:\n" + newPath + "\n\n" +
                                "Structure:\n" +
                                "VidmateME/\n" +
                                "  - videos/\n" +
                                "  - audios/\n" +
                                "  - thumbnails/",
                                null, AlertType.INFO);
                            info.setTimeout(4000);
                            midlet.getDisplay().setCurrent(info, SettingsCanvas.this);
                            pathDisplay.setText(newPath);
                            settings.setStoragePath(newPath);
                            settings.saveSettings();
                            return;
                        case 1: 
                            newPath = "file:///E:/VidmateME/"; 
                            break;
                        case 2: 
                            newPath = "file:///C:/VidmateME/"; 
                            break;
                        case 3: 
                            newPath = "file:///TFCard/VidmateME/"; 
                            break;
                        case 4:
                            // Personaliser
                            final TextBox customPath = new TextBox("Chemin personnalise", 
                                settings.getStoragePath(), 100, TextField.URL);
                            customPath.addCommand(new Command("OK", Command.OK, 1));
                            customPath.addCommand(new Command("Annuler", Command.BACK, 2));
                            customPath.setCommandListener(new CommandListener() {
                                public void commandAction(Command c, Displayable d) {
                                    if (c.getCommandType() == Command.OK) {
                                        String custom = customPath.getString();
                                        // ✅ Ensure ends with VidmateME/
                                        if (!custom.endsWith("/")) custom += "/";
                                        if (!custom.endsWith("VidmateME/")) {
                                            if (custom.endsWith("/")) {
                                                custom += "VidmateME/";
                                            } else {
                                                custom += "/VidmateME/";
                                            }
                                        }
                                        
                                        try {
                                            StorageManager.setStoragePath(custom);
                                            settings.setStoragePath(custom);
                                            pathDisplay.setText(custom);
                                            settings.saveSettings();
                                        } catch (Exception e) {
                                            Alert error = new Alert("Erreur",
                                                "Chemin invalide: " + e.getMessage(),
                                                null, AlertType.ERROR);
                                            error.setTimeout(3000);
                                            midlet.getDisplay().setCurrent(error, SettingsCanvas.this);
                                        }
                                    }
                                    midlet.getDisplay().setCurrent(SettingsCanvas.this);
                                }
                            });
                            midlet.getDisplay().setCurrent(customPath);
                            return;
                    }
                    
                    if (!newPath.equals("")) {
                        try {
                            StorageManager.setStoragePath(newPath);
                            settings.setStoragePath(newPath);
                            pathDisplay.setText(newPath);
                            settings.saveSettings();
                        } catch (Exception e) {
                            Alert error = new Alert("Erreur",
                                "Impossible d'utiliser ce chemin.\n" +
                                "Essayez Auto-detecter.",
                                null, AlertType.ERROR);
                            error.setTimeout(2000);
                            midlet.getDisplay().setCurrent(error, SettingsCanvas.this);
                        }
                    }
                    midlet.getDisplay().setCurrent(SettingsCanvas.this);
                }
            }
        });
        midlet.getDisplay().setCurrent(pathList);
    }
}