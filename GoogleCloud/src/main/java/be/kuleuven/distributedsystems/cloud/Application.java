package be.kuleuven.distributedsystems.cloud;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.auth.Credentials;
import com.google.cloud.ServiceOptions;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.v1.FirestoreClient;
import com.google.cloud.firestore.v1.FirestoreSettings;
import io.grpc.ManagedChannelBuilder;
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

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
@SpringBootApplication
public class Application {
    public static final String TOPIC = "ABCDEFGH";
    public static Firestore firestore;

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        System.setProperty("server.port", System.getenv().getOrDefault("PORT", "8080"));
        System.out.println("Running at port " + System.getenv().getOrDefault("PORT", "8080"));

        // Apache JSP scans by default all JARs, which is not necessary, so disable it
        System.setProperty(org.apache.tomcat.util.scan.Constants.SKIP_JARS_PROPERTY, "*.jar");
        System.setProperty(org.apache.tomcat.util.scan.Constants.SCAN_JARS_PROPERTY, "taglibs-standard-spec-*.jar,taglibs-standard-impl-*.jar");
//        firestore = FirestoreOptions.getDefaultInstance().toBuilder()
//                .setProjectId("demo-distributed-systems-kul")
//                .setHost("localhost:8084")
//                .setCredentials(new FirestoreOptions.EmulatorCredentials())
//                .setCredentialsProvider(FixedCredentialsProvider.create(new FirestoreOptions.EmulatorCredentials()))
//                .build()
//                .getService();

        // Start Spring Boot application
        ApplicationContext context = SpringApplication.run(Application.class, args);
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

}
