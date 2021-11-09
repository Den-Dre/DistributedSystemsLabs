package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.*;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jetty.server.RequestLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import java.time.LocalDateTime;
import java.util.*;

@Component
public class Model {

    @Autowired
    private final WebClient.Builder builder = WebClient.builder();

    private final String API_KEY = "wCIoTqec6vGJijW2meeqSokanZuqOL";

    /**
     * Fetch all shows from the API-endpoint.
     *
     * @return A List of {@link Show} objects.
     */
    public List<Show> getShows() {

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

        return List.copyOf(shows);
    }

    /**
     * Fetch a {@link Show} given the company name and the showId.
     *
     * @param company String representing a company (e.g. "reliabletheatrecompany")
     * @param showId The id of a show.
     * @return A {@link Show} object.
     */
    public Show getShow(String company, UUID showId) {
        return builder
                .baseUrl("https://reliabletheatrecompany.com/")
                .build()
                .get()
                .uri(builder1 -> builder1
                        .pathSegment("shows/{showId}")
                        .queryParam("key", API_KEY)
                        .build(showId.toString()))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Show>() {})
                .block();
    }

    public List<LocalDateTime> getShowTimes(String company, UUID showId) {
        // TODO: return a list with all possible times for the given show
        return new ArrayList<>();
    }

    public List<Seat> getAvailableSeats(String company, UUID showId, LocalDateTime time) {
        // TODO: return all available seats for a given show and time
        return new ArrayList<>();
    }

    public Seat getSeat(String company, UUID showId, UUID seatId) {
        // TODO: return the given seat
        return null;
    }

    public Ticket getTicket(String company, UUID showId, UUID seatId) {
        // TODO: return the ticket for the given seat
        return null;
    }

    public List<Booking> getBookings(String customer) {
        // TODO: return all bookings from the customer
        return new ArrayList<>();
    }

    public List<Booking> getAllBookings() {
        // TODO: return all bookings
        return new ArrayList<>();
    }

    public Set<String> getBestCustomers() {
        // TODO: return the best customer (highest number of tickets, return all of them if multiple customers have an equal amount)
        return null;
    }

    public void confirmQuotes(List<Quote> quotes, String customer) {
        // TODO: reserve all seats for the given quotes
    }
}
