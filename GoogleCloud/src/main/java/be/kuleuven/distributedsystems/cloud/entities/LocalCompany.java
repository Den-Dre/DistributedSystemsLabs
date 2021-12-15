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

    private final Map<String, Object> bookSeatMap = Collections.singletonMap("booked", true);
    private final Map<String, Object> unbookSeatMap = Collections.singletonMap("booked", false);

    public boolean isLocal() {return true;}

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
                // System.out.println("Show retrieved: " + document.get("name"));
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
        Query query = db.collection(Application.localShowCollectionName)
                .document(showId.toString())
                .collection(Application.seatsCollectionName)
                .whereEqualTo("time", time)
                .whereEqualTo("booked", false);
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

    public List<Ticket> confirmQuotes(List<Quote> quotes, String customer, String api_key, WebClient.Builder builder) throws Exception {
        List<DocumentReference> seatRefs = new ArrayList<>();
        for (Quote q: quotes) {
            seatRefs.add(db.collection(Application.localShowCollectionName)
                    .document(q.getShowId().toString())
                    .collection(Application.seatsCollectionName)
                    .document(q.getSeatId().toString()));
        }

        // DocumentSnapshot seatSnap = seatRef.get().get();
        // Boolean alreadyBooked = seatSnap.getBoolean("booked");
        // If the seat is booked throw an error
//        if (Boolean.TRUE.equals(alreadyBooked))
//            throw new Exception("Double booking in LocalCompany");

        // Book the seat
        // seatRef.update(bookSeatMap).get();
        // TODO: is it correct to use UUID.randomUUID()?

        ApiFuture<Void> futureTransaction = db.runTransaction(transaction -> {
            // retrieve document and increment population field
            // 1. Loop over seatRefs to read all the seats
            List<DocumentSnapshot> snaps = new ArrayList<>();
            for (DocumentReference seatRef: seatRefs) {
                snaps.add(transaction.get(seatRef).get());
            }

            // 2. check if all the loaded seats are not booked
            for (DocumentSnapshot seat: snaps) {
                Boolean alreadyBooked = seat.getBoolean("booked");

                if (Boolean.TRUE.equals(alreadyBooked))
                    throw new Exception("Double booking in LocalCompany");
            }

            // 3. Loop over all the seats to book them
            for (DocumentReference seatRef: seatRefs) {
                transaction.update(seatRef, "booked", true);
            }

            return null;
        });

        futureTransaction.get();

        List<Ticket> tickets = new ArrayList<>();
        for (Quote q: quotes) {
            tickets.add(new Ticket(q.getCompany(), q.getShowId(), q.getSeatId(), UUID.randomUUID(), customer));
        }

        return tickets;
    }

    public void undoBooking(Ticket t, String API_KEY, WebClient.Builder builder) {
        // TODO: does this method need to be implemented?
        // --> If someone else has booked the seat before you, you will not be able to book it anyways
        // + we don't want to un-book the other persons booking
//        DocumentReference seatRef = db.collection(Application.localShowCollectionName)
//                .document(t.getShowId().toString())
//                .collection(Application.seatsCollectionName)
//                .document(t.getSeatId().toString());
//
//        try {
//            DocumentSnapshot seatSnap = seatRef.get().get();
//            Boolean alreadyBooked = seatSnap.getBoolean("booked");
//            System.out.println("Duplicate booking in local was: " + alreadyBooked);
//
//            seatRef.update(unbookSeatMap).get();
//        } catch (InterruptedException | ExecutionException e) {
//            e.printStackTrace();
//        }
    }
}
