package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.Model;
import be.kuleuven.distributedsystems.cloud.entities.Quote;
import be.kuleuven.distributedsystems.cloud.entities.Ticket;
import com.google.api.client.json.Json;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.SerializationUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

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
    // <- Google documentatie is officieel poep
    @PostMapping("/push")
    public ResponseEntity<Void> confirmQuote(@RequestBody String message) throws ParseException {
        System.out.println("Got into endpoint");
        System.out.println(message);
//        Gson g = new Gson();
//        Json pubsubMessage = g.fromJson(message, JsonObject.class);
//        System.out.println(pubsubMessage);
        try {
            JsonParser parser = new JsonParser();
            JsonObject obj  = parser.parse(message).getAsJsonObject();
            JsonObject messageOjb = obj.get("message").getAsJsonObject();

            JsonObject attributes = messageOjb.get("attributes").getAsJsonObject();
            String API_KEY = attributes.get("apiKey").toString();
            String customer = attributes.get("customer").toString();
            System.out.println(customer);

            String quotesString = messageOjb.get("data").getAsString();
            byte[] data = Base64.getDecoder().decode(quotesString);
            ArrayList<Quote> quotes = (ArrayList<Quote>) SerializationUtils.deserialize(data);
            System.out.println(API_KEY);
            for (Quote q : quotes) {
                 // PubsubMessage.builder and publish
                System.out.println(q.getCompany());
                var putResult = builder
                        .baseUrl(String.format("https://%s/", q.getCompany()))
                        .build()
                        .put()
                        .uri(builder -> builder
                                .pathSegment("shows/{showId}/seats/{seatId}/ticket")
                                .queryParam("customer", customer.replaceAll("\"", ""))
                                .queryParam("key", API_KEY.replaceAll("\"", ""))
                                .build(q.getShowId().toString(), q.getSeatId().toString()))
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Ticket>() {
                        })
                        .block();
                System.out.println(putResult);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }



        return new ResponseEntity<>(HttpStatus.OK);
    }
}
