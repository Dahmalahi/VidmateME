import java.util.Vector;

public class Ytfinder {
    // v2.1 UniMedia - Dashtube as primary API
    private static final String API_DASHTUBE = "https://dashtube-api.ndukadavid70.workers.dev/api/yt/search?q=";
    private static final String API_S60TUBE = "http://s60tube.io.vn/search?q=";
    private static final String API_WILLIAMS = "http://williamsmobile.co.uk/ytsearch.php?q=";
    private static final String API_YT_MOBILE_PROXY = "http://aged-darkness-5be4.granbind20.workers.dev/?url=http://m.youtube.com/results?search_query=";
    
    public static Vector search(String query) throws Exception {
        Vector results = new Vector();
        Exception lastError = null;
        
        // Priority 1: Dashtube (NEW - fastest, JSON response)
        try {
            searchDashtube(query, results);
            if (results.size() > 0) {
                return results;
            }
        } catch (Exception e) {
            lastError = e;
            // Continue to fallback
        }
        
        // Priority 2: S60Tube (fallback)
        try {
            searchS60Tube(query, results);
            if (results.size() > 0) {
                return results;
            }
        } catch (Exception e) {
            lastError = e;
        }
        
        // Priority 3: William's Mobile (fallback)
        try {
            searchWilliams(query, results);
            if (results.size() > 0) {
                return results;
            }
        } catch (Exception e) {
            lastError = e;
        }
        
        // Priority 4: YouTube Mobile via Proxy (last resort)
        try {
            searchYouTubeMobileProxy(query, results);
            if (results.size() > 0) {
                return results;
            }
        } catch (Exception e) {
            lastError = e;
        }
        
        // All APIs failed
        if (results.size() == 0) {
            throw new Exception("Aucune API disponible. Derniere erreur: " + 
                (lastError != null ? lastError.getMessage() : "Inconnue"));
        }
        
        return results;
    }
    
    // ========== NEW METHOD: Dashtube JSON API ==========
    private static void searchDashtube(String query, Vector results) throws Exception {
        String encoded = encodeUrl(query);
        String url = API_DASHTUBE + encoded;
        
        // Fetch JSON response
        String jsonResponse = HttpUtils.fetch(url);
        
        // Parse JSON (lightweight J2ME compatible)
        parseDashtubeJson(jsonResponse, results);
        
        if (results.size() == 0) {
            throw new Exception("Dashtube: Aucun resultat");
        }
    }
    
    /**
     * Parses Dashtube API JSON response
     * Expected format (based on common YouTube API scrapers):
     * {
     *   "data": [
     *     {
     *       "id": "dQw4w9WgXcQ",
     *       "title": "Video Title",
     *       "author": "Channel Name",
     *       "duration": "3:45",
     *       "views": "1.2M",
     *       "thumbnail": "https://i.ytimg.com/vi/..."
     *     },
     *     ...
     *   ]
     * }
     * 
     * NOTE: Adjust parsing if actual API format differs!
     */
    private static void parseDashtubeJson(String json, Vector results) throws Exception {
        if (json == null || json.length() < 10) {
            throw new Exception("Reponse JSON vide");
        }
        
        // Find "data" array (or "results" - adjust based on real API)
        int dataIndex = json.indexOf("\"data\"");
        if (dataIndex == -1) {
            dataIndex = json.indexOf("\"results\""); // Alternative key
        }
        if (dataIndex == -1) {
            throw new Exception("Format JSON invalide (pas de 'data')");
        }
        
        // Find opening bracket of array
        int arrayStart = json.indexOf('[', dataIndex);
        if (arrayStart == -1) {
            throw new Exception("Tableau JSON introuvable");
        }
        
        int pos = arrayStart + 1;
        int resultCount = 0;
        
        // Parse each video object
        while (pos < json.length() && resultCount < 15) {
            // Find next object
            int objStart = json.indexOf('{', pos);
            if (objStart == -1) break;
            
            int objEnd = findMatchingBrace(json, objStart);
            if (objEnd == -1) break;
            
            String videoObj = json.substring(objStart, objEnd + 1);
            
            try {
                VideoItem item = new VideoItem();
                
                // Extract video ID (try both "id" and "videoId")
                item.videoId = extractJsonString(videoObj, "id");
                if (item.videoId == null || item.videoId.length() != 11) {
                    item.videoId = extractJsonString(videoObj, "videoId");
                }
                
                // Extract title
                item.title = extractJsonString(videoObj, "title");
                if (item.title == null || item.title.length() == 0) {
                    item.title = "Sans titre";
                }
                item.title = decodeHtml(item.title);
                
                // Extract author/channel
                item.author = extractJsonString(videoObj, "author");
                if (item.author == null || item.author.length() == 0) {
                    item.author = extractJsonString(videoObj, "channel");
                }
                if (item.author == null || item.author.length() == 0) {
                    item.author = "YouTube";
                }
                item.author = decodeHtml(item.author);
                
                // Extract duration
                item.duration = extractJsonString(videoObj, "duration");
                if (item.duration == null || item.duration.length() == 0) {
                    item.duration = "Inconnue";
                }
                
                // Extract thumbnail
                item.thumbnailUrl = extractJsonString(videoObj, "thumbnail");
                if (item.thumbnailUrl == null || item.thumbnailUrl.length() == 0) {
                    // Fallback to standard YouTube thumbnail
                    if (item.videoId != null && item.videoId.length() == 11) {
                        item.thumbnailUrl = "http://i.ytimg.com/vi/" + item.videoId + "/mqdefault.jpg";
                    }
                }
                
                // Validate videoId before adding
                if (item.videoId != null && isValidVideoId(item.videoId)) {
                    results.addElement(item);
                    resultCount++;
                }
                
            } catch (Exception e) {
                // Skip malformed video object
            }
            
            pos = objEnd + 1;
        }
        
        if (results.size() == 0) {
            throw new Exception("Aucune video valide trouvee");
        }
    }
    
