import java.util.Enumeration;
import java.util.Vector;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;

/**
 * FileSystem - JSR-75 FileConnection wrapper
 *
 * CLDC 1.1 has NO java.lang.reflect - all reflection removed.
 * Uses direct JSR-75 imports and casts.
 *
 * Supported devices: Nokia S40/S60, Sony Ericsson JP7+,
 * Motorola, Samsung SGH, BlackBerry 4.2+, WTK 2.5.2 emulator.
 *
 * Build requirement: jsr75.jar in bootclasspath (see build.xml)
 */
public class FileSystem {

    private static FileSystem instance;

    private FileSystem() {}

    public static FileSystem getInstance() {
        if (instance == null) instance = new FileSystem();
        return instance;
    }

    /** Check JSR-75 availability via Class.forName (CLDC 1.1 supports this) */
    public boolean isAvailable() {
        try {
            Class.forName("javax.microedition.io.file.FileConnection");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** List all storage roots, returns full file:/// URLs */
    public String[] listRoots() {
        try {
            Enumeration e = FileSystemRegistry.listRoots();
            Vector v = new Vector();
            while (e.hasMoreElements()) {
                String root = (String) e.nextElement();
                if (!root.startsWith("file:")) root = "file:///" + root;
                v.addElement(root);
            }
            String[] result = new String[v.size()];
            for (int i = 0; i < v.size(); i++) result[i] = (String) v.elementAt(i);
            return result;
        } catch (Exception ex) {
            return getFallbackRoots();
        }
    }

    /** List directory contents at given URL (must end with /). Dirs first. */
    public FileEntry[] listDir(String url) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(url, Connector.READ);
            Enumeration e = fc.list();
            Vector v = new Vector();
            while (e.hasMoreElements()) {
                String name = (String) e.nextElement();
                String fullUrl = url + name;
                boolean isDir = name.endsWith("/");
                String displayName = isDir ? name.substring(0, name.length()-1) : name;
                long size = 0;
                if (!isDir) {
                    FileConnection fc2 = null;
                    try {
                        fc2 = (FileConnection) Connector.open(fullUrl, Connector.READ);
                        size = fc2.fileSize();
                    } catch (Exception e2) { size = 0; }
                    finally { if (fc2!=null) try{fc2.close();}catch(Exception e3){} }
                }
                v.addElement(new FileEntry(displayName, fullUrl, isDir, size, 0));
            }
            FileEntry[] result = new FileEntry[v.size()];
            for (int i = 0; i < v.size(); i++) result[i] = (FileEntry) v.elementAt(i);
            sortEntries(result);
            return result;
        } catch (Exception ex) {
            return new FileEntry[0];
        } finally {
            if (fc != null) try { fc.close(); } catch (Exception e) {}
        }
    }

    /** Read a text file into String (up to maxBytes bytes) */
    public String readTextFile(String url, int maxBytes) throws Exception {
        FileConnection fc = (FileConnection) Connector.open(url, Connector.READ);
        try {
            InputStream is = fc.openInputStream();
            byte[] buf = new byte[maxBytes];
            int read = 0, b;
            while (read < maxBytes && (b = is.read()) != -1) buf[read++] = (byte)b;
            is.close();
            return new String(buf, 0, read, "UTF-8");
        } finally { fc.close(); }
    }

