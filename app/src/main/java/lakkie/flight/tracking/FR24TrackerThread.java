package lakkie.flight.tracking;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lakkie.flight.globepanel.GlobePanel;
import lakkie.flight.globepanel.ProjectionConverter.Point;

public class FR24TrackerThread {
    
    public static void trackAircraft(GlobePanel updateMap) {
        while (true) {
            try {
                List<FR24Aircraft> results = FR24TrackerResults.queryTracker(System.getProperty("FR24_SERVER_URL"));
                List<Point> projectedTrackedFlights = new ArrayList<>();
                for(FR24Aircraft aircraft : results) {
                    projectedTrackedFlights.add(updateMap.projector.projectToScreen1(aircraft.lat(), aircraft.lng()));
                }

                System.out.printf("Fetched %d flights\n", projectedTrackedFlights.size());

                synchronized (updateMap.projectionPoints1) {
                    updateMap.projectionPoints1 = projectedTrackedFlights;
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("Failed to fetch tracked flights!");
                e.printStackTrace();
            }

            try {
                Thread.sleep(5 * 1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
