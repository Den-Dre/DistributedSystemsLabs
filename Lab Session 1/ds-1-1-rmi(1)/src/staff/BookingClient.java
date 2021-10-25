package staff;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;
import java.util.Set;
import hotel.IBookingManager;

import hotel.*;

public class BookingClient extends AbstractScriptedSimpleTest {

	public static final String _defaultBookingCompanyName = "HaboHotel";
	public static final String _defaultBeginDate = "07/10/2011";
	public static final String _defaultEndDate = "09/10/2011";

	private String bookingManagerName;
	private Date begin;
	private Date end;

	private IBookingManager bm = null;

	public static void main(String[] args) throws Exception {
		if (System.getSecurityManager() != null)
			System.setSecurityManager(null);

		if (args.length == 0) {
			args = new String[] {_defaultBookingCompanyName, _defaultBeginDate, _defaultEndDate };
		} else if (args.length != 3) {
			System.err.println("This program requires 3 arguments: "
					+ "booking company name - begin date (dd/mm/yyyy) - end date (dd/mm/yyyy).");
			System.exit(0);
		}

		// Run booking rental company client
		BookingClient client = new BookingClient(args);
		client.execute(); // Set up our registry and stuff
		client.run();
	}

	/***************
	 * CONSTRUCTOR *
	 ***************/
	public BookingClient(String[] args) {
		bookingManagerName = args[0];

		Calendar c = Calendar.getInstance();

		Scanner s = new Scanner(args[1]).useDelimiter("/");
		int day = s.nextInt();
		int month = s.nextInt() - 1;
		int year = s.nextInt();

		// set begin date for booking
		c.set(year, month, day);
		begin = c.getTime();

		s = new Scanner(args[2]).useDelimiter("/");
		day = s.nextInt();
		month = s.nextInt() - 1;
		year = s.nextInt();

		// set end date for booking
		c.set(year, month, day);
		end = c.getTime();
	}

	public void execute() {
		try {
			// get booking rental company
			Registry registry = LocateRegistry.getRegistry();
			IBookingManager manager = (IBookingManager) registry.lookup(bookingManagerName);
			bm = (IBookingManager) manager;
			System.out.println("Booking Company " + manager.getName() + " found.");

			// Print all free car types in the given period.
//			Set<Room> rooms = manager.getFreeRooms(begin, end);
//			System.out.println("List of all free rooms in the period " + begin + " - " + end + ": ");
//			for (Room room : rooms)
//				System.out.println("\t" + room.toString());

		} catch (NotBoundException ex) {
			System.err.println("Could not find Booking company with given name.");
		} catch (RemoteException ex) {
			System.err.println(ex.getMessage());
		}
	}

	@Override
	public boolean isRoomAvailable(Integer roomNumber, LocalDate date) {
		//Implement this method
		return true;
	}

	/**
	 * Add a booking for the given guest in the given room on the given
	 * date. If the room is not available, throw a suitable Exception.
	 *
	 * @param bookingDetail
	 */
	@Override
	protected void addBooking(BookingDetail bookingDetail) throws Exception {
		bm.addBooking(bookingDetail);
	}

	@Override
	public Set<Integer> getAvailableRooms(LocalDate date) throws RemoteException {
		return bm.getAvailableRooms(date);
	}

	@Override
	public Set<Integer> getAllRooms() throws RemoteException {
		return bm.getAllRooms();
	}

	@Override
	protected Room getDummyRoom() throws Exception {
		return bm.getDummyRoom();
	}
}






















