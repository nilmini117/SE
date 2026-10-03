package com.driveflow.demo_driveflow.booking;

import com.driveflow.demo_driveflow.booking.pricing.PricingBreakdown;
import com.driveflow.demo_driveflow.users.Customer;

import java.time.LocalDate;
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
    Booking updateBookingStatus(Long id, String newStatus);
    void approveBooking(Long id);
    void declineBooking(Long id, String reason);
    void cancelBooking(Long id);
    Booking returnVehicle(Long bookingId);

    long getActiveBookingCount(Long customerId);
    boolean hasActiveBooking(Long customerId);
    List<Booking> getCompletedBookingsForCustomer(Long customerId);
    Booking getLastCompletedBookingForCustomer(Long customerId);
    boolean hasCompletedBooking(Long customerId);
    PricingBreakdown calculatePricing(Long vehicleId, Long branchId, LocalDate startDate, LocalDate endDate, String couponCode);
}
