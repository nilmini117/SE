package com.driveflow.demo_driveflow.booking;

import com.driveflow.demo_driveflow.users.Customer;
import java.util.List;

public interface BookingService {
    List<Booking> getAllBookings();
    List<Booking> getBookingsByCustomer(Customer customer);
    Booking getBookingById(Long id);
    Booking createBooking(Booking booking);
    Booking updateBooking(Long id, Booking booking);
    void approveBooking(Long id);
    void cancelBooking(Long id);
}

