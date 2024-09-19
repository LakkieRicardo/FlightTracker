package lakkie.flight.globepanel;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MapShapeData {

    public static final float POLYGON_SCALE = 10.f;

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
    float[] xPoints, yPoints;

    /**
     * The number of points to put into polygon. This should only ever be increased.
     */
    volatile int targetPointsGenerate = 0;

    int[] polyPointsX;
    int[] polyPointsY;

    /**
     * The final polygon to display. Should never be written to outside of the generateNewPoints function.
     */
    Polygon polygon;
    /**
     * Whether all the points in the xPoints and yPoints arrays are relative to each other.
     */
    boolean relativePoints;

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
        if (relativePoints) {
            this.polyPointsX = new int[numPoints + 1];
            this.polyPointsY = new int[numPoints + 1];
        } else {
            this.polyPointsX = new int[numPoints];
            this.polyPointsY = new int[numPoints];
        }

        targetPointsGenerate = numPoints;
    }

    public void requestNewPoint() {
        if (this.relativePoints) {
            if (targetPointsGenerate <= xPoints.length) {
                targetPointsGenerate++;
            }
        } else {
            if (targetPointsGenerate < xPoints.length) {
                targetPointsGenerate++;
            }
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
        }

        return mapShapes;
    }

}
