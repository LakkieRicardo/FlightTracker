package lakkie.flight.globepanel;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MapShapeData {

    private static final float POLYGON_SCALE = 10.f;

    /**
     * The offset of all xPoints and yPoints in the shape.
     */
    public float offsetX = 0, offsetY = 0;
    /**
     * The total number of points in the polygon. Always equal to xPoints.length or yPoints.length.
     */
    public final int numPoints;
    /**
     * Points in the polygon. May be relative to each other or absolute positions depending on this.relativePoints
     */
    private float[] xPoints, yPoints;

    /**
     * The number of points to put into polygon. This should only ever be increased.
     */
    private volatile int targetPointsGenerate = 0;

    /**
     * The final polygon to display. Should never be written to outside of the generateNewPoints function.
     */
    private Polygon polygon;
    /**
     * Whether all the points in the xPoints and yPoints arrays are relative to each other.
     */
    private boolean relativePoints;

    /**
     * 
     * @param offsetX Starting X
     * @param offsetY Starting y
     * @param numPoints The number of points, including the
     * starting point and excluding the final point that wraps back around to the start.
     */
    private MapShapeData(float offsetX, float offsetY, int numPoints, boolean relativePoints) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.numPoints = numPoints;
        this.xPoints = new float[numPoints];
        this.yPoints = new float[numPoints];
        this.relativePoints = relativePoints;
        this.polygon = new Polygon(new int[] {}, new int[] {}, 0);
    }

    /**
     * Worker thread that will generate new points when targetPointsGenerate is updated.
     * Will not write to targetPointsGenerate itself.
     */
    private void generateNewPointsRelative() {
        int[] polyPointsX = new int[numPoints + 1];
        int[] polyPointsY = new int[numPoints + 1];
        // While we're not done generating this shape
        while (targetPointsGenerate < numPoints) {
            // polygon.npoints represents how many points we've generated so far
            if (polygon.npoints < targetPointsGenerate) {
                // Generate the first point based off offsetX and offsetY
                int startX = (int) Math.floor(this.offsetX * POLYGON_SCALE);
                int startY = (int) Math.floor(this.offsetY * POLYGON_SCALE);
                if (polygon.npoints == 0) {
                    polyPointsX[0] = startX;
                    polyPointsY[0] = startY;
                    synchronized (polygon) {
                        polygon = new Polygon(new int[] { startX }, new int[] { startY }, 1);
                    }
                }

                int currentX = polyPointsX[polygon.npoints - 1];
                int currentY = polyPointsY[polygon.npoints - 1];

                for (int i = polygon.npoints; i < targetPointsGenerate && i < xPoints.length; i++) {
                    currentX += (int) Math.floor(POLYGON_SCALE * xPoints[i]);
                    currentY += (int) Math.floor(POLYGON_SCALE * yPoints[i]);
                    polyPointsX[i] = currentX;
                    polyPointsY[i] = currentY;
                }

                // We also have to generate the final point, which leads back to the start
                if (targetPointsGenerate == xPoints.length) {
                    polyPointsX[numPoints] = startX;
                    polyPointsY[numPoints] = startY;
                }

                synchronized (polygon) {
                    polygon = new Polygon(polyPointsX, polyPointsY, targetPointsGenerate);
                }
            }
        }
    }

    public void requestNewPoint() {
        if (targetPointsGenerate <= xPoints.length) {
            targetPointsGenerate++;
        }
    }

    public Polygon getPolygon() {
        return polygon;
    }

    private void setPoint(int idx, float x, float y) {
        this.xPoints[idx] = x;
        this.yPoints[idx] = y;
    }

    public static MapShapeData parsePolygon(String shape, boolean relativePoints) {
        List<Float> xPoints = new ArrayList<Float>();
        List<Float> yPoints = new ArrayList<Float>();

        String[] shapePoints = shape.split("\\s+");
        if (shapePoints.length < 3) {
            throw new IllegalArgumentException("Map shape must have at least 3 points");
        }

        float startX = Float.parseFloat(shapePoints[0]);
        float startY = Float.parseFloat(shapePoints[1]);
        // The first 2 points in the MapShapeData will be (0, 0) because its x and y properties
        // will be set to the first 2 points we read from input
        xPoints.add(0f);
        yPoints.add(0f);
        
        try{
            // Continuously add points 
            for (int i = 2; i < shapePoints.length; i += 2) {
                float x = Float.parseFloat(shapePoints[i]);
                float y = Float.parseFloat(shapePoints[i + 1]);
                xPoints.add(x);
                yPoints.add(y);
            }
        } catch (Exception e){
            System.err.println(e.getMessage());
        }

        MapShapeData mapShape = new MapShapeData(startX, startY, xPoints.size(), relativePoints);
        // Move all the data from the lists to the array in MapShapeData
        for (int i = 0; i < mapShape.numPoints; i++) {
            mapShape.setPoint(i, xPoints.get(i), yPoints.get(i));
        }

        // Create thread to generate points when requested
        new Thread(mapShape::generateNewPointsRelative, "Generate Points").start();

        return mapShape;
    }

    public static MapShapeData parseWorldMapFile(Scanner input) {
        List<MapShapeData> mapShapes = new ArrayList<MapShapeData>();

        while (input.hasNextLine()) {
            String shape = input.nextLine();
            switch (shape.charAt(0)) {
                case '#':
                    // Comment, ignore
                    continue;
                case 'R':
                    return parsePolygon(shape.substring(1), true);
                    
                case 'A':
                    // mapShapes.add(parsePolygon(shape.substring(1), false));
                    break;
                default:
                    // Skip this line. Absolute or relative points must be specified
                    continue;
            }

            break;
        }

        // return mapShapes;
        return null;
    }

}
