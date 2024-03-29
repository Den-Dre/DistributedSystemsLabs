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
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import net.minidev.json.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.SerializationUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.Resource;
import java.io.IOException;
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

    private final String sendGridAPIKey = "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";

    @Autowired
    public PubSubController(Model model, HashMap<String, ICompany> companies) {
        this.model = model;
        this.companies = companies;
    }


    // https://cloud.google.com/pubsub/docs/troubleshooting#messages
    @PostMapping("/push")
    public ResponseEntity<Void> confirmQuote(@RequestBody String message) throws ParseException {
        String customer = null;
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
            customer = finalCustomer;


            // Confirm all the quotes of the remote companies
            List<Quote> remoteQuotes = quotes.stream().filter(q -> !companies.get(q.getCompany()).isLocal()).collect(Collectors.toList());
            if (!remoteQuotes.isEmpty())
                successfulTickets.addAll(companies.get(remoteQuotes.get(0).getCompany()).confirmQuotes(remoteQuotes, customer, API_KEY, builder));

            // Confirm all the quotes of the local companies: we do this because we can put them all into a transaction
            List<Quote> localQuotes = quotes.stream().filter(q -> companies.get(q.getCompany()).isLocal()).collect(Collectors.toList());
            if (!localQuotes.isEmpty())
                successfulTickets.addAll(companies.get(localQuotes.get(0).getCompany()).confirmQuotes(localQuotes, customer, API_KEY, builder));

            // Make an update to the bestCustomers-list
            updateBestCustomers(customer, successfulTickets.size());

            // Add the booking to firestore
            addBooking(new Booking(UUID.randomUUID(), LocalDateTime.now(), successfulTickets, finalCustomer));
            sendMail(true, finalCustomer);
            System.out.println("Mail for successful booking sent to " + finalCustomer);

        } catch (Exception e) {
            // Prevent duplicate bookings
            System.out.println("Duplicate booking detected: " + e);
            String finalAPI_KEY = API_KEY;
            // delegate the deletion of tickets to their respective companies
            successfulTickets.forEach(t -> companies.get(t.getCompany()).undoBooking(t, finalAPI_KEY, builder));
            sendMail(false, customer);
            System.out.println("Mail for unsuccessful booking sent to " + customer);
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

    private void sendMail(Boolean successFullBooking, String recipient) {
        // TODO: don't send when in testing??
        Email from = new Email("r0760777@kuleuven.be");
        String subject = successFullBooking
                ? "Booking confirmation mail"
                : "Booking did not succeed";
        Email to = new Email(recipient);
        String contents = successFullBooking
                ? "Your booking was successful, enjoy the show!!!"
                : "Oh no, your booking failed :(";
        Content content = new Content("text/plain", contents);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(sendGridAPIKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            System.out.println("Mail status code: " + response.getStatusCode());
//            System.out.println(response.getBody());
//            System.out.println(response.getHeaders());
        } catch (IOException ex) {
            System.out.println("sending mail failed");
        }
    }
}
