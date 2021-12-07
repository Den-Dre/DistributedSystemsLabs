package be.kuleuven.distributedsystems.cloud.entities;

import be.kuleuven.distributedsystems.cloud.Application;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Component
public class LocalCompany implements ICompany {
    // @Resource(name = "db")
    private Firestore db;

    public LocalCompany(Firestore db) {
        this.db = db;
    }

    public List<Show> getShows() {
        List<Show> allShows = new ArrayList<>();
        try {
            System.out.println("DB: " + db);
            System.out.println("collection: " + db.collection(Application.localShowCollectionName));
            System.out.println("collectionget " + db.collection(Application.localShowCollectionName).get());
            System.out.println("getget :" + db.collection(Application.localShowCollectionName).get().get());
            for (DocumentSnapshot snap : db.collection(Application.localShowCollectionName).get().get().getDocuments()) {
                Show show = getShowFromSnap(snap);
                allShows.add(show);
            }

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return allShows;
    }

    public Show getShow(UUID showID) {
        return null;
    }

    public List<LocalDateTime> getShowTimes(UUID showId) {
        return null;
    }

    public List<Seat> getAvailableSeats(UUID showId, LocalDateTime time) {
        return null;
    }

    public Seat getSeat(UUID showId, UUID seatId) {
        return null;
    }
}
