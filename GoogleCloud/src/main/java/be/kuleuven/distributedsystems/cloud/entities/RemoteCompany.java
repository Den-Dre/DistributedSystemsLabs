package be.kuleuven.distributedsystems.cloud.entities;

import com.google.cloud.firestore.Firestore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class RemoteCompany implements ICompany {
    protected String company;
    private static final int RETRY_DELAY = 1000;

    public RemoteCompany(String company, Firestore db, WebClient.Builder webClientBuilder) {
        this.company = company;
        this.db = db;
        this.builder = webClientBuilder;
    }

    @Autowired
    private final WebClient.Builder builder;

    private final static String API_KEY = "wCIoTqec6vGJijW2meeqSokanZuqOL";

    private Firestore db;

    public List<Show> getShows() {
        boolean succes = false;
        Collection<Show> shows = null;
        while (!succes) {
            try {
                System.out.println(String.format("https://%s.com/", company));
                shows = builder
                        .baseUrl(String.format("https://%s.com/", company))
                        .build()
                        .get()
                        .uri(builder -> builder
                                .pathSegment("/shows")
                                .queryParam("key", API_KEY)
                                .build())
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<CollectionModel<Show>>() {})
                        .block()
                        .getContent();
                System.out.println("shows: " + shows);
                shows.forEach(e -> System.out.println(e.getName()));
                succes = true;
            } catch (Exception e) {
                System.out.println(e);
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY);
                } catch (InterruptedException e2) {
                    System.out.println(e2);
                }

            }
        }
        return new ArrayList<>(shows);
    }

    /**
     * Fetch a {@link Show} given the company name and the showId.
     *
     * @param showId The id of a show.
     * @return A {@link Show} object.
     */
    public Show getShow(UUID showId) {
        boolean succes = false;
        Show show = null;
        while (!succes) {
            try {
                show = builder
                        .baseUrl(String.format("https://%s.com/", company))
                        .build()
                        .get()
                        .uri(builder -> builder
                                .pathSegment("shows/{showId}")
                                .queryParam("key", API_KEY)
                                .build(showId.toString()))
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Show>() {})
                        .block();
                succes = true;
            } catch (Exception e) {
                System.out.println(e);
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY);
                } catch (InterruptedException e2) {
                    System.out.println(e2);
                }

            }
        }

        return show;
    }

    /**
     * Get a list of all the times a show is played.
     *
     * @param showId The id of the show to get the times of.
     * @return A List of {@link LocalDateTime} objects.
     */
    public List<LocalDateTime> getShowTimes(UUID showId) {
        boolean succes = false;
        Collection<LocalDateTime> times  = null;
        while (!succes) {
            try {
                times = builder
                        .baseUrl(String.format("https://%s.com/", company))
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
                succes = true;
            } catch (Exception e) {
                System.out.println(e);
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY);
                } catch (InterruptedException e2) {
                    System.out.println(e2);
                }

            }
        }
        return List.copyOf(times);
    }

    /**
     * Get a list of available seats for a given show at a given time at a given company.
     *
     * @param showId The id of the show to get the available seats of.
     * @param time The time at which to look for available seats.
     * @return A list of available {@link Seat} objects.
     */
    public List<Seat> getAvailableSeats(UUID showId, LocalDateTime time) {
        boolean succes = false;
        Collection<Seat> seats  = null;
        while (!succes) {
            try {
                seats = builder
                        .baseUrl(String.format("https://%s.com/", company))
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
                succes = true;
            } catch (Exception e) {
                System.out.println(e);
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY);
                } catch (InterruptedException e2) {
                    System.out.println(e2);
                }

            }
        }
        return List.copyOf(seats);
    }
    public Seat getSeat(UUID showId, UUID seatId) {
        boolean succes = false;
        Seat seat  = null;
        while (!succes) {
            try {
                seat = builder
                        .baseUrl(String.format("https://%s.com/", company))
                        .build()
                        .get()
                        .uri(builder -> builder
                                .pathSegment("shows/{showId}/seats/{seatId}")
                                .queryParam("key", API_KEY)
                                .build(showId.toString(), seatId.toString()))
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Seat>() {})
                        .block();
                succes = true;
            } catch (Exception e) {
                System.out.println(e);
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException e2) {
                    System.out.println(e2);
                }

            }
        }

        return seat;
    }

}