    /** Rename a file. newName = filename only, no path. */
    public boolean renameFile(String url, String newName) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(url, Connector.READ_WRITE);
            fc.rename(newName);
            return true;
        } catch (Exception ex) { return false; }
        finally { if (fc!=null) try{fc.close();}catch(Exception e){} }
    }

    /** Delete a file or empty directory */
    public boolean deleteFile(String url) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(url, Connector.READ_WRITE);
            fc.delete();
            return true;
        } catch (Exception ex) { return false; }
        finally { if (fc!=null) try{fc.close();}catch(Exception e){} }
    }

    /** Get detailed file metadata */
    public FileInfo getFileInfo(String url) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(url, Connector.READ);
            long size=0, modified=0;
            boolean readable=false, writable=false, hidden=false;
            try { size     = fc.fileSize();     } catch (Exception e) {}
            try { modified = fc.lastModified(); } catch (Exception e) {}
            try { readable = fc.canRead();      } catch (Exception e) {}
            try { writable = fc.canWrite();     } catch (Exception e) {}
            try { hidden   = fc.isHidden();     } catch (Exception e) {}
            return new FileInfo(size, modified, readable, writable, hidden, guessMime(url));
        } catch (Exception ex) {
            return new FileInfo(0, 0, false, false, false, "unknown");
        } finally { if (fc!=null) try{fc.close();}catch(Exception e){} }
    }

    /** Create a new directory */
    public boolean mkdir(String url) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(url, Connector.READ_WRITE);
            fc.mkdir();
            return true;
        } catch (Exception ex) { return false; }
        finally { if (fc!=null) try{fc.close();}catch(Exception e){} }
    }

    /** Write text to a file (create or overwrite) */
    public boolean writeTextFile(String url, String content) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(url, Connector.READ_WRITE);
            if (!fc.exists()) fc.create();
            else              fc.truncate(0);
            OutputStream os = fc.openOutputStream();
            byte[] bytes = content.getBytes("UTF-8");
            os.write(bytes);
            os.flush();
            os.close();
            return true;
        } catch (Exception ex) { return false; }
        finally { if (fc!=null) try{fc.close();}catch(Exception e){} }
    }

    // ===== STATIC HELPERS =====

    private String[] getFallbackRoots() {
        return new String[]{
            "file:///root1/", "file:///root2/",
            "file:///Card/",  "file:///SDCard/",
            "file:///Memory card/", "file:///Phone memory/",
            "file:///C:/", "file:///E:/", "file:///a/", "file:///b/"
        };
    }

    public static String guessMime(String url) {
        if (url == null) return "file";
        String lo = url.toLowerCase();
        if (endsAny(lo, new String[]{".txt",".log",".csv",".ini",".cfg",".nfo",".md"})) return "text";
        if (endsAny(lo, new String[]{".jpg",".jpeg",".png",".gif",".bmp",".webp"}))     return "image";
        if (endsAny(lo, new String[]{".mp3",".aac",".wav",".amr",".m4a",".ogg"}))       return "audio";
        if (endsAny(lo, new String[]{".mp4",".3gp",".avi",".mov",".mkv",".wmv"}))       return "video";
        if (endsAny(lo, new String[]{".jar",".jad"}))                                    return "midlet";
        if (endsAny(lo, new String[]{".java",".xml",".json",".html",".htm",".css",".js",".py",".c",".cpp",".h"})) return "code";
        if (lo.endsWith(".pdf"))                                                           return "pdf";
        if (endsAny(lo, new String[]{".zip",".rar",".gz",".tar",".7z"}))                 return "archive";
        if (lo.endsWith(".vcf"))  return "contact";
        if (lo.endsWith(".ics"))  return "calendar";
        return "file";
    }

    private static boolean endsAny(String s, String[] exts) {
        for (int i = 0; i < exts.length; i++) if (s.endsWith(exts[i])) return true;
        return false;
    }

    public static String formatSize(long bytes) {
        if (bytes < 0)               return "?";
        if (bytes < 1024)            return bytes + "B";
        if (bytes < 1024L * 1024)    return (bytes/1024) + "KB";
        return (bytes/(1024L*1024))  + "MB";
    }

    private void sortEntries(FileEntry[] arr) {
        // Insertion sort: dirs first, then alpha
        for (int i = 1; i < arr.length; i++) {
            FileEntry key = arr[i];
            int j = i - 1;
            while (j >= 0 && shouldSwap(arr[j], key)) { arr[j+1] = arr[j]; j--; }
            arr[j+1] = key;
        }
    }

    private boolean shouldSwap(FileEntry a, FileEntry b) {
        if (!a.isDir && b.isDir) return true;
        if (a.isDir && !b.isDir) return false;
        return a.name.toLowerCase().compareTo(b.name.toLowerCase()) > 0;
    }

    // ===== DATA CLASSES =====

    public static class FileEntry {
        public final String  name;
        public final String  url;
        public final boolean isDir;
        public final long    size;
        public final long    modified;

        public FileEntry(String name, String url, boolean isDir, long size, long modified) {
            this.name=name; this.url=url; this.isDir=isDir;
            this.size=size; this.modified=modified;
        }

        public String getIcon() {
            if (isDir) return "[D]";
            String m = FileSystem.guessMime(url);
            if (m.equals("text"))    return "[T]";
            if (m.equals("image"))   return "[I]";
            if (m.equals("audio"))   return "[A]";
            if (m.equals("video"))   return "[V]";
            if (m.equals("midlet"))  return "[J]";
            if (m.equals("code"))    return "[C]";
            if (m.equals("archive")) return "[Z]";
            if (m.equals("contact")) return "[P]";
            return "[F]";
        }
    }

    public static class FileInfo {
        public final long    size;
        public final long    modified;
        public final boolean readable;
        public final boolean writable;
        public final boolean hidden;
        public final String  mimeType;

        public FileInfo(long size, long modified, boolean readable,
                        boolean writable, boolean hidden, String mimeType) {
            this.size=size; this.modified=modified;
            this.readable=readable; this.writable=writable;
            this.hidden=hidden; this.mimeType=mimeType;
        }
    }
}
