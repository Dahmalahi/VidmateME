import javax.microedition.lcdui.*;
import javax.microedition.media.control.*;
import javax.microedition.media.MediaException;  //Ajout de l'import manquant

public class VideoCanvas extends Canvas {
    private VideoControl vc;
    
    protected void paint(Graphics g) {
        g.setColor(0x000000);
        g.fillRect(0, 0, getWidth(), getHeight());
    }
    
    public void setVideoControl(VideoControl control) {
        vc = control;
        if (vc != null) {
            Item videoItem = (Item) vc.initDisplayMode(VideoControl.USE_GUI_PRIMITIVE, null);
            try {
                vc.setDisplayFullScreen(true);
            } catch (MediaException e) {
                // Ignorer si non supporté
            }
        }
    }
}