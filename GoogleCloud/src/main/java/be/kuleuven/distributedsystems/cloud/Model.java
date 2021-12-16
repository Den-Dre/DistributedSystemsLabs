package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.*;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.firestore.*;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class Model {

    private static final int RETRY_DELAY = 1000;

    private final static String API_KEY = "wCIoTqec6vGJijW2meeqSokanZuqOL";
    private final WebClient.Builder builder;

    @Resource(name = "db")
    private Firestore db;

    @Resource(name = "isProduction")
    private boolean isProduction;

    @Resource(name = "applicationURL")
    private String applicationURL;

    private final HashMap<String, ICompany> companies;

    // private final HashMap<String, Integer> bestCustomersList = new HashMap<>();

    @Autowired
    public Model(HashMap<String, ICompany> companies, WebClient.Builder builder) {
        this.companies = companies;
        this.builder = builder;
    }

    /**
     * Fetch all shows from the API-endpoint.
     *
     * @return A List of {@link Show} objects.
     */
    public List<Show> getShows()  {
        List<Show> allShows = new ArrayList<>();
        for (ICompany company : companies.values()) {
            allShows.addAll(company.getShows(builder));
        }

        return allShows;
    }

    /**
     * Fetch a {@link Show} given the company name and the showId.
     *
     * @param company String representing a company (e.g. "reliabletheatrecompany")
     * @param showId The id of a show.
     * @return A {@link Show} object.
     */
    public Show getShow(String company, UUID showId) {
        return companies.get(company).getShow(showId, builder);
    }

    /**
     * Get a list of all the times a show is played.
     *
     * @param company String representing a company (e.g. "reliabletheatrecompany")
     * @param showId The id of the show to get the times of.
     * @return A List of {@link LocalDateTime} objects.
     */
    public List<LocalDateTime> getShowTimes(String company, UUID showId) {
        return companies.get(company).getShowTimes(showId, builder);
    }

    /**
     * Get a list of available seats for a given show at a given time at a given company.
     *
     * @param company String representing a company (e.g. "reliabletheatrecompany")
     * @param showId The id of the show to get the available seats of.
     * @param time The time at which to look for available seats.
     * @return A list of available {@link Seat} objects.
     */
    public List<Seat> getAvailableSeats(String company, UUID showId, LocalDateTime time) {
        return companies.get(company).getAvailableSeats(showId, time, builder);
    }

    public Seat getSeat(String company, UUID showId, UUID seatId) {
        return companies.get(company).getSeat(showId, seatId, builder);
    }

    /**
     * Get the ticket associated to the given parameters
     *
     * @param company: the company where the ticket was purchased
     * @param showId: the id of the show the ticket was purchased for
     * @param seatId: the id of the seat the ticket was purchased for
     * @return t iff. there exists a ticket with attributes matching to the parameters;
     *         else {@code null} is returned.
     */
    public Ticket getTicket(String company, UUID showId, UUID seatId) {
        for (Booking b : getAllBookings()) {
            for (Ticket t : b.getTickets()) {
                if (t.getCompany().equals(company) && t.getShowId().equals(showId) && t.getSeatId().equals(seatId)) {
                    return t;
                }
            }
        }

        return null;
    }

    /**
     * Return all bookings made by the given {@code customer}.
     *
     * @param customer: the email address of the given {@code customer}
     * @return bookings: a list of bookings made by the given {@code customer}
     */
    public List<Booking> getBookings(String customer) {
        List<Booking> bookings = new ArrayList<>();
        try {
            // Get all the bookings of the customer from firestore
            QuerySnapshot snapshot = db.collection("Bookings").whereEqualTo("customer", customer).get().get();
            for (DocumentSnapshot bookingSnap : snapshot.getDocuments()) {
                bookings.add(bookingFromSnap(bookingSnap));
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return bookings;
    }

    /**
     * Return all bookings that have been made up until now.
     * @return bookings: a list of all made bookings
     */
    @SuppressWarnings("unchecked")
    public List<Booking> getAllBookings() {
        List<Booking> bookings = new ArrayList<>();
        try {
            // Retrieve the bookings from firestore
            QuerySnapshot snapshot = db.collection("Bookings").get().get();
            // Loop over all the snapshots
            for (DocumentSnapshot bookingSnap : snapshot.getDocuments()) {
                bookings.add(bookingFromSnap(bookingSnap));
            }
        } catch (InterruptedException | ExecutionException | NullPointerException e) {
            e.printStackTrace();
        }
        return bookings;
    }

    private Ticket mapToTicket(Map<String, Object> ticketMap) {
        String company = (String) ticketMap.get("company");
        String customer = (String) ticketMap.get("customer");
        UUID seatId = mapToUUID(ticketMap.get("seatId"));
        UUID showId = mapToUUID(ticketMap.get("showId"));
        UUID ticketId = mapToUUID(ticketMap.get("ticketId"));
        return new Ticket(company, showId, seatId, ticketId, customer);
    }

    private Booking bookingFromSnap(DocumentSnapshot snap) {
        UUID id = mapToUUID(snap.get("id"));
        LocalDateTime time = getLocalDateTime((Map<String, Object>) snap.get("time"));

        ArrayList<Ticket> tickets = new ArrayList<>();
        for (Map<String, Object> map : (ArrayList<Map<String, Object>>) snap.get("tickets")) {
            tickets.add(mapToTicket(map));
        }

        String customer = (String) snap.get("customer");
        return new Booking(id, time, tickets, customer);
    }

    public static LocalDateTime getLocalDateTime(Map<String, Object> timeMap) {
        return LocalDateTime.of(
                Math.toIntExact((long) timeMap.get("year")),
                Math.toIntExact((long) timeMap.get("monthValue")),
                Math.toIntExact((long) timeMap.get("dayOfMonth")),
                Math.toIntExact((long) timeMap.get("hour")),
                Math.toIntExact((long) timeMap.get("minute")),
                Math.toIntExact((long) timeMap.get("second")),
                Math.toIntExact((long) timeMap.get("nano"))
        );
    }

    @SuppressWarnings("unchecked")
    private UUID mapToUUID(Object map) {
        Map<String, Long> castMap = (Map<String, Long>) map;
        return new UUID(
                castMap.get("mostSignificantBits"),
                castMap.get("leastSignificantBits")
        );
    }

    public Set<String> getBestCustomers() {
        // TODO: return the best customer (highest number of tickets, return all of them if multiple customers have an equal amount)
        // Source: https://stackoverflow.com/a/11256352
        HashSet<String> bestCustomers = new HashSet<>();
        HashMap<String, Integer> bestCustomersList = new HashMap<>();
        try {
            List<QueryDocumentSnapshot> customers = db.collection(Application.bestCustomersCollectionName).get().get().getDocuments();
            for (DocumentSnapshot c: customers) {
                String customer = c.getId().replaceAll("\"", "");
                int n_tickets = Math.toIntExact(c.getLong("n_tickets"));
                bestCustomersList.put(customer, n_tickets);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }


        if (bestCustomersList.isEmpty()) return null;
        int maxValueInMap=(Collections.max(bestCustomersList.values()));  // This will return max value in the HashMap
        for (Map.Entry<String, Integer> entry : bestCustomersList.entrySet()) {  // Iterate through HashMap
            if (entry.getValue()==maxValueInMap) {
                bestCustomers.add(entry.getKey());
            }
        }

        return bestCustomers;
    }

    /**
     * Convert the given list of {@link Quote}s into {@link Ticket}s
     * and add these to the current {@link Booking}.
     *
     * @param quotes: The list of {@link Quote}s to be converted and added
     * @param customer: The customer who has made the given {@link Quote}s
     */
    public void confirmQuotes(List<Quote> quotes, String customer) throws InterruptedException {

        // source https://cloud.google.com/pubsub/docs/emulator#accessing_environment_variables
        // Use localhost of emulator instead of real endpoint!

        TopicName topicName = TopicName.of(isProduction ? "distributedsystemspart2" : "demo-distributed-systems-kul", Application.TOPIC);

        Publisher publisher = null;
        ManagedChannel channel = null;
        TransportChannelProvider channelProvider = null;
        CredentialsProvider credentialsProvider = null;
        if (!isProduction) {
            String hostport = "localhost:8083";
            channel = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build();
            channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
            credentialsProvider = NoCredentialsProvider.create();
        }

        try {
            // Set the channel and credentials provider when creating a `Publisher`.
            // Similarly for Subscriber
            if (isProduction) {
                publisher = Publisher.newBuilder(topicName)
                        .build();
            } else {
                publisher = Publisher.newBuilder(topicName)
                        .setChannelProvider(channelProvider)
                        .setCredentialsProvider(credentialsProvider)
                        .build();
            }

            ArrayList<Quote> quotesArray = new ArrayList<>(quotes);
            byte[] quotesSerialized = SerializationUtils.serialize(quotesArray);
            ByteString data = ByteString.copyFrom(quotesSerialized);

            PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).putAttributes("customer", customer).putAttributes("apiKey", API_KEY).build();
            // if we don't add this .get(), the finally clause gets executed before the message is sent: apiFuture.get() is a blocking call!
            publisher.publish(pubsubMessage).get();

        } catch (IOException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            if (channel != null) {
                channel.shutdown();
            }
            if (publisher != null) {
                // When finished with the publisher, shutdown to free up resources.
                publisher.shutdown();
                publisher.awaitTermination(1, TimeUnit.MINUTES);
            }
        }
    }
}
