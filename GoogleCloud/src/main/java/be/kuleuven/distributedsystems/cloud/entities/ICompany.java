package be.kuleuven.distributedsystems.cloud.entities;

import be.kuleuven.distributedsystems.cloud.Application;
import com.google.cloud.firestore.DocumentSnapshot;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;

import static be.kuleuven.distributedsystems.cloud.Model.getLocalDateTime;

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

    default List<LocalDateTime> getTimesFromSnap(DocumentSnapshot snap) {
         List<HashMap<String, Object>> timesMap = (List<HashMap<String, Object>>) snap.get("times");

         List<LocalDateTime> times = new ArrayList<>();

         for (HashMap<String, Object> timeMap: timesMap) {
             times.add(getTimeFromHashMap(timeMap));
         }
         return times;
    }

    default LocalDateTime getTimeFromHashMap(HashMap<String, Object> timeMap) {
        return getLocalDateTime(timeMap);
    }

    default Seat getSeatFromSnap(DocumentSnapshot snap) {
        String company = snap.get("company").toString();
        UUID showId = mapToUUID(snap.get("showId"));
        UUID seatId = mapToUUID(snap.get("seatId"));
        LocalDateTime time = getTimeFromHashMap((HashMap<String, Object>) snap.get("time"));
        String type = snap.get("type").toString();
        String name = snap.get("name").toString();
        Double price = (Double) snap.get("price");
        return new Seat(company, showId, seatId, time, type,name, price);
    }



     default UUID mapToUUID(Object map) {
        // System.out.println("Received map in mapToUUID: " + map);
        Map<String, Long> castMap = (Map<String, Long>) map;
        // castMap.entrySet().forEach(System.out::println);
        return new UUID(
                castMap.get("mostSignificantBits"),
                castMap.get("leastSignificantBits")
        );
    }
}
