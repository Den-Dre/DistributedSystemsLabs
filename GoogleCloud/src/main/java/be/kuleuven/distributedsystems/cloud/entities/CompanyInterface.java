package be.kuleuven.distributedsystems.cloud.entities;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CompanyInterface {
    List<Show> getShows();

    Show getShow(UUID showId);

    List<LocalDateTime> getShowTimes(UUID showId);

    List<Seat> getAvailableSeats(UUID showId, LocalDateTime time);

    Seat getSat(UUID showId, UUID seatId);
}
