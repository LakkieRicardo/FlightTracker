package lakkie.flight.tracking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

import ca.fuzzlesoft.JsonParse;

public record FR24TrackerResults(int responseCode, List<FR24Aircraft> aircraft) {

    public boolean isValid() {
        return responseCode == 200 && aircraft != null;
    }

    /**
     * Synchronously queries the flight tracker server for the tracked flights at the time this function was called.
     * @param source The server to query with %d being the placeholder for the current time.
     * @return
     */
    public static FR24TrackerResults queryTracker(String source) throws IOException, InterruptedException {
        long currentTimeSec = System.currentTimeMillis() / 1000L;
        String formattedSource = String.format(source, currentTimeSec);
        HttpClient client = HttpClient.newBuilder()
            .version(Version.HTTP_1_1)
            .followRedirects(Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(formattedSource))
            .build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            // Unexpected response code
            throw new IOException(String.format("Query from %s returned unexpected code: %d", formattedSource, response.statusCode()));
        }
        
        String flightList = response.body();
        // We only need the values because the key is the callsign, which is the value at index 0.
        Collection<Object> flights = JsonParse.map(flightList).values();
        List<FR24Aircraft> parsedFlights = new ArrayList<>();

        for (Object flightObject : flights) {
            if (!(flightObject instanceof ArrayList flightProps)) {
                throw new IOException("Got unexpected object type in flight list. Expected: ArrayList, got: " + flightObject.getClass());
            }

            if (!(flightProps.get(0) instanceof String callsign)) {
                throw new IOException("Got unexpected callsign type in flight. Expected: String, got: " + flightProps.get(0).getClass());
            }

            if (!(flightProps.get(1) instanceof Number lat) || !(flightProps.get(2) instanceof Number lng)) {
                throw new IOException(String.format("Got unexpected lat/long type on flight. Expected: number type, number type, got: %s, %s",
                    flightProps.get(1).getClass(),
                    flightProps.get(2).getClass()));
            }

            if (!(flightProps.get(4) instanceof Number altitude)) {
                throw new IOException("Got unexpected callsign type in flight. Expected: Number, got: " + flightProps.get(4).getClass());
            }

            parsedFlights.add(new FR24Aircraft(lat.doubleValue(), lng.doubleValue(), callsign, altitude.doubleValue()));
        }

        return new FR24TrackerResults(response.statusCode(), parsedFlights);
    }

}
