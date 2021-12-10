package be.kuleuven.distributedsystems.cloud.entities;

import be.kuleuven.distributedsystems.cloud.Application;
import com.google.cloud.firestore.DocumentSnapshot;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public interface ICompany {
     Show getShow(UUID showID, WebClient.Builder builder);

     List<LocalDateTime> getShowTimes(UUID showId, WebClient.Builder builder);

     List<Seat> getAvailableSeats(UUID showId, LocalDateTime time, WebClient.Builder builder);

     Seat getSeat(UUID showId, UUID seatId, WebClient.Builder builder);

     List<Show> getShows(WebClient.Builder builder);

     default Show getShowFromSnap(DocumentSnapshot snap) {
        String showName = snap.get("name").toString();
        String location = snap.get("location").toString();
        String image = snap.get("image").toString();
        UUID showId = mapToUUID(snap.get("showId"));
        return new Show(Application.localCompanyName, showId, showName, location, image);
    }

     default UUID mapToUUID(Object map) {
        // System.out.println("Received map in mapToUUID: " + map);
        Map<String, Long> castMap = (Map<String, Long>) map;
        castMap.entrySet().forEach(System.out::println);
        return new UUID(
                castMap.get("mostSignificantBits"),
                castMap.get("leastSignificantBits")
        );
    }
}
