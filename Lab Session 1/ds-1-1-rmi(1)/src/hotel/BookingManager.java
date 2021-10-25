package hotel;

import java.time.LocalDate;
import java.util.*;


public class BookingManager implements IBookingManager {

	private final Room[] rooms;
	private final String bookingManagerName = "HaboHotel";

	public BookingManager() {
		this.rooms = initializeRooms();
	}

	public Set<Integer> getAllRooms() {
		Set<Integer> allRooms = new HashSet<Integer>();
		Iterable<Room> roomIterator = Arrays.asList(rooms);
		for (Room room : roomIterator) {
			allRooms.add(room.getRoomNumber());
		}
		return allRooms;
	}

	public boolean isRoomAvailable(Integer roomNumber, LocalDate date) {
		for (Room room: rooms) {
			if (room.getRoomNumber().equals(roomNumber)) {
				for (BookingDetail detail: room.getBookings()) {
					if (detail.getDate().equals(date))
						return false;
				}
			}
		}
		return true;
	}

	public synchronized void  addBooking(BookingDetail bookingDetail) {
		for (Room room: rooms) {
			if (room.getRoomNumber().equals(bookingDetail.getRoomNumber())) {
				try {
					room.addBooking(bookingDetail);

				} catch (BookingException e) {
					System.out.println("Room was already booked!");
				}
			}
		}
	}


	@Override
	public String getName() {
		return bookingManagerName;
	}

	public Set<Integer> getAvailableRooms(LocalDate date) {
		Set<Integer> availableRooms = new HashSet<>();
		for (Room room: rooms) {
			if (isRoomAvailable(room.getRoomNumber(), date))
				availableRooms.add(room.getRoomNumber());
		}
		return availableRooms;
	}

	@Override
	public Room getDummyRoom() {
		return new Room(123);
	}

	private static Room[] initializeRooms() {
		Room[] rooms = new Room[4];
		rooms[0] = new Room(101);
		rooms[1] = new Room(102);
		rooms[2] = new Room(201);
		rooms[3] = new Room(203);
		return rooms;
	}
}
