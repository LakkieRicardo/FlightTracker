package lakkie.flight;

import org.junit.jupiter.api.Test;

import lakkie.flight.globepanel.MapCountry;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Scanner;

public class MapCountryTests {

    @Test
    void testParseKosovo() throws IOException {
        Scanner scannerKosovo = new Scanner(MapCountryTests.class.getResourceAsStream("/Kosovo.txt"));
        MapCountry polygonKosovo = MapCountry.parsePolygon(scannerKosovo);
        scannerKosovo.close();

        assertEquals(20, polygonKosovo.numPoints);
    }

}
