public class APIManager {
    // ✅ NEW: Dashtube API endpoint
    private static final String DASHTUBE_BASE = "http://dashtube-api.ndukadavid70.workers.dev/api/yt/download?id=";
    private static final String WILLIAMS_BASE = "http://williamsmobile.co.uk/yt.php?url=";
    private static final String S60TUBE_BASE = "http://s60tube.io.vn/video/";
    
    /**
     * ✅ CHANGED: New priority order - Dashtube → Williams → S60Tube
     * Nouvelle signature avec support du format
     */
    public static String getDownloadUrl(String videoId, String quality, boolean audioOnly, String fileFormat) throws Exception {
        Exception lastError = null;
        
        // ✅ NEW: Priority 1 - Try Dashtube API first
        try {
            return getDashtubeUrl(videoId, quality, audioOnly, fileFormat);
        } catch (Exception e) {
            lastError = e;
            // Continue to fallback
        }
        
        // Priority 2 - Try William's Mobile
        try {
            return getWilliamsUrl(videoId, quality, audioOnly, fileFormat);
        } catch (Exception e) {
            lastError = e;
        }
        
        // Priority 3 - Fallback to S60Tube
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
    
    /**
     * ✅ NEW: Get download URL from Dashtube API
     * Expected response format:
     * {
     *   "downloadUrl": "http://...",
     *   "quality": "360p",
     *   "format": "mp4"
     * }
     */
    private static String getDashtubeUrl(String videoId, String quality, boolean audioOnly, String fileFormat) throws Exception {
        // Build API URL with parameters
        StringBuffer urlBuilder = new StringBuffer(DASHTUBE_BASE);
        urlBuilder.append(videoId);
        urlBuilder.append("&quality=").append(quality);
        
        if (audioOnly) {
            urlBuilder.append("&type=audio");
            urlBuilder.append("&format=").append(fileFormat); // mp3, aac, wav
        } else {
            urlBuilder.append("&type=video");
            urlBuilder.append("&format=").append(fileFormat); // mp4, 3gp
        }
        
        String apiUrl = urlBuilder.toString();
        
        // Fetch JSON response
        String jsonResponse = HttpUtils.fetch(apiUrl);
        
        // Parse JSON to extract download URL
        String downloadUrl = extractJsonString(jsonResponse, "downloadUrl");
        
        if (downloadUrl == null || downloadUrl.length() == 0) {
            // Try alternative field names
            downloadUrl = extractJsonString(jsonResponse, "url");
            if (downloadUrl == null || downloadUrl.length() == 0) {
                downloadUrl = extractJsonString(jsonResponse, "link");
            }
        }
        
        if (downloadUrl == null || downloadUrl.length() == 0) {
            throw new Exception("Dashtube: URL de telechargement introuvable dans la reponse");
        }
        
        // Validate URL
        if (!downloadUrl.startsWith("http://") && !downloadUrl.startsWith("https://")) {
            throw new Exception("Dashtube: URL invalide");
        }
        
        return downloadUrl;
    }
    
    /**
     * ✅ CHANGED: Enhanced error messages
     */
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
                // 3GP optimal for low quality
                if (fileFormat.equals("3gp")) {
                    videoFormat = "3gp";
                }
            }
            if (!videoFormat.equals("mp4") && !videoFormat.equals("3gp")) {
                videoFormat = "mp4"; // Par défaut
            }
            return WILLIAMS_BASE + encoded + "&format=" + videoFormat + "&quality=" + quality + "p";
        }
    }
    
    /**
     * ✅ CHANGED: More robust URL extraction
     */
    private static String getS60TubeUrl(String videoId, String quality, String fileFormat) throws Exception {
        String pageUrl = S60TUBE_BASE + videoId;
        String page = HttpUtils.fetch(pageUrl);
        
        // Look for video playback URL
        String pattern = "videoplayback?v=" + videoId;
        int pos = page.indexOf(pattern);
        
        if (pos == -1) {
            // Try alternative patterns
            pattern = "download?v=" + videoId;
            pos = page.indexOf(pattern);
        }
        
        if (pos == -1) {
            throw new Exception("S60Tube: Lien de telechargement non trouve");
        }
        
        // Find start of URL
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
            throw new Exception("S60Tube: Debut URL non trouve");
        }
        
        // Find end of URL
        int end = page.indexOf("\"", pos);
        if (end == -1) end = page.indexOf("'", pos);
        if (end == -1) end = page.indexOf(" ", pos);
        if (end == -1 || end < pos) {
            throw new Exception("S60Tube: Fin URL non trouvee");
        }
        
        String downloadUrl = page.substring(start, end);
        
        // Add quality parameter if missing
        if (downloadUrl.indexOf("quality=") == -1) {
            if (downloadUrl.indexOf("?") == -1) {
                downloadUrl += "?quality=" + quality;
            } else {
                downloadUrl += "&quality=" + quality;
            }
        }
        
        // Add format parameter if missing
        if (downloadUrl.indexOf("format=") == -1) {
            downloadUrl += "&format=" + fileFormat;
        }
        
        // Force HTTP for J2ME compatibility
        if (downloadUrl.startsWith("https://")) {
            downloadUrl = "http://" + downloadUrl.substring(8);
        }
        
        return downloadUrl;
    }
    
    /**
     * ✅ NEW: Extract string value from JSON (same as in Ytfinder)
     */
    private static String extractJsonString(String json, String key) {
        if (json == null || key == null) return null;
        
        String searchPattern = "\"" + key + "\"";
        int keyPos = json.indexOf(searchPattern);
        if (keyPos == -1) return null;
        
        // Find the colon after the key
        int colonPos = json.indexOf(':', keyPos);
        if (colonPos == -1) return null;
        
        // Skip whitespace and find opening quote
        int valueStart = -1;
        for (int i = colonPos + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') {
                valueStart = i + 1;
                break;
            } else if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                // Value is not a string (maybe number/boolean)
                return null;
            }
        }
        
        if (valueStart == -1) return null;
        
        // Find closing quote (handle escaped quotes)
        int valueEnd = -1;
        for (int i = valueStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"' && (i == valueStart || json.charAt(i - 1) != '\\')) {
                valueEnd = i;
                break;
            }
        }
        
        if (valueEnd == -1) return null;
        
        String value = json.substring(valueStart, valueEnd);
        
        // Unescape basic JSON escapes
        value = replaceString(value, "\\\"", "\"");
        value = replaceString(value, "\\\\", "\\");
        value = replaceString(value, "\\/", "/");
        
        return value;
    }
    
    /**
     * ✅ NEW: String replacement helper (J2ME compatible)
     */
    private static String replaceString(String str, String pattern, String replacement) {
        if (str == null || pattern == null || replacement == null) return str;
        
        StringBuffer result = new StringBuffer();
        int s = 0, e;
        
        while ((e = str.indexOf(pattern, s)) >= 0) {
            result.append(str.substring(s, e));
            result.append(replacement);
            s = e + pattern.length();
        }
        result.append(str.substring(s));
        
        return result.toString();
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
    
    /**
     * ✅ CHANGED: Enhanced API diagnostic with Dashtube
     */
    public static String testApis() {
        StringBuffer report = new StringBuffer("=== TEST DES APIs ===\n\n");
        
        // ✅ NEW: Test Dashtube API
        report.append("1. DASHTUBE API (Primary)\n");
        try {
            String testUrl = "https://dashtube-api.ndukadavid70.workers.dev";
            HttpUtils.fetch(testUrl);
            report.append("   Status: [OK] Disponible\n");
            report.append("   Type: JSON API\n");
            report.append("   Speed: Rapide\n\n");
        } catch (Exception e) {
            report.append("   Status: [ECHEC]\n");
            report.append("   Erreur: ").append(e.getMessage()).append("\n\n");
        }
        
        // Test William's Mobile
        report.append("2. WILLIAM'S MOBILE (Fallback 1)\n");
        try {
            String testUrl = "http://williamsmobile.co.uk";
            HttpUtils.fetch(testUrl);
            report.append("   Status: [OK] Disponible\n");
            report.append("   Type: Direct download\n\n");
        } catch (Exception e) {
            report.append("   Status: [ECHEC]\n");
            report.append("   Erreur: ").append(e.getMessage()).append("\n\n");
        }
        
        // Test S60Tube
        report.append("3. S60TUBE (Fallback 2)\n");
        try {
            String testUrl = "http://s60tube.io.vn";
            HttpUtils.fetch(testUrl);
            report.append("   Status: [OK] Disponible\n");
            report.append("   Type: HTML scraping\n\n");
        } catch (Exception e) {
            report.append("   Status: [ECHEC]\n");
            report.append("   Erreur: ").append(e.getMessage()).append("\n\n");
        }
        
        report.append("=========================\n\n");
        report.append("FORMATS SUPPORTES:\n");
        report.append("VIDEO: MP4, 3GP\n");
        report.append("AUDIO: MP3, AAC, WAV\n\n");
        report.append("QUALITES SUPPORTEES:\n");
        report.append("144p, 240p, 360p, 480p,\n");
        report.append("720p, 1080p\n\n");
        report.append("PRIORITE:\n");
        report.append("1. Dashtube (plus rapide)\n");
        report.append("2. Williams (fiable)\n");
        report.append("3. S60Tube (backup)\n");
        
        return report.toString();
    }
    
    /**
     * ✅ NEW: Test specific API
     */
    public static boolean testApi(String apiName) {
        try {
            if (apiName.equals("Dashtube")) {
                HttpUtils.fetch("https://dashtube-api.ndukadavid70.workers.dev");
                return true;
            } else if (apiName.equals("Williams")) {
                HttpUtils.fetch("http://williamsmobile.co.uk");
                return true;
            } else if (apiName.equals("S60Tube")) {
                HttpUtils.fetch("http://s60tube.io.vn");
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * ✅ NEW: Get API status summary
     */
    public static String getApiStatus() {
        StringBuffer status = new StringBuffer();
        
        status.append("Dashtube: ");
        status.append(testApi("Dashtube") ? "OK" : "DOWN");
        status.append("\n");
        
        status.append("Williams: ");
        status.append(testApi("Williams") ? "OK" : "DOWN");
        status.append("\n");
        
        status.append("S60Tube: ");
        status.append(testApi("S60Tube") ? "OK" : "DOWN");
        
        return status.toString();
    }
}