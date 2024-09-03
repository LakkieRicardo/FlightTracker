package lakkie.flight.globepanel;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MapShapeData {

    public float x = 0, y = 0;
    public final int numPoints;
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
    }

    private void updatePolygon() {
        // Must be at least 10 if you don't want loss of data because the data goes to tenths place
        final float polygonScale = 10.f;
        int[] absPointsX = new int[numPoints];
        int[] absPointsY = new int[numPoints];
        int currentX = (int) Math.floor(x * polygonScale);
        int currentY = (int) Math.floor(y * polygonScale);
        for (int i = 0; i < numPoints; i++) {
            if (relativePoints) {
                /*
                * For this mode, each point is relative to and builds on the last point.
                * For example, a line might contain 2 points in the raw data: -1.2 0.3 -2.9 1
                * But to draw as a Java polygon, we have to keep a running position of where we're at and add our current point
                * to that. So we start at (-1.2, 0.3) and the next point is (-1.2 + -2.9, 0.3 + 1)
                */
                currentX += (int) Math.floor(polygonScale * xPoints[i]);
                currentY += (int) Math.floor(polygonScale * yPoints[i]);
                absPointsX[i] = currentX;
                absPointsY[i] = currentY;
            } else {
                // Absolute points, treat currentX and currentY as the starting point and add our current point to that
                // We do not modify currentX and currentY
                absPointsX[i] = (int) Math.floor(polygonScale * xPoints[i]) + currentX;
                absPointsY[i] = (int) Math.floor(polygonScale * yPoints[i]) + currentY;
            }
        }

        this.polygon = new Polygon(absPointsX, absPointsY, numPoints);
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

    public static MapShapeData parseRelativePolygon(Scanner input) {
        float posX = input.nextFloat();
        float posY = input.nextFloat();
        int numPoints = input.nextInt();

        MapShapeData country = new MapShapeData(posX, posY, numPoints, true);

        int pointIdx = 0;
        while (input.hasNext()) {
            float x = input.nextFloat();
            float y = input.nextFloat();
            country.setPoint(pointIdx++, x, y);
        }

        country.updatePolygon();

        return country;
    }

    public static List<MapShapeData> parseWorldMapFile(Scanner input) {
        List<MapShapeData> mapShapes = new ArrayList<MapShapeData>();

        while (input.hasNextLine()) {
            String nextShape = input.nextLine();
            Scanner shapeScanner = new Scanner(nextShape);

            List<Float> xPoints = new ArrayList<Float>();
            List<Float> yPoints = new ArrayList<Float>();

            // The first 2 numbers will be interpreted as the starting position and the first point in the shape
            // After that, we will continuously add points
            float startX = shapeScanner.nextFloat();
            float startY = shapeScanner.nextFloat();

            xPoints.add(0f);
            yPoints.add(0f);
            
            while (shapeScanner.hasNext()) {
                float posX = shapeScanner.nextFloat();
                float posY = shapeScanner.nextFloat();

                posX -= startX;
                posY -= startY;

                xPoints.add(posX);
                yPoints.add(posY);
            }

            shapeScanner.close();

            // Move all the data from the lists to a new array
            MapShapeData mapShape = new MapShapeData(startX, startY, xPoints.size(), false);
            mapShape.xPoints = new float[mapShape.numPoints];
            mapShape.yPoints = new float[mapShape.numPoints];
            for (int i = 0; i < mapShape.numPoints; i++) {
                mapShape.setPoint(i, xPoints.get(i), yPoints.get(i));
            }

            mapShape.updatePolygon();

            mapShapes.add(mapShape);
        }

        return mapShapes;
    }

}
