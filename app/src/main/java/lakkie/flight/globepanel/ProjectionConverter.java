package lakkie.flight.globepanel;

// Code copied from https://github.com/afar/robinson_projection/blob/master/robinson.js

public class ProjectionConverter {

    // public static final int CANVAS_OFFSET_X = -275, CANVAS_OFFSET_Y = 690;
    public static final int CANVAS_OFFSET_X = 0, CANVAS_OFFSET_Y = 0;

    private double mapWidth;
    private double mapHeight;
    private double earthRadius;
    private double offsetX;
    private double offsetY;

    private static final double[] AA = { 0.8487, 0.84751182, 0.84479598, 0.840213, 0.83359314, 0.8257851, 0.814752,
            0.80006949, 0.78216192, 0.76060494, 0.73658673, 0.7086645, 0.67777182, 0.64475739, 0.60987582, 0.57134484,
            0.52729731, 0.48562614, 0.45167814 };

    private static final double[] BB = { 0, 0.0838426, 0.1676852, 0.2515278, 0.3353704, 0.419213, 0.5030556, 0.5868982,
            0.67182264, 0.75336633, 0.83518048, 0.91537187, 0.99339958, 1.06872269, 1.14066505, 1.20841528, 1.27035062,
            1.31998003, 1.3523 };

    public ProjectionConverter(double mapWidth, double mapHeight, double offsetX, double offsetY) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.earthRadius = (mapWidth / 2.666269758) / 2;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public Point projectToScreen(double lat, double lng) {
        lat = Math.min(90, lat);
        lat = Math.max(-90, lat);

        Point point = project(lat, lng);
        double canvasX = point.x + (mapWidth / 2);
        double canvasY = (mapHeight / 2) - point.y;
        return new Point(canvasX, canvasY + 750);
    }

    public Point projectToScreenNew(double lat, double lng) {
        lat *= 1.1D;

        lat = Math.min(90, lat);
        lat = Math.max(-90, lat);

        Point point = project(lat, lng);
        double canvasX = point.x + (mapWidth / 2);
        double canvasY = (mapHeight / 2) - point.y;
        return new Point(canvasX, canvasY + 750);
    }

    public Point project(double lat, double lng) {
        double lngSign = Math.signum(lng);
        double latSign = Math.signum(lat);
        lng = Math.abs(lng);
        lat = Math.abs(lat);
        final double radian = 0.017453293; // pi / 180

        double low = roundToNearest(5, lat - 0.0000000001);
        low = (lat == 0) ? 0 : low;
        double high = low + 5;

        int lowIndex = (int) (low / 5);
        int highIndex = (int) (high / 5);
        double ratio = (lat - low) / 5;

        double adjAA = ((AA[highIndex] - AA[lowIndex]) * ratio) + AA[lowIndex];
        double adjBB = ((BB[highIndex] - BB[lowIndex]) * ratio) + BB[lowIndex];

        double x = (adjAA * lng * radian * lngSign * earthRadius) + offsetX;
        double y = (adjBB * latSign * earthRadius) + offsetY;

        return new Point(x, y);
    }

    private double roundToNearest(double roundTo, double value) {
        return Math.floor(value / roundTo) * roundTo;
    }

    public record Point(double x, double y) {
        @Override
        public final String toString() {
            return "(" + x + ", " + y + ")";
        }
    }
}
