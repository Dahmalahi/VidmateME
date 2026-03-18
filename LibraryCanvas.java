import javax.microedition.lcdui.*;
import java.util.Vector;

/**
 * LibraryCanvas v2.1 - UniMedia Media Library Browser
 * Full filesystem browser for downloaded media files
 * Uses FileSystem utility for JSR-75 abstraction
 */
public class LibraryCanvas extends Canvas implements CommandListener {

    // ===== UNIMEDIA COLORS =====
    private static final int C_BG       = 0x0A0E27;  // Deep blue-black
    private static final int C_HDR      = 0x161B22;
    private static final int C_ACCENT   = 0x00D9FF;  // Cyan (Dashtube)
    private static final int C_ACCENT2  = 0xFF6B35;  // Orange
    private static final int C_TEXT     = 0xFFFFFF;
    private static final int C_DIM      = 0x8E8E93;
    private static final int C_SEL      = 0x1F2937;
    private static final int C_SEL_BDR  = 0x00D9FF;
    private static final int C_DIR      = 0xFFD700;  // Gold
    private static final int C_VIDEO    = 0x00D9FF;  // Cyan
    private static final int C_AUDIO    = 0xFF6B35;  // Orange
    private static final int C_SUCCESS  = 0x30D158;
    private static final int C_WARN     = 0xFF7B72;
    private static final int C_BORDER   = 0x30363D;

    private static final int HDR_H   = 32;
    private static final int ITEM_H  = 22;
    private static final int FOOT_H  = 18;

    // ===== STATE =====
    private VidmateME midlet;
    private FileSystem fs;
    
    // Navigation
    private Vector pathStack = new Vector();
    private String currentUrl = null;
    private FileSystem.FileEntry[] entries = new FileSystem.FileEntry[0];
    private String[] roots = new String[0];
    private boolean atRootList = true;
    
    // Selection
    private int selectedIdx = 0;
    private int scrollOffset = 0;
    
    // Mode
    private static final int MODE_BROWSE   = 0;
    private static final int MODE_LOADING  = 1;
    private static final int MODE_INFO     = 2;
    private static final int MODE_NO_JSR75 = 3;
    private static final int MODE_CONFIRM  = 4;
    private int mode = MODE_LOADING;
    
    // Confirm dialog
    private String confirmMsg = "";
    private FileSystem.FileEntry confirmEntry = null;
    
    // Status
    private String statusMsg = "";
    private long statusTime = 0;
    
    // Sort
    private int sortMode = 0; // 0=name, 1=size, 2=type
    private static final String[] SORT_LABELS = {"Nom", "Taille", "Type"};
    
    // Commands
    private Command backCmd    = new Command("Retour", Command.BACK, 1);
    private Command playCmd    = new Command("Lire", Command.OK, 2);
    private Command infoCmd    = new Command("Infos", Command.SCREEN, 3);
    private Command deleteCmd  = new Command("Supprimer", Command.SCREEN, 4);
    private Command refreshCmd = new Command("Actualiser", Command.SCREEN, 5);
    private Command sortCmd    = new Command("Trier", Command.SCREEN, 6);
    private Command debugCmd   = new Command("Debug", Command.SCREEN, 7);
    
    public LibraryCanvas(VidmateME m) {
        midlet = m;
        fs = FileSystem.getInstance();
        setCommandListener(this);
        addCommand(backCmd);
        addCommand(playCmd);
        addCommand(infoCmd);
        addCommand(deleteCmd);
        addCommand(refreshCmd);
        addCommand(sortCmd);
        addCommand(debugCmd);
        loadRoots();
    }
    
    // ===== DATA LOADING =====
    
    private void loadRoots() {
        mode = MODE_LOADING;
        repaint();
        new Thread(new Runnable() {
            public void run() {
                if (!fs.isAvailable()) {
                    mode = MODE_NO_JSR75;
                    repaint();
                    return;
                }
                
                roots = fs.listRoots();
                atRootList = true;
                selectedIdx = 0;
                scrollOffset = 0;
                mode = MODE_BROWSE;
                
                System.out.println("[LIBRARY] Found " + roots.length + " roots");
                for (int i = 0; i < roots.length; i++) {
                    System.out.println("  [" + i + "] " + roots[i]);
                }
                
                repaint();
            }
        }).start();
    }
    
