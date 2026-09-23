package com.driveflow.demo_driveflow.booking;

import com.driveflow.demo_driveflow.payment.Invoice;
import com.driveflow.demo_driveflow.payment.InvoiceRepository;
import com.driveflow.demo_driveflow.users.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    private void ensureInvoiceForBooking(Booking booking) {
        if (booking == null || booking.getBookingId() == null) return;
        try {
            if (invoiceRepository.findByBooking(booking).isEmpty()) {
                Invoice invoice = new Invoice();
                invoice.setBooking(booking);
                invoice.setInvoiceDate(LocalDate.now());
                BigDecimal rate = booking.getChargedRate() != null ? booking.getChargedRate() : BigDecimal.valueOf(75.00);
                int duration = booking.getDuration() != null ? booking.getDuration() : 1;
                invoice.setRentalAmt(rate.multiply(BigDecimal.valueOf(duration > 0 ? duration : 1)));
                invoice.setLateFee(BigDecimal.ZERO);
                invoice.setStatus("UNPAID");
                invoiceRepository.save(invoice);
            }
        } catch (Exception e) {
            // Unique constraint safeguard
        }
    }

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
        Booking saved = bookingRepository.save(booking);
        ensureInvoiceForBooking(saved);
        return saved;
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
    public void approveBooking(Long id) {
        Booking booking = getBookingById(id);
        booking.setStatus("CONFIRMED");
        Booking saved = bookingRepository.save(booking);
        ensureInvoiceForBooking(saved);
    }

    @Override
    public void cancelBooking(Long id) {
        Booking booking = getBookingById(id);
        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);
    }
}
