package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.Application;
import be.kuleuven.distributedsystems.cloud.Model;
import be.kuleuven.distributedsystems.cloud.entities.Quote;
import be.kuleuven.distributedsystems.cloud.entities.Seat;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class APIController {
    private final Model model;

    @Autowired
    public APIController(Model model) {
        this.model = model;
        System.out.println("We got in da constructa!");

        SubscriptionAdminClient subscriptionAdminClient;
        String hostport = "localhost:8083";
        ManagedChannel channel = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build();
        try {

            TransportChannelProvider channelProvider =
                    FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
            CredentialsProvider credentialsProvider = NoCredentialsProvider.create();

            String pushEndpoint = "http://localhost:8080/push";
            PushConfig pushConfig = PushConfig.newBuilder().setPushEndpoint(pushEndpoint).build();
            subscriptionAdminClient = SubscriptionAdminClient.create(
                    SubscriptionAdminSettings.newBuilder()
                            .setCredentialsProvider(credentialsProvider)
                            .setTransportChannelProvider(channelProvider)
                            .build());

            TopicName topicName = TopicName.of("demo-distributed-systems-kul", Application.TOPIC);
            TopicAdminClient topicClient =
                    TopicAdminClient.create(
                            TopicAdminSettings.newBuilder()
                                    .setTransportChannelProvider(channelProvider)
                                    .setCredentialsProvider(credentialsProvider)
                                    .build());
            Topic topic;
            try {
//                if (topicClient.getTopic(topicName) == null)
//                else
//                topic = topicClient.getTopic(topicName);
                topic = topicClient.createTopic(topicName);
                System.out.println("Topic toppie! " + topic.getName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            ProjectSubscriptionName subscriptionName =
                    ProjectSubscriptionName.of("demo-distributed-systems-kul", "testSubscriptionID");
            Subscription subscription;
            try {
//                if (subscriptionAdminClient.getSubscription(subscriptionName) == null)
//                else
//                subscription = subscriptionAdminClient.getSubscription(subscriptionName);
                subscription = subscriptionAdminClient.createSubscription(subscriptionName, topicName, pushConfig, 60);
                System.out.println("Created push subscription: " + subscription.getName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            channel.shutdown();
                // When finished with the publisher, shutdown to free up resources.
        }
    }

    @PostMapping(path = "/addToCart", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<Void> addToCart(
            @ModelAttribute Quote quote,
            @RequestHeader(value = "referer") String referer,
            @CookieValue(value = "cart", required = false) String cartString) {
        List<Quote> cart = Cart.fromCookie(cartString);
        cart.add(quote);
        ResponseCookie cookie = Cart.toCookie(cart);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        headers.add(HttpHeaders.LOCATION, referer);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @PostMapping("/removeFromCart")
    public ResponseEntity<Void> removeFromCart(
            @ModelAttribute Quote quote,
            @RequestHeader(value = "referer") String referer,
            @CookieValue(value = "cart", required = false) String cartString) {
        List<Quote> cart = Cart.fromCookie(cartString);
        cart.remove(quote);
        ResponseCookie cookie = Cart.toCookie(cart);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        headers.add(HttpHeaders.LOCATION, referer);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @PostMapping("/confirmCart")
    public ResponseEntity<Void> confirmCart(
            @CookieValue(value = "cart", required = false) String cartString) throws Exception {
        List<Quote> cart = Cart.fromCookie(cartString);
        System.out.println("Got into /confirmCart endpoint");
        this.model.confirmQuotes(new ArrayList<>(cart), AuthController.getUser().getEmail());
        cart.clear();
        ResponseCookie cookie = Cart.toCookie(cart);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        headers.add(HttpHeaders.LOCATION, "/account");
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

//    @PostMapping("/test")
//    public ResponseEntity<Void> confirmQuote(@RequestBody ByteString body ) {
//        System.out.println("Got into endpoint");
//        return new ResponseEntity<>(HttpStatus.OK);
//    }
    // TODO receive the serialized quotes from Model.java, deserialze them
    // and put into PUT request

}
