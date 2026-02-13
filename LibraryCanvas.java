import javax.microedition.lcdui.*;
import javax.microedition.io.file.*;
import javax.microedition.io.*;
import java.util.Enumeration;
import java.util.Vector;

public class LibraryCanvas extends List implements CommandListener {
    private VidmateME midlet;
    private Vector filePaths = new Vector();
    private final Command playCmd = new Command("Lire", Command.OK, 1);
    private final Command deleteCmd = new Command("Supprimer", Command.ITEM, 2);
    private final Command backCmd = new Command("Retour", Command.BACK, 3);
    
    public LibraryCanvas(VidmateME m) {
        super("Bibliotheque Videos", List.IMPLICIT);
        midlet = m;
        addCommand(playCmd);
        addCommand(deleteCmd);
        addCommand(backCmd);
        setCommandListener(this);
        refresh();
    }
    
    public void refresh() {
        deleteAll();
        filePaths.removeAllElements();
        
        try {
            String videosPath = StorageManager.getDownloadPath() + "videos/";
            FileConnection fc = (FileConnection) Connector.open(videosPath);
            
            if (!fc.exists()) {
                append("Dossier vide", null);
                fc.mkdir();
                fc.close();
                return;
            }
            
            // Videos
            Enumeration files = fc.list("*.mp4;*.3gp;*.avi", false);
            while (files.hasMoreElements()) {
                String name = (String) files.nextElement();
                String fullPath = videosPath + name;
                filePaths.addElement(fullPath);
                append("[VIDEO] " + name, null);
            }
            fc.close();
            
            // Audios
            String audiosPath = StorageManager.getDownloadPath() + "audios/";
            fc = (FileConnection) Connector.open(audiosPath);
            if (!fc.exists()) fc.mkdir();
            
            files = fc.list("*.mp3;*.wav;*.amr", false);
            while (files.hasMoreElements()) {
                String name = (String) files.nextElement();
                String fullPath = audiosPath + name;
                filePaths.addElement(fullPath);
                append("[AUDIO] " + name, null);
            }
            fc.close();
            
            if (size() == 0) {
                append("Aucun fichier", null);
                append("Utilisez 'Rechercher' ou 'Convertir'", null);
            }
        } catch (Exception e) {
            append("Erreur: " + e.getMessage(), null);
        }
    }
    
    public void commandAction(Command c, Displayable d) {
        if (c == backCmd) {
            midlet.backToMenu();
        } else if ((c == playCmd || c == List.SELECT_COMMAND) && size() > 0) {
            int idx = getSelectedIndex();
            if (idx >= 0 && idx < filePaths.size()) {
                String path = (String) filePaths.elementAt(idx);
                String name = getString(idx);
                
                // Determiner le type (video ou audio)
                boolean isAudio = name.startsWith("[AUDIO]");
                name = name.substring(name.indexOf("]") + 2); // Enlever prefixe
                
                if (isAudio) {
                    AudioPlayerCanvas player = new AudioPlayerCanvas(midlet, path, name);
                    midlet.getDisplay().setCurrent(player);
                } else {
                    PlayerCanvas player = new PlayerCanvas(midlet, path, name);
                    midlet.getDisplay().setCurrent(player);
                }
            }
        } else if (c == deleteCmd && size() > 0) {
            int idx = getSelectedIndex();
            if (idx >= 0 && idx < filePaths.size()) {
                confirmDelete(idx);
            }
        }
    }
    
    private void confirmDelete(final int idx) {
        Alert confirm = new Alert("Confirmer", 
            "Supprimer " + getString(idx) + " ?", 
            null, AlertType.CONFIRMATION);
        confirm.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                if (c.getLabel().equals("OK")) {
                    try {
                        String path = (String) filePaths.elementAt(idx);
                        FileConnection fc = (FileConnection) Connector.open(path);
                        if (fc.exists()) fc.delete();
                        fc.close();
                        filePaths.removeElementAt(idx);
                        delete(idx);
                        Alert success = new Alert("Succes", "Fichier supprime", null, AlertType.INFO);
                        success.setTimeout(1500);
                        midlet.getDisplay().setCurrent(success, LibraryCanvas.this);
                    } catch (Exception e) {
                        Alert err = new Alert("Erreur", "Echec suppression", null, AlertType.ERROR);
                        err.setTimeout(2000);
                        midlet.getDisplay().setCurrent(err, LibraryCanvas.this);
                    }
                } else {
                    midlet.getDisplay().setCurrent(LibraryCanvas.this);
                }
            }
        });
        confirm.setTimeout(Alert.FOREVER);
        midlet.getDisplay().setCurrent(confirm, this);
    }
}