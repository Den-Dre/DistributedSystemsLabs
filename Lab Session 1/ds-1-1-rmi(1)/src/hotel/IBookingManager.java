package hotel;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.Set;

public interface IBookingManager extends Remote {
    Set<Integer> getAllRooms() throws RemoteException;

    boolean isRoomAvailable(Integer roomNumber, LocalDate date) throws RemoteException;

    void addBooking(BookingDetail bookingDetail) throws RemoteException;

    String getName() throws RemoteException;

    Set<Integer> getAvailableRooms(LocalDate date) throws RemoteException;

    /**
     * We've added this method to test what the effect is of sending an object without serializing it.
     * IT turns our that we get a Marhalling error if we try to return a Room without serializing.
     *
     * Thus, the {@link Room} class must extend the {@link java.io.Serializable} interface.
     *
     * @return A dummy room
     * @throws RemoteException:
     */
    Room getDummyRoom() throws RemoteException;
}
