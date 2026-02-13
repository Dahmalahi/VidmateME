public class APIManager {
    private static final String WILLIAMS_BASE = "http://williamsmobile.co.uk/yt.php?url=";
    
    // Nouvelle signature avec support du format
    public static String getDownloadUrl(String videoId, String quality, boolean audioOnly, String fileFormat) throws Exception {
        Exception lastError = null;
        
        // Essayer William's Mobile
        try {
            return getWilliamsUrl(videoId, quality, audioOnly, fileFormat);
        } catch (Exception e) {
            lastError = e;
        }
        
        // Fallback: S60Tube
        try {
            return getS60TubeUrl(videoId, quality, fileFormat);
        } catch (Exception e) {
            lastError = e;
        }
        
        throw new Exception("Toutes les APIs ont echoue. Derniere erreur: " + 
            (lastError != null ? lastError.getMessage() : "Inconnue"));
    }
    
    // Ancienne signature pour compatibilité
    public static String getDownloadUrl(String videoId, String quality, boolean audioOnly) throws Exception {
        String format = audioOnly ? "mp3" : "mp4";
        return getDownloadUrl(videoId, quality, audioOnly, format);
    }
    
    private static String getWilliamsUrl(String videoId, String quality, boolean audioOnly, String fileFormat) throws Exception {
        String ytUrl = "https://www.youtube.com/watch?v=" + videoId;
        String encoded = encodeUrl(ytUrl);
        
        if (audioOnly) {
            // Audio: mp3, aac, wav
            String audioFormat = fileFormat;
            if (!audioFormat.equals("mp3") && !audioFormat.equals("aac") && !audioFormat.equals("wav")) {
                audioFormat = "mp3"; // Par défaut
            }
            return WILLIAMS_BASE + encoded + "&format=" + audioFormat;
        } else {
            // Vidéo: mp4, 3gp
            String videoFormat = fileFormat;
            if (quality.equals("144") || quality.equals("240")) {
                videoFormat = "3gp"; // Forcer 3GP pour basses qualités si demandé
            }
            if (!videoFormat.equals("mp4") && !videoFormat.equals("3gp")) {
                videoFormat = "mp4"; // Par défaut
            }
            return WILLIAMS_BASE + encoded + "&format=" + videoFormat + "&quality=" + quality + "p";
        }
    }
    
    private static String getS60TubeUrl(String videoId, String quality, String fileFormat) throws Exception {
        String pageUrl = "http://s60tube.io.vn/video/" + videoId;
        String page = HttpUtils.fetch(pageUrl);
        
        String pattern = "videoplayback?v=" + videoId;
        int pos = page.indexOf(pattern);
        if (pos == -1) {
            throw new Exception("Lien non trouve dans S60Tube");
        }
        
        int start = -1;
        for (int i = pos - 1; i >= 0 && i > pos - 100; i--) {
            if (i + 7 <= page.length()) {
                String substr = page.substring(i, Math.min(i + 7, page.length()));
                if (substr.equals("http://")) {
                    start = i;
                    break;
                }
            }
            if (page.charAt(i) == '"' || page.charAt(i) == '\'') {
                if (i + 1 < pos && page.substring(i + 1, Math.min(i + 8, page.length())).startsWith("http://")) {
                    start = i + 1;
                }
                break;
            }
        }
        
        if (start == -1) {
            throw new Exception("Debut URL non trouve");
        }
        
        int end = page.indexOf("\"", pos);
        if (end == -1) end = page.indexOf("'", pos);
        if (end == -1) end = page.indexOf(" ", pos);
        if (end == -1 || end < pos) {
            throw new Exception("Fin URL non trouvee");
        }
        
        String downloadUrl = page.substring(start, end);
        
        if (downloadUrl.indexOf("quality=") == -1) {
            if (downloadUrl.indexOf("?") == -1) {
                downloadUrl += "?quality=" + quality;
            } else {
                downloadUrl += "&quality=" + quality;
            }
        }
        
        // Ajouter format si nécessaire
        if (downloadUrl.indexOf("format=") == -1) {
            downloadUrl += "&format=" + fileFormat;
        }
        
        if (downloadUrl.startsWith("https://")) {
            downloadUrl = "http://" + downloadUrl.substring(8);
        }
        
        return downloadUrl;
    }
    
    private static String encodeUrl(String url) {
        StringBuffer result = new StringBuffer();
        char[] hex = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || 
                (c >= '0' && c <= '9') || c == '-' || c == '_' || 
                c == '.' || c == '~') {
                result.append(c);
            } else {
                result.append('%');
                result.append(hex[(c >> 4) & 0xF]);
                result.append(hex[c & 0xF]);
            }
        }
        return result.toString();
    }
    
    public static String testApis() {
        StringBuffer report = new StringBuffer("Test des APIs:\n\n");
        
        // Test William's Mobile
        try {
            String testUrl = "http://williamsmobile.co.uk";
            HttpUtils.fetch(testUrl);
            report.append("[OK] William's Mobile\n");
        } catch (Exception e) {
            report.append("[ECHEC] William's Mobile: " + e.getMessage() + "\n");
        }
        
        // Test S60Tube
        try {
            String testUrl = "http://s60tube.io.vn";
            HttpUtils.fetch(testUrl);
            report.append("[OK] S60Tube\n");
        } catch (Exception e) {
            report.append("[ECHEC] S60Tube: " + e.getMessage() + "\n");
        }
        
        report.append("\nFormats supportes:\n");
        report.append("VIDEO: MP4, 3GP\n");
        report.append("AUDIO: MP3, AAC, WAV\n");
        report.append("QUALITES: 144p-1080p\n");
        
        return report.toString();
    }
}
