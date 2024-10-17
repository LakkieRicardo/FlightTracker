package lakkie.flight.globepanel;

import java.awt.Graphics2D;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;
import java.util.Map;

import ca.fuzzlesoft.JsonParse;
import lakkie.flight.globepanel.ProjectionConverter.Point;

public record TestPoint(String label, int x, int y) {

    public static void loadFromFile(List<TestPoint> testPointList, InputStream file, ProjectionConverter projector) {
        StringBuilder jsonContents = new StringBuilder();
        Scanner scanner = new Scanner(file);

        while (scanner. hasNextLine()) {
            jsonContents.append(scanner.nextLine());
        }
        scanner.close();

        List<Object> jsonPointList = JsonParse.list(jsonContents.toString());

        for (Object jsonPointObj : jsonPointList) {
            if (!(jsonPointObj instanceof Map jsonPoint)) {
                continue;
            }

            String label = (String)jsonPoint.get("Label");
            double lat = (double)jsonPoint.get("Latitude");
            double lng = (double)jsonPoint.get("Longitude");

            Point projectedPoint;
            projectedPoint = projector.projectToScreen(lat, lng);

            testPointList.add(new TestPoint(label, (int)projectedPoint.x(), (int)projectedPoint.y()));
        }
    }

    public static void render(Graphics2D g2d, List<TestPoint> testPointList) {
        for (TestPoint testPoint : testPointList) {
            g2d.drawRect(testPoint.x - 5, testPoint.y - 5, 10, 10);
            g2d.drawString(testPoint.label, testPoint.x + 10, testPoint.y + 10);
        }
    }
    
}
