package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.*;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.NotFoundException;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.auth.Credentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.pubsub.v1.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.HypermediaWebClientConfigurer;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;

@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
@SpringBootApplication
public class Application {

    public static final String TOPIC = "DS-PubSub";
    public static Firestore firestore;
    public final static String localShowCollectionName = "LocalShows";

    public final static String localCompanyName = "MartijnAndreasCo";
    public final static String urCompanyName = "unreliabletheatrecompany.com";
    public final static String rCompanyName = "reliabletheatrecompany.com";

    // TODO: make private and make getters
    public final static String seatsCollectionName = "seats";
    public final static String timesCollectionName = "times";
    public final static String bestCustomersCollectionName = "bestCustomers";

    private final static String API_KEY = "wCIoTqec6vGJijW2meeqSokanZuqOL";


    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        System.out.println("IN MAAAAAAIN");
        System.setProperty("server.port", System.getenv().getOrDefault("PORT", "8080"));
        System.out.println("Running at port " + System.getenv().getOrDefault("PORT", "8080"));

        // Apache JSP scans by default all JARs, which is not necessary, so disable it
        System.setProperty(org.apache.tomcat.util.scan.Constants.SKIP_JARS_PROPERTY, "*.jar");
        System.setProperty(org.apache.tomcat.util.scan.Constants.SCAN_JARS_PROPERTY, "taglibs-standard-spec-*.jar,taglibs-standard-impl-*.jar");

        // Start Spring Boot application
        ApplicationContext context = SpringApplication.run(Application.class, args);

        // Check whether local shows are present in Firestore
        // Query all local shows

        initialisePubSub();