    private void loadDir(final String url) {
        mode = MODE_LOADING;
        statusMsg = "Chargement...";
        repaint();
        new Thread(new Runnable() {
            public void run() {
                System.out.println("[LIBRARY] Loading: " + url);
                entries = fs.listDir(url);
                currentUrl = url;
                atRootList = false;
                selectedIdx = 0;
                scrollOffset = 0;
                sortEntries();
                mode = MODE_BROWSE;
                
                System.out.println("[LIBRARY] Loaded " + entries.length + " items");
                repaint();
            }
        }).start();
    }
    
    // ===== PAINT =====
    
    protected void paint(Graphics g) {
        int W = getWidth(), H = getHeight();
        
        g.setColor(C_BG);
        g.fillRect(0, 0, W, H);
        
        switch (mode) {
            case MODE_LOADING:  drawLoading(g, W, H); break;
            case MODE_NO_JSR75: drawNoJSR75(g, W, H); break;
            case MODE_BROWSE:   drawBrowser(g, W, H); break;
            case MODE_INFO:     drawInfo(g, W, H);    break;
            case MODE_CONFIRM:  drawBrowser(g, W, H); drawConfirm(g, W, H); break;
        }
    }
    
    private void drawHeader(Graphics g, int W, String title) {
        g.setColor(C_HDR);
        g.fillRect(0, 0, W, HDR_H);
        g.setColor(C_ACCENT);
        g.fillRect(0, 0, W, 3);
        
        g.setColor(C_TEXT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
        
        String display = title;
        Font f = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
        int maxW = W - 70;
        while (display.length() > 5 && f.stringWidth(display) > maxW) {
            display = ".." + display.substring(3);
        }
        g.drawString(display, 6, 8, Graphics.TOP | Graphics.LEFT);
        
        g.setColor(C_ACCENT2);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        g.drawString(SORT_LABELS[sortMode], W - 6, 10, Graphics.TOP | Graphics.RIGHT);
    }
    
    private void drawFooter(Graphics g, int W, int H) {
        g.setColor(C_HDR);
        g.fillRect(0, H - FOOT_H, W, FOOT_H);
        g.setColor(C_BORDER);
        g.fillRect(0, H - FOOT_H - 1, W, 1);
        
        String msg = "";
        if (statusMsg.length() > 0 && System.currentTimeMillis() - statusTime < 3000) {
            msg = statusMsg;
        } else {
            if (atRootList) {
                msg = "Racines: " + roots.length;
            } else {
                int fileCount = 0, dirCount = 0;
                for (int i = 0; i < entries.length; i++) {
                    if (entries[i].isDir) dirCount++; else fileCount++;
                }
                msg = "Dossiers:" + dirCount + " Fichiers:" + fileCount;
            }
        }
        
        g.setColor(C_DIM);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        g.drawString(msg, W/2, H - FOOT_H + 3, Graphics.TOP | Graphics.HCENTER);
    }
    
    private void drawBrowser(Graphics g, int W, int H) {
        String headerTitle;
        if (atRootList) {
            headerTitle = "[Bibliotheque] Stockage";
        } else {
            headerTitle = currentUrl != null ? currentUrl : "/";
        }
        drawHeader(g, W, headerTitle);
        
        int listH = H - HDR_H - FOOT_H;
        int visibleCount = listH / ITEM_H;
        int itemCount = atRootList ? roots.length : entries.length;
        
        if (itemCount == 0) {
            g.setColor(C_DIM);
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM));
            g.drawString("Aucun fichier", W/2, H/2 - 30, Graphics.TOP | Graphics.HCENTER);
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
            g.drawString("Telechargez depuis:", W/2, H/2, Graphics.TOP | Graphics.HCENTER);
            g.drawString("Rechercher Videos", W/2, H/2 + 15, Graphics.TOP | Graphics.HCENTER);
            g.drawString("ou Convertir Lien", W/2, H/2 + 30, Graphics.TOP | Graphics.HCENTER);
            drawFooter(g, W, H);
            return;
        }
        
