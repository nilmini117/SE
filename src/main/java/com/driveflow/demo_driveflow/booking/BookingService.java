package com.driveflow.demo_driveflow.booking;

import com.driveflow.demo_driveflow.users.Customer;
import java.util.List;
import java.util.Map;

public interface BookingService {
    List<Booking> getAllBookings();
    List<Booking> getBookingsByCustomer(Customer customer);
    List<Booking> getBookingsByStatus(String status);
    Map<String, Long> getBookingStatusCounts();
    Booking getBookingById(Long id);
    Booking createBooking(Booking booking);
    Booking updateBooking(Long id, Booking booking);
    void approveBooking(Long id);
    void declineBooking(Long id, String reason);
    void cancelBooking(Long id);
}

