import java.util.Vector;

public class Ytfinder {
    private static final String API_SEARCH_1 = "http://s60tube.io.vn/search?q=";
    private static final String API_SEARCH_2 = "http://williamsmobile.co.uk/ytsearch.php?q=";
    private static final String API_SEARCH_3 = "http://aged-darkness-5be4.granbind20.workers.dev/?url=http://m.youtube.com/results?search_query=";
    
    public static Vector search(String query) throws Exception {
        Vector results = new Vector();
        Exception lastError = null;
        
        // Tentative 1: S60Tube
        try {
            searchS60Tube(query, results);
            if (results.size() > 0) return results;
        } catch (Exception e) {
            lastError = e;
        }
        
        // Tentative 2: William's Mobile
        try {
            searchWilliams(query, results);
            if (results.size() > 0) return results;
        } catch (Exception e) {
            lastError = e;
        }
        
        // Tentative 3: Proxy YouTube Mobile
        try {
            searchYouTubeMobile(query, results);
            if (results.size() > 0) return results;
        } catch (Exception e) {
            lastError = e;
        }
        
        if (results.size() == 0) {
            throw new Exception("Aucune API disponible. Derniere erreur: " + 
                (lastError != null ? lastError.getMessage() : "Inconnue"));
        }
        
        return results;
    }
    
    private static void searchS60Tube(String query, Vector results) throws Exception {
        String encoded = encodeUrl(query);
        String url = API_SEARCH_1 + encoded;
        String html = HttpUtils.fetch(url);
        
        parseS60TubeHtml(html, results);
    }
    
    private static void searchWilliams(String query, Vector results) throws Exception {
        String encoded = encodeUrl(query);
        String url = API_SEARCH_2 + encoded;
        String html = HttpUtils.fetch(url);
        
        parseWilliamsHtml(html, results);
    }
    
    private static void searchYouTubeMobile(String query, Vector results) throws Exception {
        String encoded = encodeUrl(query);
        String url = API_SEARCH_3 + encoded;
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
                    // CORRECTION: Utiliser replaceString au lieu de replace
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
    
    // CORRECTION: Methode replaceString au lieu de replace
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