        if (scrollOffset > itemCount - visibleCount)
            scrollOffset = Math.max(0, itemCount - visibleCount);
        if (selectedIdx < scrollOffset) scrollOffset = selectedIdx;
        if (selectedIdx >= scrollOffset + visibleCount)
            scrollOffset = selectedIdx - visibleCount + 1;
        
        Font fnSmall = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        Font fnBold  = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD,  Font.SIZE_SMALL);
        
        for (int i = scrollOffset; i < itemCount && i < scrollOffset + visibleCount; i++) {
            int y = HDR_H + (i - scrollOffset) * ITEM_H;
            boolean isSel = (i == selectedIdx);
            
            String name, icon, sizeStr;
            boolean isDir;
            int iconColor;
            
            if (atRootList) {
                name = prettifyRoot(roots[i]);
                icon = "[*]";
                sizeStr = "";
                isDir = true;
                iconColor = C_ACCENT;
            } else {
                FileSystem.FileEntry e = entries[i];
                name = e.name;
                icon = e.getIcon();
                sizeStr = e.isDir ? "" : FileSystem.formatSize(e.size);
                isDir = e.isDir;
                iconColor = getIconColor(icon, isDir);
            }
            
            if (isSel) {
                g.setColor(C_SEL);
                g.fillRect(0, y, W, ITEM_H);
                g.setColor(C_SEL_BDR);
                g.drawRect(0, y, W - 1, ITEM_H - 1);
            }
            
            g.setColor(iconColor);
            g.setFont(fnBold);
            g.drawString(icon, 6, y + 4, Graphics.TOP | Graphics.LEFT);
            
            int nameX = 32;
            int nameW = W - nameX - (sizeStr.length() > 0 ? 50 : 6);
            g.setColor(isSel ? C_TEXT : (isDir ? C_DIR : C_VIDEO));
            g.setFont(isSel ? fnBold : fnSmall);
            String dispName = name;
            while (dispName.length() > 3 && (isSel ? fnBold : fnSmall).stringWidth(dispName) > nameW) {
                dispName = dispName.substring(0, dispName.length() - 4) + "...";
            }
            g.drawString(dispName, nameX, y + 4, Graphics.TOP | Graphics.LEFT);
            
            if (sizeStr.length() > 0) {
                g.setColor(isSel ? C_ACCENT2 : C_DIM);
                g.setFont(fnSmall);
                g.drawString(sizeStr, W - 6, y + 4, Graphics.TOP | Graphics.RIGHT);
            }
            
            if (!isSel) {
                g.setColor(C_BORDER);
                g.drawLine(32, y + ITEM_H - 1, W - 1, y + ITEM_H - 1);
            }
        }
        
        if (itemCount > visibleCount) {
            int sbH = listH;
            int thumbH = Math.max(8, sbH * visibleCount / itemCount);
            int thumbY = HDR_H + sbH * scrollOffset / itemCount;
            g.setColor(C_BORDER);
            g.fillRect(W - 4, HDR_H, 4, sbH);
            g.setColor(C_ACCENT);
            g.fillRect(W - 4, thumbY, 4, thumbH);
        }
        
        drawFooter(g, W, H);
    }
    
    private void drawLoading(Graphics g, int W, int H) {
        g.setColor(C_ACCENT);
        g.fillRect(0, 0, W, 3);
        g.setColor(C_HDR);
        g.fillRect(0, 3, W, HDR_H - 3);
        g.setColor(C_TEXT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
        g.drawString("UniMedia Bibliotheque", 6, 8, Graphics.TOP | Graphics.LEFT);
        
        g.setColor(C_ACCENT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE));
        g.drawString("Chargement...", W/2, H/2 - 12, Graphics.TOP | Graphics.HCENTER);
        g.setColor(C_DIM);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        g.drawString("Acces au systeme de fichiers", W/2, H/2 + 10, Graphics.TOP | Graphics.HCENTER);
    }
    
    private void drawNoJSR75(Graphics g, int W, int H) {
        g.setColor(C_WARN);
        g.fillRect(0, 0, W, 3);
        g.setColor(C_HDR);
        g.fillRect(0, 3, W, HDR_H - 3);
        g.setColor(C_WARN);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
        g.drawString("Bibliotheque", 6, 8, Graphics.TOP | Graphics.LEFT);
        
        int cy = 50;
        g.setColor(C_WARN);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE));
        g.drawString("JSR-75 Introuvable", W/2, cy, Graphics.TOP | Graphics.HCENTER);
        
        g.setColor(C_TEXT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        String[] lines = {
            "",
            "Ce telephone ne supporte pas",
            "javax.microedition.io.file",
            "",
            "Necessaire pour:",
            "- Parcourir fichiers",
            "- Voir telechargements",
            "",
            "Compatibles:",
            " Nokia S60/S40 3rd+",
            " Sony Ericsson JP7+",
            " Motorola/Samsung",
            "",
            "Emulateur: activez JSR-75"
        };
        for (int i = 0; i < lines.length; i++) {
            g.drawString(lines[i], W/2, cy + 20 + i * 13, Graphics.TOP | Graphics.HCENTER);
        }
        
        g.setColor(C_DIM);
        g.drawString("Retour pour menu", W/2, H - 20, Graphics.TOP | Graphics.HCENTER);
    }
    
    private void drawInfo(Graphics g, int W, int H) {
        if (atRootList || entries.length == 0) {
            mode = MODE_BROWSE;
            repaint();
            return;
        }
        
        FileSystem.FileEntry entry = entries[selectedIdx];
        FileSystem.FileInfo info = fs.getFileInfo(entry.url);
        
        drawHeader(g, W, "Infos Fichier");
        
        int y = HDR_H + 8;
        Font fb = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        Font fn = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        
        g.setColor(getIconColor(entry.getIcon(), entry.isDir));
        g.setFont(fb);
        g.drawString(entry.getIcon(), 6, y, Graphics.TOP | Graphics.LEFT);
        g.setColor(entry.isDir ? C_DIR : C_VIDEO);
        String shortName = entry.name;
        while (shortName.length() > 3 && fb.stringWidth(shortName) > W - 40) {
            shortName = shortName.substring(0, shortName.length() - 4) + "...";
        }
        g.drawString(shortName, 32, y, Graphics.TOP | Graphics.LEFT);
        y += 20;
        
        String[][] rows = {
            {"Type",    entry.isDir ? "Dossier" : info.mimeType},
            {"Taille",  entry.isDir ? "-" : FileSystem.formatSize(info.size)},
            {"Octets",  entry.isDir ? "-" : String.valueOf(info.size)},
            {"Lecture", info.readable ? "Oui" : "Non"},
            {"Ecriture", info.writable ? "Oui" : "Non"},
        };
        
        for (int i = 0; i < rows.length; i++) {
            int ry = y + i * 18;
            g.setColor((i % 2 == 0) ? 0xFF0D1117 : 0xFF131920);
            g.fillRect(4, ry, W - 8, 18);
            
            g.setColor(C_DIM);
            g.setFont(fn);
            g.drawString(rows[i][0] + ":", 8, ry + 3, Graphics.TOP | Graphics.LEFT);
            
            g.setColor(C_TEXT);
            String value = rows[i][1];
            while (value.length() > 3 && fn.stringWidth(value) > W - W/3 - 12) {
                value = value.substring(0, value.length() - 4) + "...";
            }
            g.drawString(value, W/3, ry + 3, Graphics.TOP | Graphics.LEFT);
        }
        
        y += rows.length * 18 + 8;
        
        g.setColor(C_ACCENT2);
        g.setFont(fn);
        g.drawString("Chemin:", 8, y, Graphics.TOP | Graphics.LEFT);
        g.setColor(C_DIM);
        String urlDisp = entry.url;
        while (urlDisp.length() > 4 && fn.stringWidth(urlDisp) > W - 16) {
            urlDisp = urlDisp.substring(0, urlDisp.length() - 4) + "...";
        }
        g.drawString(urlDisp, 8, y + 12, Graphics.TOP | Graphics.LEFT);
        
        drawFooter(g, W, H);
        g.setColor(C_DIM);
        g.setFont(fn);
        g.drawString("Fire/Retour: fermer", W/2, H - FOOT_H - 16, Graphics.TOP | Graphics.HCENTER);
    }
    
    private void drawConfirm(Graphics g, int W, int H) {
        int bw = W - 20, bh = 70;
        int bx = 10, by = H/2 - bh/2;
        
        g.setColor(C_HDR);
        g.fillRect(bx, by, bw, bh);
        g.setColor(C_WARN);
        g.drawRect(bx, by, bw - 1, bh - 1);
        g.fillRect(bx, by, bw, 3);
        
        g.setColor(C_WARN);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
        g.drawString("Confirmer", W/2, by + 8, Graphics.TOP | Graphics.HCENTER);
        
        g.setColor(C_TEXT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        
        // Word wrap message
        String[] lines = wrapText(confirmMsg, W - 30);
        for (int i = 0; i < lines.length && i < 2; i++) {
            g.drawString(lines[i], W/2, by + 24 + i * 13, Graphics.TOP | Graphics.HCENTER);
        }
        
        g.setColor(C_SUCCESS);
        g.drawString("Fire=OUI", W/4, by + 52, Graphics.TOP | Graphics.HCENTER);
        g.setColor(C_WARN);
        g.drawString("Retour=NON", W*3/4, by + 52, Graphics.TOP | Graphics.HCENTER);
    }
    
    // ===== KEY HANDLING =====
    
    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);
        
        if (mode == MODE_CONFIRM) {
            if (action == Canvas.FIRE) {
                executeDelete();
            } else {
                mode = MODE_BROWSE;
                repaint();
            }
            return;
        }
        
        if (mode == MODE_INFO) {
            mode = MODE_BROWSE;
            repaint();
            return;
        }
        
        if (mode != MODE_BROWSE) return;
        
        int count = atRootList ? roots.length : entries.length;
        
        switch (action) {
            case Canvas.UP:
                if (selectedIdx > 0) { selectedIdx--; repaint(); }
                break;
            case Canvas.DOWN:
                if (selectedIdx < count - 1) { selectedIdx++; repaint(); }
                break;
            case Canvas.FIRE:
                openSelected();
                break;
            case Canvas.LEFT:
                navigateUp();
                break;
            case Canvas.RIGHT:
                openSelected();
                break;
        }
        
        if ((char)keyCode == '#') {
            showInfo();
        }
    }
    
    // ===== ACTIONS =====
    
    private void openSelected() {
        if (atRootList) {
            if (roots.length == 0) return;
            pathStack.addElement("root");
            
            String rootUrl = roots[selectedIdx];
            String vidmateUrl = rootUrl;
            if (!vidmateUrl.endsWith("/")) vidmateUrl += "/";
            vidmateUrl += "VidmateME/";
            
            FileSystem.FileEntry[] test = fs.listDir(vidmateUrl);
            if (test.length > 0 || dirExists(vidmateUrl)) {
                loadDir(vidmateUrl);
            } else {
                loadDir(rootUrl);
            }
        } else {
            if (entries.length == 0) return;
            FileSystem.FileEntry entry = entries[selectedIdx];
            if (entry.isDir) {
                pathStack.addElement(currentUrl);
                String dirUrl = entry.url;
                if (!dirUrl.endsWith("/")) dirUrl += "/";
                loadDir(dirUrl);
            } else {
                playFile(entry);
            }
        }
    }
    
    private boolean dirExists(String url) {
        FileSystem.FileInfo info = fs.getFileInfo(url);
        return info.readable;
    }
    
    private void playFile(FileSystem.FileEntry entry) {
        String mime = FileSystem.guessMime(entry.name);
        
        System.out.println("[LIBRARY] Playing: " + entry.name + " (type=" + mime + ")");
        
        if (mime.equals("video")) {
            PlayerCanvas player = new PlayerCanvas(midlet, entry.url, entry.name);
            midlet.getDisplay().setCurrent(player);
        } else if (mime.equals("audio")) {
            AudioPlayerCanvas player = new AudioPlayerCanvas(midlet, entry.url, entry.name);
            midlet.getDisplay().setCurrent(player);
        } else {
            mode = MODE_INFO;
            repaint();
        }
    }
    
    private void navigateUp() {
        if (pathStack.isEmpty()) {
            midlet.backToMenu();
        } else {
            String parent = (String) pathStack.lastElement();
            pathStack.removeElementAt(pathStack.size() - 1);
            if (parent.equals("root")) {
                atRootList = true;
                currentUrl = null;
                selectedIdx = 0;
                scrollOffset = 0;
                mode = MODE_BROWSE;
                repaint();
            } else {
                loadDir(parent);
            }
        }
    }
    
    private void showInfo() {
        if (!atRootList && entries.length > 0) {
            mode = MODE_INFO;
            repaint();
        }
    }
    
    private void confirmDelete() {
        if (atRootList || entries.length == 0) return;
        
        confirmEntry = entries[selectedIdx];
        confirmMsg = "Supprimer: " + confirmEntry.name + "?";
        mode = MODE_CONFIRM;
        repaint();
    }
    
    private void executeDelete() {
        if (confirmEntry == null) {
            mode = MODE_BROWSE;
            repaint();
            return;
        }
        
        final FileSystem.FileEntry entry = confirmEntry;
        confirmEntry = null;
        
        new Thread(new Runnable() {
            public void run() {
                boolean ok = fs.deleteFile(entry.url);
                setStatus(ok ? "Supprime: " + entry.name : "Echec suppression");
                loadDir(currentUrl);
            }
        }).start();
    }
    
    public void refresh() {
        if (atRootList) {
            loadRoots();
        } else if (currentUrl != null) {
            loadDir(currentUrl);
        }
    }
    
    private void sortEntries() {
        if (entries == null || entries.length <= 1) return;
        
        for (int i = 0; i < entries.length - 1; i++) {
            for (int j = 0; j < entries.length - 1 - i; j++) {
                boolean swap = false;
                FileSystem.FileEntry a = entries[j], b = entries[j+1];
                
                switch (sortMode) {
                    case 0: // Name - dirs first
                        if (!a.isDir && b.isDir) swap = true;
                        else if (a.isDir == b.isDir)
                            swap = a.name.toLowerCase().compareTo(b.name.toLowerCase()) > 0;
                        break;
                    case 1: // Size - largest first
                        if (a.isDir && !b.isDir) swap = true;
                        else if (!a.isDir && !b.isDir) swap = a.size < b.size;
                        break;
                    case 2: // Type
                        String mimeA = FileSystem.guessMime(a.name);
                        String mimeB = FileSystem.guessMime(b.name);
                        swap = mimeA.compareTo(mimeB) > 0;
                        break;
                }
                
                if (swap) {
                    FileSystem.FileEntry tmp = entries[j];
                    entries[j] = entries[j+1];
                    entries[j+1] = tmp;
                }
            }
        }
    }
    
    // ===== DEBUG =====
    
    private void showDebug() {
        Form debugForm = new Form("Debug Bibliotheque");
        
        debugForm.append("=== ETAT ACTUEL ===\n");
        debugForm.append("JSR-75: " + (fs.isAvailable() ? "OK" : "ABSENT") + "\n");
        debugForm.append("Mode: " + (atRootList ? "Racines" : "Dossier") + "\n");
        debugForm.append("URL: " + (currentUrl != null ? currentUrl : "null") + "\n");
        debugForm.append("Fichiers: " + entries.length + "\n\n");
        
        debugForm.append("=== RACINES DETECTEES ===\n");
        for (int i = 0; i < roots.length; i++) {
            debugForm.append((i+1) + ". " + roots[i] + "\n");
        }
        
        debugForm.append("\n=== TEST VIDMATEME ===\n");
        String[] testPaths = {
            "file:///root1/VidmateME/",
            "file:///root/VidmateME/",
            "file:///E:/VidmateME/",
            "file:///C:/VidmateME/"
        };
        
        for (int i = 0; i < testPaths.length; i++) {
            FileSystem.FileEntry[] test = fs.listDir(testPaths[i]);
            if (test.length > 0) {
                debugForm.append("[OK] " + testPaths[i] + "\n");
                
                FileSystem.FileEntry[] videos = fs.listDir(testPaths[i] + "videos/");
                FileSystem.FileEntry[] audios = fs.listDir(testPaths[i] + "audios/");
                debugForm.append("  videos/: " + videos.length + "\n");
                debugForm.append("  audios/: " + audios.length + "\n");
            } else {
                debugForm.append("[VIDE] " + testPaths[i] + "\n");
            }
        }
        
        debugForm.addCommand(new Command("OK", Command.OK, 1));
        debugForm.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                midlet.getDisplay().setCurrent(LibraryCanvas.this);
            }
        });
        midlet.getDisplay().setCurrent(debugForm);
    }
    
    // ===== HELPERS =====
    
    private String prettifyRoot(String url) {
        if (url == null) return "";
        String s = url;
        if (s.startsWith("file:///")) s = s.substring(8);
        if (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s.length() == 0 ? url : s;
    }
    
    private int getIconColor(String icon, boolean isDir) {
        if (isDir) return C_DIR;
        if (icon.equals("[V]")) return C_VIDEO;
        if (icon.equals("[A]")) return C_AUDIO;
        if (icon.equals("[I]")) return 0xFF8C00;
        if (icon.equals("[T]")) return C_TEXT;
        return C_ACCENT;
    }
    
    private void setStatus(String msg) {
        statusMsg = msg;
        statusTime = System.currentTimeMillis();
    }
    
    private String[] wrapText(String text, int maxWidth) {
        Vector lines = new Vector();
        Font f = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        
        String[] words = split(text, ' ');
        StringBuffer line = new StringBuffer();
        
        for (int i = 0; i < words.length; i++) {
            String testLine = line.length() == 0 ? words[i] : line.toString() + " " + words[i];
            if (f.stringWidth(testLine) > maxWidth && line.length() > 0) {
                lines.addElement(line.toString());
                line = new StringBuffer(words[i]);
            } else {
                if (line.length() > 0) line.append(" ");
                line.append(words[i]);
            }
        }
        if (line.length() > 0) lines.addElement(line.toString());
        
        String[] result = new String[lines.size()];
        for (int i = 0; i < lines.size(); i++) result[i] = (String) lines.elementAt(i);
        return result;
    }
    
    private String[] split(String str, char delim) {
        Vector v = new Vector();
        int start = 0;
        for (int i = 0; i <= str.length(); i++) {
            if (i == str.length() || str.charAt(i) == delim) {
                if (i > start) v.addElement(str.substring(start, i));
                start = i + 1;
            }
        }
        String[] result = new String[v.size()];
        for (int i = 0; i < v.size(); i++) result[i] = (String) v.elementAt(i);
        return result;
    }
    
    // ===== COMMANDS =====
    
    public void commandAction(Command c, Displayable d) {
        if (c == backCmd) {
            if (mode == MODE_INFO) {
                mode = MODE_BROWSE;
                repaint();
            } else {
                navigateUp();
            }
        } else if (c == playCmd) {
            openSelected();
        } else if (c == infoCmd) {
            showInfo();
        } else if (c == deleteCmd) {
            confirmDelete();
        } else if (c == refreshCmd) {
            refresh();
        } else if (c == sortCmd) {
            sortMode = (sortMode + 1) % 3;
            sortEntries();
            setStatus("Tri: " + SORT_LABELS[sortMode]);
            repaint();
        } else if (c == debugCmd) {
            showDebug();
        }
    }
}