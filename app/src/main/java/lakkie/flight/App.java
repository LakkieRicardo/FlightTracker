package lakkie.flight;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import lakkie.flight.globepanel.GlobePanel;

public class App {
    
    private static void constructFrame() {
        JFrame mapFrame = new JFrame("Flight Tracker");
        mapFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        GlobePanel.addToFrame(mapFrame);
        mapFrame.setVisible(true);
    }    

    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::constructFrame);
    }
}
