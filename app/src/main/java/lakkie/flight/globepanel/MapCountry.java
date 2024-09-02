package lakkie.flight.globepanel;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Scanner;

public class MapCountry {

    public float x = 0, y = 0;
    public final int numPoints;
    private final float[] xPoints, yPoints;
    private Polygon polygon = null; 

    private MapCountry(float x, float y, int numPoints) {
        this.x = x;
        this.y = y;
        this.numPoints = numPoints;
        this.xPoints = new float[numPoints];
        this.yPoints = new float[numPoints];
    }

    private void updatePolygon() {
        /*
         * These points are imported from an SVG map of the world, so each polygon point is relative to the last point.
         * For example, a line might contain 2 points in the raw data: -1.2 0.3 -2.9 1
         * But to draw as a Java polygon, we have to keep a running position of where we're at and add our current point
         * to that. So we start at (-1.2, 0.3) and the next point is (-1.2 + -2.9, 0.3 + 1)
         */
        final float polygonScale = 100.f;
        int[] absPointsX = new int[numPoints];
        int[] absPointsY = new int[numPoints];
        int currentX = (int) Math.floor(x * polygonScale);
        int currentY = (int) Math.floor(y * polygonScale);
        for (int i = 0; i < numPoints; i++) {
            currentX += (int) Math.floor(polygonScale * xPoints[i]);
            currentY += (int) Math.floor(polygonScale * yPoints[i]);
            absPointsX[i] = currentX;
            absPointsY[i] = currentY;
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

    public static MapCountry parsePolygon(Scanner input) {
        float posX = input.nextFloat();
        float posY = input.nextFloat();
        int numPoints = input.nextInt();

        MapCountry country = new MapCountry(posX, posY, numPoints);

        int pointIdx = 0;
        while (input.hasNext()) {
            float x = input.nextFloat();
            float y = input.nextFloat();
            country.setPoint(pointIdx++, x, y);
        }

        country.updatePolygon();

        return country;
    }

}
