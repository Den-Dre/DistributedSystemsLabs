package be.kuleuven.distributedsystems.cloud.entities;

import com.google.cloud.firestore.Firestore;
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

//@Component
public class RemoteCompany implements ICompany {
    protected String company;
    private static final int RETRY_DELAY = 1000;

    public RemoteCompany(String company, Firestore db) {
        this.company = company;
        this.db = db;
    }

    private final static String API_KEY = "wCIoTqec6vGJijW2meeqSokanZuqOL";

    private Firestore db;

    public boolean isLocal() {return false;}

    public List<Show> getShows(WebClient.Builder builder) {
        boolean succes = false;
        Collection<Show> shows = null;
        while (!succes) {
            try {
                shows = builder
                        .baseUrl(String.format("https://%s/", company))
                        .build()
                        .get()
                        .uri(builder2 -> builder2
                                .pathSegment("/shows")
                                .queryParam("key", API_KEY)
                                .build())
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<CollectionModel<Show>>() {})
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
        return new ArrayList<>(shows);
    }

    /**
     * Fetch a {@link Show} given the company name and the showId.
     *
     * @param showId The id of a show.
     * @return A {@link Show} object.
     */
    public Show getShow(UUID showId, WebClient.Builder builder) {
        boolean succes = false;
        Show show = null;
        while (!succes) {
            try {
                show = builder
                        .baseUrl(String.format("https://%s/", company))
                        .build()
                        .get()
                        .uri(builder2 -> builder2
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
    public List<LocalDateTime> getShowTimes(UUID showId, WebClient.Builder builder) {
        boolean succes = false;
        Collection<LocalDateTime> times  = null;
        while (!succes) {
            try {
                times = builder
                        .baseUrl(String.format("https://%s/", company))
                        .build()
                        .get()
                        .uri(builder2 -> builder2
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
    public List<Seat> getAvailableSeats(UUID showId, LocalDateTime time, WebClient.Builder builder) {
        boolean succes = false;
        Collection<Seat> seats  = null;
        while (!succes) {
            try {
                seats = builder
                        .baseUrl(String.format("https://%s/", company))
                        .build()
                        .get()
                        .uri(builder2 -> builder2
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
    public Seat getSeat(UUID showId, UUID seatId, WebClient.Builder builder) {
        boolean succes = false;
        Seat seat  = null;
        while (!succes) {
            try {
                seat = builder
                        .baseUrl(String.format("https://%s/", company))
                        .build()
                        .get()
                        .uri(builder2 -> builder2
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

    public List<Ticket> confirmQuotes(List<Quote> quotes, String customer, String api_key, WebClient.Builder builder) {
        List<Ticket> tickets = new ArrayList<>();
        for (Quote q: quotes) {
            var ticket = builder
                    .baseUrl(String.format("https://%s/", q.getCompany()))
                    .build()
                    .put()
                    .uri(builder2 -> builder2
                            .pathSegment("shows/{showId}/seats/{seatId}/ticket")
                            .queryParam("customer", customer)
                            .queryParam("key", api_key.replaceAll("\"", ""))
                            .build(q.getShowId().toString(), q.getSeatId().toString()))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Ticket>() {
                    })
                    .block();
            tickets.add(ticket);
        }
        return tickets;
    }

    // Remove made bookings due to a duplicate booking being present in the list
    public void undoBooking(Ticket t, String API_KEY, WebClient.Builder builder) {
        var ticket = builder
                .baseUrl(String.format("https://%s/", t.getCompany()))
                .build()
                .delete()
                .uri(builder2 -> builder2
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
