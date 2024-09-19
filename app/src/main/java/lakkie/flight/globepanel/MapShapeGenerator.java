package lakkie.flight.globepanel;

import java.awt.Polygon;
import java.util.List;

public class MapShapeGenerator {
    
    private final List<MapShapeData> shapes;

    private int maxX = 0, maxY = 0;
    private boolean printedMax = false;

    public MapShapeGenerator(List<MapShapeData> shapes) {
        this.shapes = shapes;
    }

    public void generateNewPoints() {
        while (true) {
            for (MapShapeData shape : shapes) {
                if (shape.relativePoints) {
                    generateNewPointsRelative(shape);
                } else {
                    generateNewPointsAbsolute(shape);
                }
            }

            if (!printedMax) {
                printedMax = true;
                System.out.printf("max x: %d, max y: %d\n", maxX, maxY);
            }
        }
    }

    /**
     * Worker thread that will generate new points when targetPointsGenerate is updated.
     * Will not write to targetPointsGenerate itself.
     */
    private void generateNewPointsRelative(MapShapeData s) {
        // polygon.npoints represents how many points we've generated so far
        if (s.polygon.npoints < s.targetPointsGenerate) {
            // Generate the first point based off offsetX and offsetY
            int startX = (int) Math.floor(s.offsetX * MapShapeData.POLYGON_SCALE);
            int startY = (int) Math.floor(s.offsetY * MapShapeData.POLYGON_SCALE);
            if (s.polygon.npoints == 0) {
                s.polyPointsX[0] = startX;
                s.polyPointsY[0] = startY;
                synchronized (s.polygon) {
                    s.polygon = new Polygon(new int[] { startX }, new int[] { startY }, 1);
                }
            }

            int currentX = s.polyPointsX[s.polygon.npoints - 1];
            int currentY = s.polyPointsY[s.polygon.npoints - 1];

            for (int i = s.polygon.npoints; i < s.targetPointsGenerate && i < s.xPoints.length; i++) {
                currentX += (int) Math.floor(MapShapeData.POLYGON_SCALE * s.xPoints[i]);
                currentY += (int) Math.floor(MapShapeData.POLYGON_SCALE * s.yPoints[i]);
                s.polyPointsX[i] = currentX;
                s.polyPointsY[i] = currentY;
                if (currentX > maxX) {
                    maxX = currentX;
                }
                if (currentY > maxY) {
                    maxY = currentY;
                }
            }

            // We also have to generate the final point, which leads back to the start
            if (s.targetPointsGenerate == s.xPoints.length) {
                s.polyPointsX[s.numPoints] = startX;
                s.polyPointsY[s.numPoints] = startY;
            }

            synchronized (s.polygon) {
                s.polygon = new Polygon(s.polyPointsX, s.polyPointsY, s.targetPointsGenerate);
            }
        }
    }

    private void generateNewPointsAbsolute(MapShapeData s) {
        // polygon.npoints represents how many points we've generated so far
        if (s.polygon.npoints < s.targetPointsGenerate) {
            // Generate the first point based off offsetX and offsetY
            int startX = (int) Math.floor(s.offsetX * MapShapeData.POLYGON_SCALE);
            int startY = (int) Math.floor(s.offsetY * MapShapeData.POLYGON_SCALE);
            if (s.polygon.npoints == 0) {
                s.polyPointsX[0] = startX;
                s.polyPointsY[0] = startY;
                synchronized (s.polygon) {
                    s.polygon = new Polygon(new int[] { startX }, new int[] { startY }, 1);
                }
            }

            int currentX = s.polyPointsX[s.polygon.npoints - 1];
            int currentY = s.polyPointsY[s.polygon.npoints - 1];

            for (int i = s.polygon.npoints; i < s.targetPointsGenerate && i < s.xPoints.length; i++) {
                currentX = (int) Math.floor(MapShapeData.POLYGON_SCALE * s.xPoints[i]);
                currentY = (int) Math.floor(MapShapeData.POLYGON_SCALE * s.yPoints[i]);
                s.polyPointsX[i] = currentX;
                s.polyPointsY[i] = currentY;
            }

            synchronized (s.polygon) {
                s.polygon = new Polygon(s.polyPointsX, s.polyPointsY, s.targetPointsGenerate);
            }
        }
    }

}
