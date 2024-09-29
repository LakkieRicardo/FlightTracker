package lakkie.flight;

import org.junit.jupiter.api.Test;

import lakkie.flight.globepanel.MapShapeData;
import lakkie.flight.tracking.FR24Aircraft;
import lakkie.flight.tracking.FR24TrackerResults;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Scanner;

public class TestTrackerResults {

    @Test
    void testQueryResults() throws IOException, InterruptedException {
        FR24TrackerResults.queryTracker(System.getProperty("FR24_SERVER_URL"));
    }

}