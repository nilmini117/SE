package com.driveflow.demo_driveflow.booking;

import com.driveflow.demo_driveflow.users.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Override
    public List<Booking> getBookingsByCustomer(Customer customer) {
        if (customer == null) return List.of();
        return bookingRepository.findByCustomer(customer);
    }


    @Override
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));
    }

    @Override
    public Booking createBooking(Booking booking) {
        booking.setStatus("PENDING");
        return bookingRepository.save(booking);
    }

    @Override
    public Booking updateBooking(Long id, Booking updatedBooking) {
        Booking existing = getBookingById(id);
        if (updatedBooking.getBookingDate() != null) {
            existing.setBookingDate(updatedBooking.getBookingDate());
        }
        if (updatedBooking.getEndDate() != null) {
            existing.setEndDate(updatedBooking.getEndDate());
        }
        if (updatedBooking.getQuantity() != null) {
            existing.setQuantity(updatedBooking.getQuantity());
        }
        if (updatedBooking.getChargedRate() != null) {
            existing.setChargedRate(updatedBooking.getChargedRate());
        }
        if (updatedBooking.getStatus() != null) {
            existing.setStatus(updatedBooking.getStatus());
        }
        if (updatedBooking.getDuration() != null) {
            existing.setDuration(updatedBooking.getDuration());
        }
        if (updatedBooking.getVehicle() != null) {
            existing.setVehicle(updatedBooking.getVehicle());
        }
        if (updatedBooking.getCustomer() != null) {
            existing.setCustomer(updatedBooking.getCustomer());
        }
        return bookingRepository.save(existing);
    }

    @Override
    public void cancelBooking(Long id) {
        Booking booking = getBookingById(id);
        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);
    }
}
