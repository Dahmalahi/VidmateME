import javax.microedition.media.*;
import javax.microedition.media.control.*;
import javax.microedition.lcdui.*;

public class MediaPlayer {
    private Player player;
    private VideoControl vc;
    private Canvas canvas;
    
    public MediaPlayer(Canvas c) {
        canvas = c;
    }
    
    public boolean play(String filePath) {
        try {
            stop();
            
            player = Manager.createPlayer(StorageManager.openInputStream(filePath), "video/3gpp");
            player.realize();
            
            vc = (VideoControl) player.getControl("VideoControl");
            if (vc != null && canvas instanceof VideoCanvas) {
                ((VideoCanvas)canvas).setVideoControl(vc);
            }
            
            player.prefetch();
            player.start();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public void stop() {
        if (player != null) {
            try {
                player.stop();
                player.deallocate();
                player.close();
            } catch (Exception e) {}
            player = null;
            vc = null;
        }
    }
    
    public void setVolume(int level) {
        if (player != null) {
            VolumeControl vc = (VolumeControl) player.getControl("VolumeControl");
            if (vc != null) vc.setLevel(level);
        }
    }
}