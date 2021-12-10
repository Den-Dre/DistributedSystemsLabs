package be.kuleuven.distributedsystems.cloud.entities;

import be.kuleuven.distributedsystems.cloud.Application;
import com.google.cloud.firestore.DocumentSnapshot;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public interface ICompany {
     Show getShow(UUID showID);

     List<LocalDateTime> getShowTimes(UUID showId);

     List<Seat> getAvailableSeats(UUID showId, LocalDateTime time);

     Seat getSeat(UUID showId, UUID seatId);

     List<Show> getShows();

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
