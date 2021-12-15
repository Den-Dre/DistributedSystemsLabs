package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.Application;
import be.kuleuven.distributedsystems.cloud.Model;
import be.kuleuven.distributedsystems.cloud.entities.*;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minidev.json.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.SerializationUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@RestController
public class PubSubController {
    private final Model model;

    @Autowired
    private final WebClient.Builder builder = WebClient.builder();

    @Resource(name = "db")
    private Firestore db;

    private final HashMap<String, ICompany> companies;

    @Autowired
    public PubSubController(Model model, HashMap<String, ICompany> companies) {
        this.model = model;
        this.companies = companies;
    }

    // https://cloud.google.com/pubsub/docs/troubleshooting#messages
    @PostMapping("/push")
    public ResponseEntity<Void> confirmQuote(@RequestBody String message) throws ParseException {
        String customer;
        ArrayList<Quote> quotes;
        ArrayList<Ticket> successfulTickets = new ArrayList<>();
        String API_KEY = null;

        try {
            JsonParser parser = new JsonParser();
            JsonObject obj  = parser.parse(message).getAsJsonObject();
            JsonObject messageOjb = obj.get("message").getAsJsonObject();

            JsonObject attributes = messageOjb.get("attributes").getAsJsonObject();
            API_KEY = attributes.get("apiKey").toString();
            customer = attributes.get("customer").toString();

            String quotesString = messageOjb.get("data").getAsString();
            byte[] data = Base64.getDecoder().decode(quotesString);
            quotes = (ArrayList<Quote>) SerializationUtils.deserialize(data);

            String finalCustomer = customer.replaceAll("\"", "");


            // Confirm all the quotes of the remote companies
            List<Quote> remoteQuotes = quotes.stream().filter(q -> !companies.get(q.getCompany()).isLocal()).collect(Collectors.toList());
            if (!remoteQuotes.isEmpty())
                successfulTickets.addAll(companies.get(remoteQuotes.get(0).getCompany()).confirmQuotes(remoteQuotes, customer, API_KEY, builder));

            // Confirm all the quotes of the local companies: we do this because we can put them all into a transaction
            List<Quote> localQuotes = quotes.stream().filter(q -> companies.get(q.getCompany()).isLocal()).collect(Collectors.toList());
            if (!localQuotes.isEmpty())
                successfulTickets.addAll(companies.get(localQuotes.get(0).getCompany()).confirmQuotes(localQuotes, customer, API_KEY, builder));


//            for (Quote q : quotes) {
//                 // PubsubMessage.builder and publish
//                ICompany company = companies.get(q.getCompany());
//                var ticket = company.confirmQuote(q, customer, API_KEY, builder);
//                successfulTickets.add(ticket);
//            }


            ArrayList<Ticket> tickets = quotes.stream().map(
                    quote -> new Ticket(quote.getCompany(), quote.getShowId(), quote.getSeatId(), UUID.randomUUID(), finalCustomer)
            ).collect(Collectors.toCollection(ArrayList::new));
            updateBestCustomers(customer, tickets.size());

            addBooking(new Booking(UUID.randomUUID(), LocalDateTime.now(), tickets, finalCustomer));

        } catch (Exception e) {
            // Prevent duplicate bookings
            System.out.println("Duplicate booking detected: " + e);
            String finalAPI_KEY = API_KEY;
            // delegate the deletion of tickets to their respective companies
            successfulTickets.forEach(t -> companies.get(t.getCompany()).undoBooking(t, finalAPI_KEY, builder));
            // undoBookings(successfulTicketIDs, API_KEY);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

     /**
      * Add the given booking to the list of kept bookings.
      * @param booking: the {@link Booking} to be added.
      */
    public void addBooking(Booking booking) {
        ApiFuture<WriteResult> future = db.collection("Bookings").document("booking_" + booking.getId()).set(booking);
        try {
            WriteResult result = future.get();
            //System.out.println("Update time : " + result.getUpdateTime());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void updateBestCustomers(String customer, Integer n_tickets) {
        DocumentReference ref = db.collection(Application.bestCustomersCollectionName).document(customer);
        try {
            int n_tickets_current = 0;
            try {
                n_tickets_current = Math.toIntExact(ref.get().get().getLong("n_tickets"));
            } catch (NullPointerException ignored){}
            Map<String, Object> n_tickets_new = Collections.singletonMap("n_tickets", n_tickets_current + n_tickets);
            ref.set(n_tickets_new).get();
        } catch (InterruptedException | ExecutionException | NullPointerException e) {
            e.printStackTrace();
        }

    }

//    // Remove made bookings due to a duplicate booking being present in the list
//    private void undoBookings(ArrayList<Ticket> toDelete, String API_KEY) {
//        for (Ticket t : toDelete) {
//
//            var ticket = builder
//                    .baseUrl(String.format("https://%s/", t.getCompany()))
//                    .build()
//                    .delete()
//                    .uri(builder -> builder
//                            .pathSegment("shows/{showId}/seats/{seatId}/ticket/{ticketID}")
//                            .queryParam("customer", t.getCustomer())
//                            .queryParam("key", API_KEY.replaceAll("\"", ""))
//                            .build(t.getShowId().toString(), t.getSeatId().toString(), t.getTicketId().toString()))
//                    .retrieve()
//                    .bodyToMono(new ParameterizedTypeReference<Ticket>() {
//                    })
//                    .block();
//        }
//    }
}
