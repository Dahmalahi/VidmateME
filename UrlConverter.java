public class UrlConverter {
    public static String extractVideoId(String input) {
        input = input.trim();
        
        // Cas 1: ID seul (11 caracteres)
        if (input.length() == 11 && isValidVideoId(input)) {
            return input;
        }
        
        // Cas 2: Format youtu.be/ID
        int ytbIndex = input.indexOf("youtu.be/");
        if (ytbIndex != -1) {
            int start = ytbIndex + 9;
            int end = input.indexOf('?', start);
            if (end == -1) end = input.indexOf('&', start);
            if (end == -1) end = input.length();
            String id = input.substring(start, end);
            if (id.length() >= 11) return id.substring(0, 11);
        }
        
        // Cas 3: Format watch?v=ID
        int vIndex = input.indexOf("v=");
        if (vIndex != -1) {
            int start = vIndex + 2;
            int end = input.indexOf('&', start);
            if (end == -1) end = input.indexOf('#', start);
            if (end == -1) end = input.length();
            String id = input.substring(start, end);
            if (id.length() >= 11) return id.substring(0, 11);
        }
        
        // Cas 4: Format /embed/ID
        int embedIndex = input.indexOf("/embed/");
        if (embedIndex != -1) {
            int start = embedIndex + 7;
            int end = input.indexOf('?', start);
            if (end == -1) end = input.length();
            String id = input.substring(start, end);
            if (id.length() >= 11) return id.substring(0, 11);
        }
        
        return null;
    }
    
    private static boolean isValidVideoId(String id) {
        if (id.length() != 11) return false;
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || 
                  (c >= '0' && c <= '9') || c == '-' || c == '_')) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean isYoutubeUrl(String url) {
        url = url.toLowerCase();
        return url.indexOf("youtube") != -1 || url.indexOf("youtu.be") != -1;
    }
}