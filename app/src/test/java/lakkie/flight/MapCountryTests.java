package lakkie.flight;

import org.junit.jupiter.api.Test;

import lakkie.flight.globepanel.MapShapeData;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Scanner;

public class MapCountryTests {

    @Test
    void testParseKosovo() throws IOException {
        Scanner scannerKosovo = new Scanner(MapCountryTests.class.getResourceAsStream("/Kosovo.txt"));
        MapShapeData polygonKosovo = MapShapeData.parseRelativePolygon(scannerKosovo);
        scannerKosovo.close();

        assertEquals(20, polygonKosovo.numPoints);
    }

}
