package lakkie.flight.globepanel;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MapShapeData {

    private static final float POLYGON_SCALE = 10.f;

    /**
     * The offset of all xPoints and yPoints in the shape.
     */
    public float x = 0, y = 0;
    /**
     * The total number of points in the polygon. Always equal to xPoints.length or yPoints.length.
     */
    public final int numPoints;
    /**
     * Points in the polygon. May be relative to each other or absolute positions depending on this.relativePoints
     */
    private float[] xPoints, yPoints;
    
    private Polygon polygon = null;
    /**
     * Whether all the points in the xPoints and yPoints arrays are relative to each other.
     */
    private boolean relativePoints;

    private MapShapeData(float x, float y, int numPoints, boolean relativePoints) {
        this.x = x;
        this.y = y;
        this.numPoints = numPoints;
        this.xPoints = new float[numPoints];
        this.yPoints = new float[numPoints];
        this.relativePoints = relativePoints;
    }

    private void updatePolygonRelative() {
        int[] polyPointsX = new int[numPoints + 1];
        int[] polyPointsY = new int[numPoints + 1];
        // Start at the offset position
        int currentXScaled = (int) Math.floor(this.x * POLYGON_SCALE);
        int currentYScaled = (int) Math.floor(this.y * POLYGON_SCALE);

        for (int i = 0; i < numPoints; i++) {
            currentXScaled += (int) Math.floor(POLYGON_SCALE * xPoints[i]);
            currentYScaled += (int) Math.floor(POLYGON_SCALE * yPoints[i]);
            
            polyPointsX[i] = currentXScaled;
            polyPointsY[i] = currentYScaled;
        }

        polyPointsX[numPoints] = (int) Math.floor(this.x * POLYGON_SCALE);
        polyPointsY[numPoints] = (int) Math.floor(this.y * POLYGON_SCALE);

        this.polygon = new Polygon(polyPointsX, polyPointsY, numPoints);
    }

    private void updatePolygonAbsolute() {
        int[] polyPointsX = new int[numPoints + 1];
        int[] polyPointsY = new int[numPoints + 1];
        int offsetXScaled = (int) Math.floor(this.x * POLYGON_SCALE);
        int offsetYScaled = (int) Math.floor(this.y * POLYGON_SCALE);
        for (int i = 0; i < numPoints; i++) {
            // Absolute points, treat currentX and currentY as the starting point and add our current point to that
            // We do not modify currentX and currentY
            polyPointsX[i] = (int) Math.floor(POLYGON_SCALE * xPoints[i]) + offsetXScaled;
            polyPointsY[i] = (int) Math.floor(POLYGON_SCALE * yPoints[i]) + offsetYScaled;
        }

        this.polygon = new Polygon(polyPointsX, polyPointsY, numPoints);
    }

    private void updatePolygon() {
        if (this.relativePoints) {
            updatePolygonRelative();
        } else {
            updatePolygonAbsolute();
        }
    }

    public Polygon getPolygon() {
        // No points were set
        if (polygon == null) {
            return null;
        }

        Rectangle bounds = polygon.getBounds();
        if (bounds.x != this.x || bounds.y != this.y) {
            updatePolygon();
        }

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

        // Create renderable Polygon object
        mapShape.updatePolygon();

        return mapShape;
    }

    public static List<MapShapeData> parseWorldMapFile(Scanner input) {
        List<MapShapeData> mapShapes = new ArrayList<MapShapeData>();

        while (input.hasNextLine()) {
            String shape = input.nextLine();
            switch (shape.charAt(0)) {
                case '#':
                    // Comment, ignore
                    continue;
                case 'R':
                    mapShapes.add(parsePolygon(shape.substring(1), true));
                    break;
                case 'A':
                    mapShapes.add(parsePolygon(shape.substring(1), false));
                    break;
                default:
                    // Skip this line. Absolute or relative points must be specified
                    continue;
            }

            break;
        }

        return mapShapes;
    }

}