    /**
     * Extract string value from JSON object
     * Example: "title":"My Video" → returns "My Video"
     */
    private static String extractJsonString(String json, String key) {
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
        value = replaceString(value, "\\n", "\n");
        
        return value;
    }
    
    /**
     * Find matching closing brace for opening brace at startPos
     */
    private static int findMatchingBrace(String json, int startPos) {
        int depth = 0;
        boolean inString = false;
        
        for (int i = startPos; i < json.length(); i++) {
            char c = json.charAt(i);
            
            // Handle string boundaries
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }
        
        return -1; // No matching brace found
    }
    
    // ========== EXISTING METHODS (kept as fallbacks) ==========
    
    private static void searchS60Tube(String query, Vector results) throws Exception {
        String encoded = encodeUrl(query);
        String url = API_S60TUBE + encoded;
        String html = HttpUtils.fetch(url);
        
        parseS60TubeHtml(html, results);
    }
    
    private static void searchWilliams(String query, Vector results) throws Exception {
        String encoded = encodeUrl(query);
        String url = API_WILLIAMS + encoded;
        String html = HttpUtils.fetch(url);
        
        parseWilliamsHtml(html, results);
    }
    
    // FIXED: Renamed to avoid confusion, uses proper proxy URL
    private static void searchYouTubeMobileProxy(String query, Vector results) throws Exception {
        String encoded = encodeUrl(query);
        String url = API_YT_MOBILE_PROXY + encoded;
        String html = HttpUtils.fetch(url);
        
        parseYouTubeMobileHtml(html, results);
    }
    
    public static Vector searchById(String videoId) throws Exception {
        Vector results = new Vector();
        String thumbnailUrl = "http://i.ytimg.com/vi/" + videoId + "/mqdefault.jpg";
        
        try {
            String url = "http://s60tube.io.vn/video/" + videoId;
            String html = HttpUtils.fetch(url);
            parseVideoPage(html, results, videoId, thumbnailUrl);
            if (results.size() > 0) return results;
        } catch (Exception e) {
            // Ignorer
        }
        
        VideoItem item = new VideoItem();
        item.videoId = videoId;
        item.title = "Video " + videoId;
        item.author = "YouTube";
        item.duration = "Inconnue";
        item.thumbnailUrl = thumbnailUrl;
        results.addElement(item);
        
        return results;
    }
    
    // ========== HTML PARSERS (unchanged) ==========
    
    private static void parseS60TubeHtml(String html, Vector results) {
        String linkPattern = "href=\"/video/";
        int index = 0;
        
        while ((index = html.indexOf(linkPattern, index)) != -1 && results.size() < 15) {
            index += linkPattern.length();
            int endQuote = html.indexOf("\"", index);
            if (endQuote == -1) break;
            
            String videoId = html.substring(index, endQuote);
            
            if (videoId.length() != 11 || !isValidVideoId(videoId)) {
                index = endQuote;
                continue;
            }
            
            String thumbnailUrl = "http://i.ytimg.com/vi/" + videoId + "/mqdefault.jpg";
            
            int closingTag = html.indexOf("</a>", endQuote);
            if (closingTag != -1) {
                int textStart = html.lastIndexOf('>', closingTag) + 1;
                if (textStart > endQuote && textStart < closingTag) {
                    String title = html.substring(textStart, closingTag).trim();
                    title = decodeHtml(title);
                    
                    if (title.length() > 0 && !title.equalsIgnoreCase("Video") && 
                        !title.equalsIgnoreCase("S60Tube") && title.indexOf("<") == -1) {
                        
                        VideoItem item = new VideoItem();
                        item.videoId = videoId;
                        item.title = title;
                        item.author = "YouTube";
                        item.duration = "Inconnue";
                        item.thumbnailUrl = thumbnailUrl;
                        results.addElement(item);
                    }
                }
            }
            index = endQuote;
        }
    }
    
