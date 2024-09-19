package lakkie.flight.globepanel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import lakkie.flight.globepanel.ProjectionConverter.Point;

public class GlobePanel extends JPanel implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener {
    
    private static final float MIN_CAMERA_ZOOM = 0.1f, MAX_CAMERA_ZOOM = 10f;
    private static final float CAMERA_ZOOM_MULTIPLIER = 1.025f;

    /**
     * Mouse X and Y positions recorded when the left mouse button was initially pressed
     */
    private int mouseDownX = 0, mouseDownY = 0;

    private boolean isDragMouseButton1 = false;

    /**
     * The position in world space when the mouse was pressed down.
     */
    private int mouseDownWorldX = 0, mouseDownWorldY = 0;

    /**
     * The current position of the camera in world space.
     * 
     * Position 0, 0 is the center of the screen, and objects in it are sized in pixels * zoom.
     * 
     * 
     */
    private int currentWorldX = 0, currentWorldY = 0;

    /**
     * Used as a debugging tool
     */
    private int mouseX = 0, mouseY = 0;

    /**
     * Set to true when ctrl is pressed
     */
    private boolean showMouseCoords = false;
    
    /**
     * Scalar for the level of zoom.
     */
    private float zoomScalar = 1;

    private final List<MapShapeData> mapShapes;
    private final MapShapeGenerator mapShapeGenerator;
    private final ProjectionConverter projector;
    private final Point testPoint;

    public GlobePanel() {
        super();
        setMinimumSize(new Dimension(400, 300));
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        addKeyListener(this);
        setBackground(Color.BLACK);

        // Reuse default font but set size to 10.pt
        setFont(getFont().deriveFont(10.f));

        Scanner scannerWorldMap = new Scanner(GlobePanel.class.getResourceAsStream("/World.txt"));
        mapShapes = MapShapeData.parseWorldMapFile(scannerWorldMap);
        scannerWorldMap.close();

        mapShapeGenerator = new MapShapeGenerator(mapShapes);
        new Thread(mapShapeGenerator::generateNewPoints, "Generate Shapes").start();

        projector = new ProjectionConverter(2000, 857, 0, 0);
        testPoint = projector.projectToScreen(9.477428703979816, -62.9371994835163);
        System.out.println(testPoint);
    }

    private void paintDebug(Graphics g) {
        g.drawString("World X: " + currentWorldX, 5, 15);
        g.drawString("World Y: " + currentWorldY, 5, 25);
        g.drawString("Zoom Scalar " + zoomScalar, 5, 35);
        if (showMouseCoords) {
            g.drawString(String.format("(%d, %d)", mouseX, mouseY), mouseX, mouseY);
        }
    }

    private AffineTransform getCameraTransform(AffineTransform originalTransform) {
        // Move to the center of the image
        AffineTransform cameraTransform = AffineTransform.getTranslateInstance(getWidth() / 2, getHeight() / 2);
        cameraTransform.concatenate(AffineTransform.getScaleInstance(zoomScalar, zoomScalar));
        cameraTransform.concatenate(AffineTransform.getTranslateInstance(-currentWorldX, -currentWorldY));
        // Move back to original position
        cameraTransform.concatenate(AffineTransform.getTranslateInstance(-getWidth() / 2, -getHeight() / 2));
        return cameraTransform;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (!(g instanceof Graphics2D g2d)) {
            System.err.println("Could not get Graphics as an instance of Graphics2D! Cannot render!");
            return;
        }

        AffineTransform originalTransform = g2d.getTransform();
        g2d.setTransform(getCameraTransform(originalTransform));

        // Draw foreground
        g2d.setColor(Color.PINK);
        g2d.setStroke(new BasicStroke(5.f));

        for (MapShapeData shape : mapShapes) {
            Polygon polygon = shape.getPolygon();
            synchronized (polygon) {
                g2d.drawPolygon(polygon);
            }
        }

        g2d.setColor(Color.BLUE);
        g2d.drawRect((int)testPoint.x(), (int)testPoint.y(), 50, 50);

        g2d.setTransform(originalTransform);

        paintDebug(g2d);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) {
            return;
        }

        isDragMouseButton1 = true;

        mouseDownWorldX = currentWorldX;
        mouseDownWorldY = currentWorldY;

        mouseDownX = e.getX();
        mouseDownY = e.getY();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) {
            return;
        }

        isDragMouseButton1 = true;
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
        mouseX = e.getX();
        mouseY = e.getY();
        if (isDragMouseButton1) {
            // Amount we've moved since starting mouse drag
            int xDiff = e.getX() - mouseDownX;
            int yDiff = e.getY() - mouseDownY;

            // FIXME where is this 2/3 speed coming from?
            // Update for zoom
            xDiff = (int) Math.floor(xDiff / zoomScalar * 1.5f);
            yDiff = (int) Math.floor(yDiff / zoomScalar * 1.5f);

            // Add this amount to the start positioning of the mouse drag in world space
            currentWorldX = mouseDownWorldX - xDiff;
            currentWorldY = mouseDownWorldY - yDiff;
        }

        SwingUtilities.invokeLater(this::repaint);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();

        SwingUtilities.invokeLater(this::repaint);
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        // TODO fix touchpad scroll
        if (e.getUnitsToScroll() < 0) {
            zoomScalar *= CAMERA_ZOOM_MULTIPLIER;
        } else {
            zoomScalar /= CAMERA_ZOOM_MULTIPLIER;
        }
        // Clamp to min and max
        zoomScalar = Math.max(MIN_CAMERA_ZOOM, zoomScalar);
        zoomScalar = Math.min(MAX_CAMERA_ZOOM, zoomScalar);

        SwingUtilities.invokeLater(this::repaint);
    }

    @Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
            showMouseCoords = true;
            SwingUtilities.invokeLater(this::repaint);
        }
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            for (MapShapeData mapShape : mapShapes) {
                mapShape.requestNewPoint();
            }
            System.out.println("Draw new point");
            SwingUtilities.invokeLater(this::repaint);
        }
	}

	@Override
	public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
            showMouseCoords = false;
            SwingUtilities.invokeLater(this::repaint);
        }
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

        frame.addMouseListener(panelGlobe);
        frame.addMouseMotionListener(panelGlobe);
        frame.addMouseWheelListener(panelGlobe);
        frame.addKeyListener(panelGlobe);
    }

}
