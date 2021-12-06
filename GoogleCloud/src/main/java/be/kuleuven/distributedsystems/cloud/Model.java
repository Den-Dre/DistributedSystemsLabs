package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.*;
import com.google.api.core.ApiFuture;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
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

import static be.kuleuven.distributedsystems.cloud.Application.db;

@Component
public class Model {

    private static final int RETRY_DELAY = 1000;

    @Autowired
    private final WebClient.Builder builder = WebClient.builder();

    private final static String API_KEY = "wCIoTqec6vGJijW2meeqSokanZuqOL";

    @Resource(name = "db")
    private Firestore db;

    private final HashMap<String, Integer> bestCustomersList = new HashMap<>();

    public void addBestCustomer(String customer, ArrayList<Ticket> tickets) {
        if (bestCustomersList.containsKey(customer))
            bestCustomersList.put(customer, tickets.size() + bestCustomersList.get(customer));
        else
            bestCustomersList.put(customer, tickets.size());
    }

    /**
     * Add the given booking to the list of kept bookings.
     * @param booking: the {@link Booking} to be added.
     */
    // TODO remove this?
    @Deprecated
    public void addBooking(Booking booking) {
        ApiFuture<WriteResult> future = db.collection("Bookings").document("booking_" + booking.getId()).set(booking);
        try {
            System.out.println("Update time : " + future.get().getUpdateTime());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Fetch all shows from the API-endpoint.
     *
     * @return A List of {@link Show} objects.
     */
    public List<Show> getShows()  {
        var shows = builder
                            .baseUrl("https://reliabletheatrecompany.com/")
                            .build()
                            .get()
                            .uri(builder -> builder
                                    .pathSegment("shows")
                                    .queryParam("key", API_KEY)
                                    .build())
                            .retrieve()
                            .bodyToMono(new ParameterizedTypeReference<CollectionModel<Show>>() {})
                            .block()
                            .getContent();
        boolean succes = false;
        Collection<Show>shows2 = null;
        while (!succes) {
            try {
                shows2 = builder
                        .baseUrl("https://unreliabletheatrecompany.com/")
                        .build()
                        .get()
                        .uri(builder -> builder
                                .pathSegment("shows")
                                .queryParam("key", API_KEY)
                                .build())
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<CollectionModel<Show>>() {})
                        .block()
                        .getContent();
                succes = true;
            } catch (Exception e) {
                System.out.println(e);
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY);
                } catch (InterruptedException e2) {
                    System.out.println(e2);
                }

            }
        }


        List<Show> allShows = new ArrayList<>();
        allShows.addAll(shows);
        allShows.addAll(shows2);

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

    private Show getShowFromSnap(DocumentSnapshot snap) {
        String showName = snap.get("name").toString();
        String location = snap.get("location").toString();
        String image = snap.get("image").toString();
        UUID showId = mapToUUID(snap.get("showId"));
        return new Show(Application.companyName, showId, showName, location, image);
    }