    private static void parseWilliamsHtml(String html, Vector results) {
        String pattern = "watch?v=";
        int index = 0;
        
        while ((index = html.indexOf(pattern, index)) != -1 && results.size() < 15) {
            index += pattern.length();
            
            if (index + 11 > html.length()) break;
            String videoId = html.substring(index, index + 11);
            
            if (!isValidVideoId(videoId)) {
                index++;
                continue;
            }
            
            int titleStart = html.lastIndexOf('>', index) + 1;
            int titleEnd = html.indexOf('<', titleStart);
            
            if (titleStart > 0 && titleEnd > titleStart) {
                String title = html.substring(titleStart, titleEnd).trim();
                title = decodeHtml(title);
                
                if (title.length() > 0) {
                    VideoItem item = new VideoItem();
                    item.videoId = videoId;
                    item.title = title;
                    item.author = "YouTube";
                    item.duration = "Inconnue";
                    item.thumbnailUrl = "http://i.ytimg.com/vi/" + videoId + "/mqdefault.jpg";
                    results.addElement(item);
                }
            }
            
            index += 11;
        }
    }
    
    private static void parseYouTubeMobileHtml(String html, Vector results) {
        String pattern = "/watch?v=";
        int index = 0;
        
        while ((index = html.indexOf(pattern, index)) != -1 && results.size() < 15) {
            index += pattern.length();
            
            if (index + 11 > html.length()) break;
            String videoId = html.substring(index, Math.min(index + 11, html.length()));
            
            int endPos = 0;
            for (int i = 0; i < videoId.length(); i++) {
                char c = videoId.charAt(i);
                if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || 
                    (c >= '0' && c <= '9') || c == '-' || c == '_') {
                    endPos = i + 1;
                } else {
                    break;
                }
            }
            
            videoId = videoId.substring(0, Math.min(endPos, 11));
            
            if (videoId.length() == 11 && isValidVideoId(videoId)) {
                VideoItem item = new VideoItem();
                item.videoId = videoId;
                item.title = "Video " + videoId;
                item.author = "YouTube";
                item.duration = "Inconnue";
                item.thumbnailUrl = "http://i.ytimg.com/vi/" + videoId + "/mqdefault.jpg";
                results.addElement(item);
            }
            
            index += 11;
        }
    }
    
    private static void parseVideoPage(String html, Vector results, String videoId, String thumbnailUrl) {
        String title = "Video " + videoId;
        
        int titlePos = html.indexOf("<title>");
        if (titlePos != -1) {
            int tStart = titlePos + 7;
            int tEnd = html.indexOf("</title>", tStart);
            if (tEnd != -1) {
                title = html.substring(tStart, tEnd).trim();
                
                if (title.indexOf(" - S60Tube") != -1) {
                    title = title.substring(0, title.indexOf(" - S60Tube")).trim();
                }
                if (title.indexOf(" - YouTube") != -1) {
                    title = title.substring(0, title.indexOf(" - YouTube")).trim();
                }
                
                title = decodeHtml(title);
            }
        }
        
        String duration = "Inconnue";
        int durPos = html.indexOf("duration");
        if (durPos != -1) {
            int durStart = html.indexOf(":", durPos) + 1;
            int durEnd = html.indexOf(",", durStart);
            if (durEnd == -1) durEnd = html.indexOf("}", durStart);
            if (durStart > 0 && durEnd > durStart) {
                try {
                    String durStr = html.substring(durStart, durEnd).trim();
                    durStr = replaceString(durStr, "\"", "");
                    durStr = replaceString(durStr, "'", "");
                    duration = durStr;
                } catch (Exception e) {
                    // Garder "Inconnue"
                }
            }
        }
        
        VideoItem item = new VideoItem();
        item.videoId = videoId;
        item.title = title;
        item.author = "YouTube";
        item.duration = duration;
        item.thumbnailUrl = thumbnailUrl;
        results.addElement(item);
    }
    
    // ========== UTILITY METHODS (unchanged) ==========
    
    private static boolean isValidVideoId(String id) {
        if (id == null || id.length() != 11) return false;
        
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || 
                  (c >= '0' && c <= '9') || c == '-' || c == '_')) {
                return false;
            }
        }
        return true;
    }
    
    private static String decodeHtml(String str) {
        if (str == null) return "";
        str = replaceString(str, "&amp;", "&");
        str = replaceString(str, "&quot;", "\"");
        str = replaceString(str, "&#39;", "'");
        str = replaceString(str, "&apos;", "'");
        str = replaceString(str, "&nbsp;", " ");
        str = replaceString(str, "&lt;", "<");
        str = replaceString(str, "&gt;", ">");
        str = replaceString(str, "\\u0026", "&");
        str = replaceString(str, "\\\"", "\"");
        str = replaceString(str, "\\/", "/");
        return str;
    }
    
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
    
    private static String encodeUrl(String s) {
        StringBuffer out = new StringBuffer();
        char[] hex = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ') {
                out.append('+');
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || 
                       (c >= '0' && c <= '9') || c == '-' || c == '_' || 
                       c == '.' || c == '~') {
                out.append(c);
            } else {
                out.append('%');
                out.append(hex[(c >> 4) & 0xF]);
                out.append(hex[c & 0xF]);
            }
        }
        return out.toString();
    }
}