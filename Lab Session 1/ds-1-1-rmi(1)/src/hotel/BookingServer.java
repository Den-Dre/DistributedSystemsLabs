package hotel;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BookingServer {

    private static final String _rentalCompanyName = "HaboHotel";
    private static final Logger logger = Logger.getLogger(BookingServer.class.getName());

    public static void main(String[] args) throws BookingException, RemoteException {

        // set security manager if non existent
        if(System.getSecurityManager() != null)
            System.setSecurityManager(null);

        // create Booking company

        IBookingManager bookingManager = new BookingManager();
        Set<Integer> rooms = bookingManager.getAllRooms();

        // locate registry
        Registry registry = null;
        try {
            registry = LocateRegistry.getRegistry();
        } catch(RemoteException e) {
            logger.log(Level.SEVERE, "Could not locate RMI registry.");
            System.exit(-1);
        }

        // register Booking company
        IBookingManager stub;
        try {
            stub = (IBookingManager) UnicastRemoteObject.exportObject(bookingManager, 0);
            registry.rebind(_rentalCompanyName, stub);
            logger.log(Level.INFO, "<{0}> Booking Company {0} is registered.", _rentalCompanyName);
        } catch(RemoteException e) {
            logger.log(Level.SEVERE, "<{0}> Could not get stub bound of Booking Company {0}.", _rentalCompanyName);
            e.printStackTrace();
            System.exit(-1);
        }
    }

}
