package com.driveflow.demo_driveflow.booking;

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
        existing.setBookingDate(updatedBooking.getBookingDate());
        existing.setEndDate(updatedBooking.getEndDate());
        existing.setQuantity(updatedBooking.getQuantity());
        existing.setChargedRate(updatedBooking.getChargedRate());
        existing.setStatus(updatedBooking.getStatus());
        existing.setDuration(updatedBooking.getDuration());
        existing.setVehicle(updatedBooking.getVehicle());
        return bookingRepository.save(existing);
    }

    @Override
    public void cancelBooking(Long id) {
        Booking booking = getBookingById(id);
        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);
    }
}
