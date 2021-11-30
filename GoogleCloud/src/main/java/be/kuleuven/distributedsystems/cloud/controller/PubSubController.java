package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.Model;
import be.kuleuven.distributedsystems.cloud.entities.Booking;
import be.kuleuven.distributedsystems.cloud.entities.Quote;
import be.kuleuven.distributedsystems.cloud.entities.Ticket;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class PubSubController {
    private final Model model;

    @Autowired
    private final WebClient.Builder builder = WebClient.builder();

    @Autowired
    public PubSubController(Model model) {
        this.model = model;
    }

    // https://cloud.google.com/pubsub/docs/troubleshooting#messages
    @PostMapping("/push")
    public ResponseEntity<Void> confirmQuote(@RequestBody String message) throws ParseException {
        System.out.println("In confirmQuote");
        String customer;
        ArrayList<Quote> quotes;
        ArrayList<Ticket> successfulTicketIDs = new ArrayList<>();
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

            for (Quote q : quotes) {
                 // PubsubMessage.builder and publish
                String finalCustomer = customer.replaceAll("\"", "");
                String finalAPI_KEY = API_KEY;
                System.out.println(q.getSeatId());
                var ticket = builder
                        .baseUrl(String.format("https://%s/", q.getCompany()))
                        .build()
                        .put()
                        .uri(builder -> builder
                                .pathSegment("shows/{showId}/seats/{seatId}/ticket")
                                .queryParam("customer", finalCustomer)
                                .queryParam("key", finalAPI_KEY.replaceAll("\"", ""))
                                .build(q.getShowId().toString(), q.getSeatId().toString()))
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Ticket>() {
                        })
                        .block();

                successfulTicketIDs.add(ticket);

                ArrayList<Ticket> tickets = quotes.stream().map(
                        quote -> new Ticket(quote.getCompany(), quote.getShowId(), quote.getSeatId(), UUID.randomUUID(), finalCustomer)
                ).collect(Collectors.toCollection(ArrayList::new));
                this.model.addBestCustomer(customer, tickets);
                this.model.addBooking(new Booking(UUID.randomUUID(), LocalDateTime.now(), tickets, finalCustomer));
            }

        } catch (Exception e) {
            // Prevent duplicate bookings
            System.out.println("Duplicate booking detected: " + e);
            undoBookings(successfulTicketIDs, API_KEY);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // Remove made bookings due to a duplicate booking being present in the list
    private void undoBookings(ArrayList<Ticket> toDelete, String API_KEY) {
        for (Ticket t : toDelete) {
            var ticket = builder
                    .baseUrl(String.format("https://%s/", t.getCompany()))
                    .build()
                    .delete()
                    .uri(builder -> builder
                            .pathSegment("shows/{showId}/seats/{seatId}/ticket/{ticketID}")
                            .queryParam("customer", t.getCustomer())
                            .queryParam("key", API_KEY.replaceAll("\"", ""))
                            .build(t.getShowId().toString(), t.getSeatId().toString(), t.getTicketId().toString()))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Ticket>() {
                    })
                    .block();
        }
    }
}
