import javax.microedition.io.file.*;
import javax.microedition.io.*;
import java.io.*;
import java.util.Enumeration;

public class StorageManager {
    private static String rootPath = "file:///E:/VidmateME/";
    
    public static String getDownloadPath() {
        Enumeration roots = FileSystemRegistry.listRoots();
        while (roots.hasMoreElements()) {
            String root = (String) roots.nextElement();
            String path = "file:///" + root + "VidmateME/";
            if (isWritable(path)) {
                rootPath = path;
                return path;
            }
        }
        return rootPath;
    }
    
    private static boolean isWritable(String path) {
        try {
            FileConnection fc = (FileConnection) Connector.open(path, Connector.READ_WRITE);
            if (!fc.exists()) {
                fc.mkdir();
            }
            fc.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static OutputStream openOutputStream(String fullPath, boolean append) throws IOException {
        FileConnection fc = (FileConnection) Connector.open(fullPath, 
            append ? Connector.READ_WRITE : Connector.WRITE);
        
        if (append && fc.exists()) {
            // Mode append simple (CLDC 1.1)
            fc.close();
            fc = (FileConnection) Connector.open(fullPath, Connector.WRITE);
            OutputStream os = fc.openOutputStream();
            return os;
        } else {
            if (fc.exists()) fc.delete();
            fc.create();
            return fc.openOutputStream();
        }
    }
    
    public static InputStream openInputStream(String fullPath) throws IOException {
        FileConnection fc = (FileConnection) Connector.open(fullPath, Connector.READ);
        return fc.openInputStream();
    }
}