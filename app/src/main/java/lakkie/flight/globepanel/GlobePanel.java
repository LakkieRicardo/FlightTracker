package lakkie.flight.globepanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class GlobePanel extends JPanel implements MouseMotionListener, MouseListener {
    
    /**
     * Mouse X and Y positions recorded when the left mouse button was initially pressed
     */
    private int mouseDownX = 0, mouseDownY = 0;

    /**
     * The position in world space when the mouse was pressed down.
     */
    private int mouseDownWorldX = 0, mouseDownWorldY = 0;

    private int currentWorldX = 0, currentWorldY = 0;

    public GlobePanel() {
        super();
        setMinimumSize(new Dimension(400, 300));
        addMouseListener(this);
        addMouseMotionListener(this);

        setFont(getFont().deriveFont(10.f));
    }

    private void paintDebug(Graphics g) {
        g.drawString("World X: " + currentWorldX, 5, 15);
        g.drawString("World Y: " + currentWorldY, 5, 25);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());

        // Draw foreground
        g.setColor(Color.PINK);
        g.drawRect(this.getWidth() / 4 - currentWorldX, this.getHeight() / 4 - currentWorldY, this.getWidth() / 2, this.getHeight() / 2);

        paintDebug(g);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mouseDownWorldX = currentWorldX;
        mouseDownWorldY = currentWorldY;

        mouseDownX = e.getX();
        mouseDownY = e.getY();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // Amount we've moved since starting mouse drag
        int xDiff = e.getX() - mouseDownX;
        int yDiff = e.getY() - mouseDownY;

        // Add this amount to the start positioning of the mouse drag in world space
        currentWorldX = mouseDownWorldX - xDiff;
        currentWorldY = mouseDownWorldY - yDiff;

        SwingUtilities.invokeLater(this::repaint);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    /**
     * Adds a GlobePanel to a JFrame and packs it. Also registers input listeners.
     */
    public static void addToFrame(JFrame frame) {
        GlobePanel panelGlobe = new GlobePanel();
        frame.add(panelGlobe);
        frame.addMouseListener(panelGlobe);
        frame.addMouseMotionListener(panelGlobe);
        frame.pack();
        frame.setMinimumSize(panelGlobe.getMinimumSize());
    }

}
