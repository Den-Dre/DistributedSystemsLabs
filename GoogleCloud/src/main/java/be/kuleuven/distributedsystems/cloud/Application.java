package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.*;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.auth.Credentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.HypermediaWebClientConfigurer;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
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

    public static final String TOPIC = "ABCDEFGH";
    public static Firestore firestore;
    public final static String localShowCollectionName = "LocalShows";
    public final static String localCompanyName = "MartijnAndreasCo";
    public final static String urCompanyName = "unreliabletheatrecompany.com";
    public final static String rCompanyName = "reliabletheatrecompany.com";

    // TODO: make private and make getters
    public final static String seatsCollectionName = "seats";
    public final static String timesCollectionName = "times";

    private final static String API_KEY = "wCIoTqec6vGJijW2meeqSokanZuqOL";

//    @Autowired
//    private final WebClient.Builder webClientBuilder = WebClient.builder();

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        System.setProperty("server.port", System.getenv().getOrDefault("PORT", "8080"));
        System.out.println("Running at port " + System.getenv().getOrDefault("PORT", "8080"));

        // Apache JSP scans by default all JARs, which is not necessary, so disable it
        System.setProperty(org.apache.tomcat.util.scan.Constants.SKIP_JARS_PROPERTY, "*.jar");
        System.setProperty(org.apache.tomcat.util.scan.Constants.SCAN_JARS_PROPERTY, "taglibs-standard-spec-*.jar,taglibs-standard-impl-*.jar");

        // Start Spring Boot application
        ApplicationContext context = SpringApplication.run(Application.class, args);

        // Check whether local shows are present in Firestore
        // Query all local shows

        try {
            if (db().collection(localShowCollectionName).limit(1).get().get().isEmpty()) {
                System.out.println("Local shows database is empty: uploading data.json");
                uploadLocalShows();
            }

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private static void uploadLocalShows() {
        // Calling db() each time when uploading to firestore caused the weird channel allocation site error
        // Solution -> call it once for the whole method
        Firestore fs = db();
        String contents = readLocalShows();

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
                for (Seat s: seatsList) {
                    fs.collection(localShowCollectionName).document(showId.toString()).collection(seatsCollectionName).document(s.getSeatId().toString()).set(s).get();
                }
                // db().collection(localShowCollectionName).document(showId.toString()).collection(seatsCollectionName).document("seats").set(seatsMap).get();
                fs.collection(localShowCollectionName).document(showId.toString()).collection(timesCollectionName).document(timesCollectionName).set(timesMap).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    private static String readLocalShows()  {
        String fileContents = null;
        String classPath = new File("").getAbsolutePath();
        try {
            fileContents = Files.readString(Path.of(classPath + "/src/main/resources/data.json"));
        } catch (IOException e) {
            System.out.println(e);
        }
        return fileContents;
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
    public boolean isProduction() {
        return Objects.equals(System.getenv("GAE_ENV"), "standard");
    }

    @Bean
    public String projectId() {
        return "demo-distributed-systems-kul";
    }

    public String get_API_KEY() { return API_KEY; }

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
        // Source: https://gist.github.com/ryanpbrewster/aef2a5c411a074819c8d7b67be80621c
        return FirestoreOptions.newBuilder()
                .setProjectId("demo-distributed-systems-kul")
                .setChannelProvider(
                        InstantiatingGrpcChannelProvider.newBuilder().setEndpoint("localhost:8084")
                                .setChannelConfigurator(
                                        ManagedChannelBuilder::usePlaintext
                                ).build())
                .setCredentialsProvider(FixedCredentialsProvider.create(new FakeCreds()))
                .build()
                .getService();
    }

    @Bean
    HashMap<String, ICompany> companies() {
        HashMap<String, ICompany> companyMap = new HashMap<>();
        companyMap.put(rCompanyName, new RemoteCompany(rCompanyName, db()));
        companyMap.put(urCompanyName, new RemoteCompany(urCompanyName, db()));
        companyMap.put(Application.localCompanyName, new LocalCompany(db()));
        return companyMap;
    }
}
