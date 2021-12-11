package be.kuleuven.distributedsystems.cloud.entities;

import be.kuleuven.distributedsystems.cloud.Application;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Component
public class LocalCompany implements ICompany {
    private Firestore db;

    public LocalCompany(Firestore db) {
        this.db = db;
    }

    public List<Show> getShows(WebClient.Builder builder) {
        List<Show> allShows = new ArrayList<>();
        try {
            for (DocumentSnapshot snap : db.collection(Application.localShowCollectionName).get().get().getDocuments()) {
                Show show = getShowFromSnap(snap);
                allShows.add(show);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return allShows;
    }

    public Show getShow(UUID showId, WebClient.Builder builder) {
        Query query = db.collection(Application.localShowCollectionName).whereEqualTo("showId", showId);

        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        Show show = null;
        try {
            for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
                System.out.println("Show retrieved: " + document.get("name"));
                show = getShowFromSnap(document);

            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return show;
    }

    public List<LocalDateTime> getShowTimes(UUID showId, WebClient.Builder builder) {
        List<LocalDateTime> times = null;
        try {
            DocumentSnapshot snap = db.collection(Application.localShowCollectionName).document(showId.toString()).collection(Application.timesCollectionName).document(Application.timesCollectionName).get().get();
            times = getTimesFromSnap(snap);

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return times;
    }

    public List<Seat> getAvailableSeats(UUID showId, LocalDateTime time, WebClient.Builder builder) {
        Query query = db.collection(Application.localShowCollectionName).document(showId.toString()).collection(Application.seatsCollectionName).whereEqualTo("time", time);
        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        List<Seat> seats = new ArrayList<>();
        try {
            for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
                seats.add(getSeatFromSnap(document));
            }

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return seats;
    }

    // TODO: test this method (can only be done after getAvailableSeats)
    public Seat getSeat(UUID showId, UUID seatId, WebClient.Builder builder) {
        Seat seat = null;

        try {
            DocumentSnapshot snap = db.collection(Application.localShowCollectionName)
                    .document(showId.toString())
                    .collection(Application.seatsCollectionName)
                    .document(seatId.toString()).get().get();
            seat = getSeatFromSnap(snap);

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }


        return seat;
    }
}
