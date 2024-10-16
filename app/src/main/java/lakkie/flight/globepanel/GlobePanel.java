package lakkie.flight.globepanel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import javax.imageio.ImageIO;

import lakkie.flight.globepanel.ProjectionConverter.Point;
import lakkie.flight.tracking.FR24Aircraft;
import lakkie.flight.tracking.FR24TrackerThread;

public class GlobePanel extends JPanel implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener {
    
    private static final float MIN_CAMERA_ZOOM = 0.1f, MAX_CAMERA_ZOOM = 10f;
    private static final float CAMERA_ZOOM_MULTIPLIER = 1.025f;

    private static final Font PLANE_INFO_FONT = new Font("Courier New", Font.BOLD, 6);
    private static final Font DEBUG_FONT = new Font("Courier New", Font.PLAIN, 14);

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

    private boolean drawMapInfo = false;

    public final ProjectionConverter projector;
    private final MapShapeGenerator mapShapeGenerator;
    private final List<MapShapeData> mapShapes;
    public Image mapImage;
    public List<Point> projectionPoints = new ArrayList<>();
    public List<TestPoint> testPointList = new ArrayList<>();
    
    public final Object planeLock = new Object();
    public List<Point> planePositions = new ArrayList<>();
    public List<FR24Aircraft> planeInfo = new ArrayList<>();
    public long planeLastUpdate = System.currentTimeMillis();
    
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

        projector = new ProjectionConverter(20450, 10350, 0, 0);

        new Thread(() -> FR24TrackerThread.trackAircraft(this), "Track Aircraft").start();
        for (double lat = -90; lat <= 90; lat += 10) {
            for (double lng = -180; lng <= 180; lng += 20) {
                projectionPoints.add(projector.projectToScreen(lat, lng));
            }
        }

        TestPoint.loadFromFile(testPointList, GlobePanel.class.getResourceAsStream("/TestPoints.json"), projector);

        try {
            mapImage = ImageIO.read(GlobePanel.class.getResourceAsStream("/Robinson_with_Tissot's_Indicatrices_of_Distortion.png"));
        } catch (IOException e) {
            System.err.println("Failed to load test image!");
            mapImage = null;
            e.printStackTrace();
        }
    }

    private void paintDebug(Graphics g) {
        g.setFont(DEBUG_FONT);
        g.setColor(Color.WHITE);
        g.drawString("World X: " + currentWorldX, 5, 15);
        g.drawString("World Y: " + currentWorldY, 5, 30);
        g.drawString(String.format("Zoom Scalar: %.2f (Reset: press =)", zoomScalar), 5, 45);
        synchronized (planeLock) {
            g.drawString("Tracked flights: " + planeInfo.size(), 5, 60);
            g.drawString(String.format("Flights last updated: %dms ago", (System.currentTimeMillis() - planeLastUpdate)), 5, 75);
        }
        g.drawString(String.format("Showing map overlay: %b (Toggle: press \\)", drawMapInfo), 5, 90);
        if (showMouseCoords) {
            g.setColor(Color.GREEN);
            g.drawString(String.format("(%d, %d)", mouseX, mouseY), mouseX, mouseY);
        }
    }

    private AffineTransform getCameraTransform(AffineTransform originalTransform) {
        // FIXME change camera zoom to middle of screen
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

        g2d.setFont(PLANE_INFO_FONT);

        // Prepare transform for world space
        AffineTransform originalTransform = g2d.getTransform();
        g2d.setTransform(getCameraTransform(originalTransform));

        // Info for debugging map alignment
        if (drawMapInfo && mapImage != null) {
            g2d.drawImage(mapImage, -355, -159, 20430, 10345, null);
            g2d.setColor(Color.BLUE);
            g2d.setStroke(new BasicStroke(5.f));
            for (Point projPoint : projectionPoints) {
                g2d.fillRect((int)projPoint.x() - 10, (int)projPoint.y() - 10, 20, 20);
            }
            TestPoint.render(g2d, testPointList);
        }

        // Draw actual map
        g2d.setColor(Color.PINK);
        g2d.setStroke(new BasicStroke(5.f));

        for (MapShapeData shape : mapShapes) {
            Polygon polygon = shape.getPolygon();
            synchronized (polygon) {
                g2d.drawPolygon(polygon);
            }
        }

        // Draw planes
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(2.f));
        synchronized (planeLock) {
            for (int i = 0; i < planePositions.size(); i++) {
                Point point = planePositions.get(i);
                g2d.fillRect((int)point.x() - 2, (int)point.y() - 2, 4, 4);
                String callsign = planeInfo.get(i).callsign().toUpperCase();
                int callsignWidth = g2d.getFontMetrics().stringWidth(callsign);
                g2d.drawString(callsign, (int)point.x() - callsignWidth / 2, (int)point.y() + 8);
            }
        }

        // Reset transform for UI
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
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            for (MapShapeData mapShape : mapShapes) {
                mapShape.requestNewPoint();
            }
            System.out.println("Draw new point");
            SwingUtilities.invokeLater(this::repaint);
        } else if (e.getKeyChar() == KeyEvent.VK_EQUALS) {
            zoomScalar = 1;
            SwingUtilities.invokeLater(this::repaint);
        } else if (e.getKeyChar() == KeyEvent.VK_BACK_SLASH) {
            drawMapInfo = !drawMapInfo;
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
