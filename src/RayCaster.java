import javax.swing.JFrame;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class RayCaster extends JFrame {

    public static void main( final String[] args ){
        RayCaster window = new RayCaster();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setPreferredSize(new Dimension(800, 600));
        window.setVisible(true);
        window.pack();
        window.init();
    }

    public void init() {
        Container pane = this.getContentPane();
        RayPanel panel = new RayPanel(pane.getWidth(), pane.getHeight());

        pane.add(panel, BorderLayout.CENTER);
        this.addKeyListener(panel);
        this.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent evt) {
                panel.updateSize(pane.getWidth(), pane.getHeight());
            }
        });

        panel.start();
    }
}