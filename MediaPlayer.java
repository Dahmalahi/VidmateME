import javax.microedition.media.*;
import javax.microedition.media.control.*;
import javax.microedition.lcdui.*;
import java.io.InputStream;

public class MediaPlayer {
    private Player player;
    private VideoControl videoControl;
    private VolumeControl volumeControl;
    private Canvas canvas;
    private String currentFilePath = "";
    private boolean isPlaying = false;
    private boolean isPaused = false;
    
    // ✅ NEW: Player state constants
    public static final int STATE_CLOSED = 0;
    public static final int STATE_UNREALIZED = 1;
    public static final int STATE_REALIZED = 2;
    public static final int STATE_PREFETCHED = 3;
    public static final int STATE_STARTED = 4;
    
    public MediaPlayer(Canvas c) {
        canvas = c;
    }
    
    /**
     * ✅ CHANGED: Enhanced play method with better error handling
     */
    public boolean play(String filePath) {
        try {
            // Stop any existing playback
            stop();
            
            currentFilePath = filePath;
            
            // ✅ NEW: Verify file exists before attempting to play
            if (!StorageManager.fileExists(filePath)) {
                throw new Exception("File not found: " + filePath);
            }
            
            // ✅ CHANGED: Get input stream with proper error handling
            InputStream is = null;
            try {
                is = StorageManager.openInputStream(filePath);
            } catch (Exception e) {
                throw new Exception("Cannot open file: " + e.getMessage());
            }
            
            // ✅ CHANGED: Detect MIME type based on file extension
            String mimeType = detectMimeType(filePath);
            
            // Create player
            player = Manager.createPlayer(is, mimeType);
            player.realize();
            
            // Get video control if available
            videoControl = (VideoControl) player.getControl("VideoControl");
            if (videoControl != null && canvas instanceof VideoCanvas) {
                ((VideoCanvas) canvas).setVideoControl(videoControl);
            }
            
            // Get volume control
            volumeControl = (VolumeControl) player.getControl("VolumeControl");
            if (volumeControl != null) {
                volumeControl.setLevel(75); // Default volume
            }
            
            // Prefetch and start
            player.prefetch();
            player.start();
            
            isPlaying = true;
            isPaused = false;
            
            return true;
            
        } catch (Exception e) {
            // ✅ NEW: Cleanup on error
            cleanup();
            
            // ✅ NEW: Show error to user
            if (canvas instanceof PlayerCanvas || canvas instanceof AudioPlayerCanvas) {
                Alert error = new Alert("Erreur Lecture",
                    "Impossible de lire le fichier:\n" + 
                    e.getMessage() + "\n\n" +
                    "Fichier: " + getFileName(filePath) + "\n" +
                    "Stockage: " + StorageManager.getDownloadPath(),
                    null, AlertType.ERROR);
                error.setTimeout(5000);
                // Note: Cannot show alert here without Display reference
                // This should be handled by the calling Canvas
            }
            
            return false;
        }
    }
    
    /**
     * ✅ NEW: Detect MIME type from file extension
     */
    private String detectMimeType(String filePath) {
        String lower = filePath.toLowerCase();
        
        // Video formats
        if (lower.endsWith(".3gp")) {
            return "video/3gpp";
        } else if (lower.endsWith(".mp4")) {
            return "video/mp4";
        } else if (lower.endsWith(".avi")) {
            return "video/x-msvideo";
        }
        
        // Audio formats
        else if (lower.endsWith(".mp3")) {
            return "audio/mpeg";
        } else if (lower.endsWith(".wav")) {
            return "audio/wav";
        } else if (lower.endsWith(".aac")) {
            return "audio/aac";
        } else if (lower.endsWith(".amr")) {
            return "audio/amr";
        }
        
        // Default
        return "video/3gpp";
    }
    
    /**
     * ✅ NEW: Extract filename from path
     */
    private String getFileName(String path) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash != -1 && lastSlash < path.length() - 1) {
            return path.substring(lastSlash + 1);
        }
        return path;
    }
    
    /**
     * ✅ NEW: Pause playback
     */
    public boolean pause() {
        if (player != null && isPlaying) {
            try {
                player.stop();
                isPaused = true;
                isPlaying = false;
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
    
    /**
     * ✅ NEW: Resume playback
     */
    public boolean resume() {
        if (player != null && isPaused) {
            try {
                player.start();
                isPlaying = true;
                isPaused = false;
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
    
    /**
     * ✅ CHANGED: Renamed to cleanup for clarity
     */
    public void stop() {
        cleanup();
    }
    
    /**
     * ✅ NEW: Proper cleanup method
     */
    private void cleanup() {
        isPlaying = false;
        isPaused = false;
        
        if (player != null) {
            try {
                player.stop();
            } catch (Exception e) {}
            
            try {
                player.deallocate();
            } catch (Exception e) {}
            
            try {
                player.close();
            } catch (Exception e) {}
            
            player = null;
        }
        
        videoControl = null;
        volumeControl = null;
        currentFilePath = "";
    }
    
    /**
     * ✅ CHANGED: Enhanced volume control
     */
    public void setVolume(int level) {
        // Clamp level between 0 and 100
        if (level < 0) level = 0;
        if (level > 100) level = 100;
        
        if (volumeControl != null) {
            try {
                volumeControl.setLevel(level);
            } catch (Exception e) {
                // Ignore volume control errors
            }
        }
    }
    
    /**
     * ✅ NEW: Get current volume level
     */
    public int getVolume() {
        if (volumeControl != null) {
            try {
                return volumeControl.getLevel();
            } catch (Exception e) {
                return 75; // Default
            }
        }
        return 75;
    }
    
    /**
     * ✅ NEW: Get media duration in microseconds
     */
    public long getDuration() {
        if (player != null) {
            try {
                return player.getDuration();
            } catch (Exception e) {
                return Player.TIME_UNKNOWN;
            }
        }
        return Player.TIME_UNKNOWN;
    }
    
    /**
     * ✅ NEW: Get current playback time in microseconds
     */
    public long getMediaTime() {
        if (player != null) {
            try {
                return player.getMediaTime();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }
    
    /**
     * ✅ NEW: Seek to specific time (microseconds)
     */
    public boolean setMediaTime(long time) {
        if (player != null) {
            try {
                player.setMediaTime(time);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
    
    /**
     * ✅ NEW: Get player state
     */
    public int getState() {
        if (player == null) {
            return STATE_CLOSED;
        }
        
        try {
            return player.getState();
        } catch (Exception e) {
            return STATE_CLOSED;
        }
    }
    
    /**
     * ✅ NEW: Check if currently playing
     */
    public boolean isPlaying() {
        return isPlaying;
    }
    
    /**
     * ✅ NEW: Check if paused
     */
    public boolean isPaused() {
        return isPaused;
    }
    
    /**
     * ✅ NEW: Get current file path
     */
    public String getCurrentFilePath() {
        return currentFilePath;
    }
    
    /**
     * ✅ NEW: Get video control (if available)
     */
    public VideoControl getVideoControl() {
        return videoControl;
    }
    
    /**
     * ✅ NEW: Get volume control (if available)
     */
    public VolumeControl getVolumeControl() {
        return volumeControl;
    }
    
    /**
     * ✅ NEW: Check if video control is available
     */
    public boolean hasVideoControl() {
        return videoControl != null;
    }
    
    /**
     * ✅ NEW: Check if volume control is available
     */
    public boolean hasVolumeControl() {
        return volumeControl != null;
    }
}