        try {
            if (db().collection(localShowCollectionName).limit(1).get().get().isEmpty()) {
                System.out.println("Local shows database is empty: uploading data.json");
                uploadLocalShows(context);
            }

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private static void initialisePubSub() {
        if (isProduction()) {
            System.out.println("init pubsub production");
            TopicAdminClient topicClient = null;
            System.out.println("getting topicname");
            TopicName topicName = TopicName.of("distributedsystemspart2", TOPIC);
            try {
                topicClient =
                        TopicAdminClient.create(
                                TopicAdminSettings.newBuilder()
                                        .build());
            } catch (IOException e) {
                e.printStackTrace();
            }

            Topic topic;
            try {
                topic = topicClient.getTopic(topicName);
            } catch (NotFoundException e) {
                topic = topicClient.createTopic(topicName);
            }
            System.out.println("Topic toppie! " + topic.getName());

        } else {
            String hostport = "localhost:8083";
            ManagedChannel channel = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build();
            TransportChannelProvider channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
            CredentialsProvider credentialsProvider = NoCredentialsProvider.create();

            String pushEndpoint = "http://localhost:8080/push";

            // 1. Make the push config
            PushConfig pushConfig = PushConfig.newBuilder().setPushEndpoint(pushEndpoint).build();

            SubscriptionAdminClient subscriptionAdminClient = null;
            try {
                subscriptionAdminClient = SubscriptionAdminClient.create(
                        SubscriptionAdminSettings.newBuilder()
                                .setCredentialsProvider(credentialsProvider)
                                .setTransportChannelProvider(channelProvider)
                                .build());
            } catch (IOException e) {
                e.printStackTrace();
            }

            TopicName topicName = TopicName.of("demo-distributed-systems-kul", TOPIC);
            TopicAdminClient topicClient = null;
            try {
                topicClient = TopicAdminClient.create(
                        TopicAdminSettings.newBuilder()
                                .setTransportChannelProvider(channelProvider)
                                .setCredentialsProvider(credentialsProvider)
                                .build());
            } catch (IOException e) {
                e.printStackTrace();
            }


            Topic topic;
            try {
                topic = topicClient.getTopic(topicName);
            } catch (NotFoundException e) {
                topic = topicClient.createTopic(topicName);
            }
            // System.out.println("Using this topic: " + topic.getName());

            ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of("demo-distributed-systems-kul", "subscriptionID");
            Subscription subscription;
            // Try to fetch the subscription, if it does not exist: create it
            try {
                subscription = subscriptionAdminClient.getSubscription(subscriptionName);
            } catch (NotFoundException e) {
                subscription = subscriptionAdminClient.createSubscription(subscriptionName, topicName, pushConfig, 10);
            }
            // System.out.println("Using following push subscription: " + subscription.getName());
            channel.shutdownNow();
        }
    }

    private static void uploadLocalShows(ApplicationContext context) {
        // Calling db() each time when uploading to firestore caused the weird channel allocation site error
        // Solution -> call it once for the whole method
        Firestore fs = db();
        String contents = readLocalShows(context);

        JsonParser parser = new JsonParser();
        JsonObject obj  = parser.parse(contents).getAsJsonObject();
        JsonArray shows = obj.get("shows").getAsJsonArray();
        String name, location, image;
        JsonArray seats;
        for (int i = 0; i < shows.size(); i++) {
            UUID showId = UUID.randomUUID();
            JsonObject currentShow = shows.get(i).getAsJsonObject();
            name = currentShow.get("name").getAsString();
            location = currentShow.get("location").getAsString();
            image = currentShow.get("image").getAsString();
            seats = currentShow.get("seats").getAsJsonArray();
            String type, seatName, time;
            double price;
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            // HashMap<String, Object> seatsMap = new HashMap<>();
            List<Seat> seatsList = new ArrayList<>();

            HashSet<LocalDateTime> timesSet = new HashSet<>();

            for (int j = 0; j < seats.size(); j++) {
                JsonObject currentSeat = seats.get(j).getAsJsonObject();
                type = currentSeat.get("type").getAsString();
                seatName = currentSeat.get("name").getAsString();
                price = currentSeat.get("price").getAsDouble();
                time = currentSeat.get("time").getAsString();
                LocalDateTime timeObj = LocalDateTime.parse(time, formatter);
                timesSet.add(timeObj);

                Seat seat = new Seat(localCompanyName, showId, UUID.randomUUID(), timeObj, type, seatName, price);
                // seatsMap.put(seat.getSeatId().toString(), seat);
                seatsList.add(seat);
            }
            Show show = new Show(localCompanyName, showId, name, location, image);

            HashMap<String, ArrayList<LocalDateTime>> timesMap = new HashMap<>();
            timesMap.put("times", new ArrayList<>(timesSet));

            try {
                fs.collection(localShowCollectionName).document(showId.toString()).set(show).get();
                // Each seat is now a document. (Instead of all seats in one document called "seats" that is in the collection called "seats")
                for (Seat s: seatsList) {
                    fs.collection(localShowCollectionName).document(showId.toString()).collection(seatsCollectionName).document(s.getSeatId().toString()).set(s).get();
                }
                fs.collection(localShowCollectionName).document(showId.toString()).collection(timesCollectionName).document(timesCollectionName).set(timesMap).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    private static String readLocalShows(ApplicationContext context)  {
        // Source: https://mkyong.com/spring/spring-resource-loader-with-getresource-example/
        System.out.println("looking for data.json");
        Resource resource = context.getResource("classpath:/data.json");

        System.out.println("reading data.json");
        try {
            StringBuilder fileContents = new StringBuilder();
            InputStream in = resource.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                fileContents.append(line);
            }
            reader.close();
            return fileContents.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class FakeCreds extends Credentials {
        final Map<String, List<String>> HEADERS = new HashMap<>();

        public FakeCreds() {
            HEADERS.put("Authorization", List.of(new String[]{"Bearer Owner"}));
        }

        @Override
        public String getAuthenticationType() {
            return null;
        }

        @Override
        public Map<String, List<String>> getRequestMetadata(URI uri) throws IOException {
            return HEADERS;
        }

        @Override
        public boolean hasRequestMetadata() {
            return true;
        }

        @Override
        public boolean hasRequestMetadataOnly() {
            return true;
        }

        @Override
        public void refresh() throws IOException {

        }
    }

//    public FirestoreOptions.Builder setEmulatorHost(String emulatorHost) {}

    @Bean
    public static boolean isProduction() {
        return Objects.equals(System.getenv("GAE_ENV"), "standard");
    }

    @Bean
    public String projectId() {
        return isProduction() ? "distributedsystemspart2" : "demo-distributed-systems-kul";
    }

    public String get_API_KEY() { return API_KEY; }

    @Bean
    public static String applicationURL() {
        return "https://distributedsystemspart2.ew.r.appspot.com";
    }

    /*
     * You can use this builder to create a Spring WebClient instance which can be used to make REST-calls.
     */
    @Bean
    WebClient.Builder webClientBuilder(HypermediaWebClientConfigurer configurer) {
        return configurer.registerHypermediaTypes(WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()))
                .codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs().maxInMemorySize(100 * 1024 * 1024)));
    }

    @Bean
    HttpFirewall httpFirewall() {
        DefaultHttpFirewall firewall = new DefaultHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        return firewall;
    }

    @Bean
    public static Firestore db() {
        if (isProduction()) {
            return FirestoreOptions.newBuilder()
                    .setProjectId("distributedsystemspart2")
                    .build()
                    .getService();
        }
        // Source: https://gist.github.com/ryanpbrewster/aef2a5c411a074819c8d7b67be80621c
        return FirestoreOptions.newBuilder()
                .setProjectId("demo-distributed-systems-kul")
                .setChannelProvider(channelProvider)
                .setCredentialsProvider(FixedCredentialsProvider.create(new FakeCreds()))
                .build()
                .getService();
    }

    private static final InstantiatingGrpcChannelProvider channelProvider =
            InstantiatingGrpcChannelProvider
            .newBuilder()
            .setEndpoint("localhost:8084")
            .setChannelConfigurator(ManagedChannelBuilder::usePlaintext)
            .build();

    @Bean
    HashMap<String, ICompany> companies() {
        HashMap<String, ICompany> companyMap = new HashMap<>();
        companyMap.put(rCompanyName, new RemoteCompany(rCompanyName, db()));
        companyMap.put(urCompanyName, new RemoteCompany(urCompanyName, db()));
        companyMap.put(Application.localCompanyName, new LocalCompany(db()));
        return companyMap;
    }
}