    /**
     * Fetch a {@link Show} given the company name and the showId.
     *
     * @param company String representing a company (e.g. "reliabletheatrecompany")
     * @param showId The id of a show.
     * @return A {@link Show} object.
     */
    public Show getShow(String company, UUID showId) {
        boolean succes = false;
        Show show = null;
        while (!succes) {
            try {
                show = builder
                        .baseUrl(String.format("https://%s/", company))
                        .build()
                        .get()
                        .uri(builder -> builder
                                .pathSegment("shows/{showId}")
                                .queryParam("key", API_KEY)
                                .build(showId.toString()))
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Show>() {})
                        .block();
                succes = true;
            } catch (Exception e) {
                System.out.println(e);
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY);
                } catch (InterruptedException e2) {
                    System.out.println(e2);
                }

            }
        }

        return show;
    }

    /**
     * Get a list of all the times a show is played.
     *
     * @param company String representing a company (e.g. "reliabletheatrecompany")
     * @param showId The id of the show to get the times of.
     * @return A List of {@link LocalDateTime} objects.
     */
    public List<LocalDateTime> getShowTimes(String company, UUID showId) {
        boolean succes = false;
        Collection<LocalDateTime> times  = null;
        while (!succes) {
            try {
                times = builder
                        .baseUrl(String.format("https://%s/", company))
                        .build()
                        .get()
                        .uri(builder -> builder
                                .pathSegment("shows/{showId}/times")
                                .queryParam("key", API_KEY)
                                .build(showId.toString()))
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<CollectionModel<LocalDateTime>>() {})
                        .block()
                        .getContent();
                succes = true;
            } catch (Exception e) {
                System.out.println(e);
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY);
                } catch (InterruptedException e2) {
                    System.out.println(e2);
                }

            }
        }
        return List.copyOf(times);
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
        boolean succes = false;
        Collection<Seat> seats  = null;
        while (!succes) {
            try {
                seats = builder
                        .baseUrl(String.format("https://%s/", company))
                        .build()
                        .get()
                        .uri(builder -> builder
                                .pathSegment("shows/{showId}/seats")
                                .queryParam("key", API_KEY)
                                .queryParam("time", time.toString())
                                .queryParam("available", "true")
                                .build(showId.toString()))
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<CollectionModel<Seat>>() {})
                        .block()
                        .getContent();
                succes = true;
            } catch (Exception e) {
                System.out.println(e);
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY);
                } catch (InterruptedException e2) {
                    System.out.println(e2);
                }

            }
        }
        return List.copyOf(seats);
    }

    public Seat getSeat(String company, UUID showId, UUID seatId) {
        boolean succes = false;
        Seat seat  = null;
        while (!succes) {
            try {
                seat = builder
                        .baseUrl(String.format("https://%s/", company))
                        .build()
                        .get()
                        .uri(builder -> builder
                                .pathSegment("shows/{showId}/seats/{seatId}")
                                .queryParam("key", API_KEY)
                                .build(showId.toString(), seatId.toString()))
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Seat>() {})
                        .block();
                succes = true;
            } catch (Exception e) {
                System.out.println(e);
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException e2) {
                    System.out.println(e2);
                }

            }
        }

        return seat;
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
        // Take the delay of the Pub Sub into account
        try {
            TimeUnit.MILLISECONDS.sleep(RETRY_DELAY/2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        var bookings = getAllBookings();
        System.out.println("Current customer: " + customer);
        bookings.forEach(b -> System.out.println("Booking x of Model.bookings: " + b.getCustomer()));
        return bookings
                .stream()
                .filter(b -> b.getCustomer().equals(customer))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Return all bookings that have been made up until now.
     * @return bookings: a list of all made bookings
     */
    @SuppressWarnings("unchecked")
    public List<Booking> getAllBookings() {
        CollectionReference reference = db.collection("Bookings");
        System.out.println("Get all bookings: ");
        reference.listDocuments().forEach(System.out::println);
        QuerySnapshot snapshot = null;
        try {
            snapshot = db.collection("Bookings").get().get();

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        assert snapshot != null;
        List<Booking> bookings = new ArrayList<>();
        for (QueryDocumentSnapshot snap : snapshot.getDocuments()) {
            System.out.println("Customer from snapshot" + snap.get("customer"));
            UUID id = mapToUUID(snap.get("id"));
            Map<String, Object> timesnap = (Map<String, Object>) snap.get("time");
            LocalDateTime time = LocalDateTime.of(
                    Math.toIntExact((long) timesnap.get("year")),
                    Math.toIntExact((long) timesnap.get("monthValue")),
                    Math.toIntExact((long) timesnap.get("dayOfMonth")),
                    Math.toIntExact((long) timesnap.get("hour")),
                    Math.toIntExact((long) timesnap.get("minute")),
                    Math.toIntExact((long) timesnap.get("second")),
                    Math.toIntExact((long) timesnap.get("nano"))
                    );
            ArrayList<Ticket> tickets = new ArrayList<>();
            for (Map<String, Object> map : (ArrayList<Map<String, Object>>) Objects.requireNonNull(snap.get("tickets"))) {
                map.entrySet().forEach(System.out::println);
                String company = (String) map.get("company");
                String customer = (String) map.get("customer");
                UUID seatId = mapToUUID(map.get("seatId"));
                UUID showId = mapToUUID(map.get("showId"));
                UUID ticketId = mapToUUID(map.get("ticketId"));
                tickets.add(new Ticket(company, showId, seatId, ticketId, customer));
            }

            String customer = (String) snap.get("customer");
            bookings.add(new Booking(id, time, tickets, customer));
        }
        return bookings;
    }

    @SuppressWarnings("unchecked")
    private UUID mapToUUID(Object map) {
        // System.out.println("Received map in mapToUUID: " + map);
        Map<String, Long> castMap = (Map<String, Long>) map;
        castMap.entrySet().forEach(System.out::println);
        return new UUID(
                castMap.get("mostSignificantBits"),
                castMap.get("leastSignificantBits")
        );
    }

    public Set<String> getBestCustomers() {
        // TODO: return the best customer (highest number of tickets, return all of them if multiple customers have an equal amount)
        // Source: https://stackoverflow.com/a/11256352
        HashSet<String> bestCustomers = new HashSet<>();
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
        String hostport = "localhost:8083";
        ManagedChannel channel = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build();

        Publisher publisher = null;
        try {
            TransportChannelProvider channelProvider =
                    FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
            CredentialsProvider credentialsProvider = NoCredentialsProvider.create();
            TopicName topicName = TopicName.of("demo-distributed-systems-kul", Application.TOPIC);

            // Set the channel and credentials provider when creating a `Publisher`.
            // Similarly for Subscriber
            publisher = Publisher.newBuilder(topicName)
                    .setChannelProvider(channelProvider)
                    .setCredentialsProvider(credentialsProvider)
                    .build();

            ArrayList<Quote> quotesArray = new ArrayList<>(quotes);
            byte[] quotesSerialized = SerializationUtils.serialize(quotesArray);
            ByteString data = ByteString.copyFrom(quotesSerialized);
            System.out.println("Quote of ticket: " + quotesSerialized.toString());
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).putAttributes("customer", customer).putAttributes("apiKey", API_KEY).build();
            // if we don't add this .get(), the finally clause gets executed before the message is sent: apiFuture.get() is a blocking call!
            publisher.publish(pubsubMessage).get();
        } catch (IOException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            channel.shutdown();
            if (publisher != null) {
                // When finished with the publisher, shutdown to free up resources.
                publisher.shutdown();
                publisher.awaitTermination(1, TimeUnit.MINUTES);
            }
        }
    }
}
