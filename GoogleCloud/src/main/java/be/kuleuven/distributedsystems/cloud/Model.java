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
        var show = builder
                .baseUrl("https://reliabletheatrecompany.com/")
                .build()
                .get()
                .uri(builder -> builder
                        .pathSegment("shows/{showId}")
                        .queryParam("key", API_KEY)
                        .build(showId.toString()))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Show>() {})
                .block();
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
        var times = builder
                .baseUrl("https://reliabletheatrecompany.com/")
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
        // System.out.println(times);
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
        var seats = builder
                .baseUrl("https://reliabletheatrecompany.com/")
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
        return List.copyOf(seats);
    }

    public Seat getSeat(String company, UUID showId, UUID seatId) {

        // HELP!!: mega inefficient omdat ge moet loopen over alle times
        var showTimes = getShowTimes(company, showId);
        for (LocalDateTime showTime: showTimes) {
            var possibleSeats = getAvailableSeats(company, showId, showTime);
            for (Seat seat: possibleSeats) {
                if (seat.getSeatId().equals(seatId)) {
                    return seat;
                }
            }
        }
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
