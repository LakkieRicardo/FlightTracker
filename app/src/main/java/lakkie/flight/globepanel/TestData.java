package lakkie.flight.globepanel;

import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

import lakkie.flight.globepanel.ProjectionConverter.Point;

public class TestData {

    public static void loadFromFile(List<Point> testPointList, InputStream file) {
        Scanner scanner = new Scanner(file);
        scanner.close();
    }
    
}
