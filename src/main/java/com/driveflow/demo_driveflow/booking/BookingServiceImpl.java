package com.driveflow.demo_driveflow.booking;

import com.driveflow.demo_driveflow.payment.Invoice;
import com.driveflow.demo_driveflow.payment.InvoiceRepository;
import com.driveflow.demo_driveflow.users.Customer;
import com.driveflow.demo_driveflow.vehicle.Vehicle;
import com.driveflow.demo_driveflow.vehicle.VehicleRepository;
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

    @Autowired
    private VehicleRepository vehicleRepository;

    private void ensureInvoiceForBooking(Booking booking) {
        if (booking == null || booking.getBookingId() == null) return;
        try {
            if (invoiceRepository.findByBooking(booking).isEmpty()) {
                Invoice invoice = new Invoice();
                invoice.setBooking(booking);
                invoice.setInvoiceDate(LocalDate.now());
                BigDecimal rentalAmt = booking.getChargedRate();
                if (rentalAmt == null) {
                    int duration = booking.getDuration() != null ? booking.getDuration() : 1;
                    rentalAmt = BigDecimal.valueOf((duration > 0 ? duration : 1) * 3000.00);
                }
                invoice.setRentalAmt(rentalAmt);
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
        return bookingRepository.findAllSortedWithPendingFirst();
    }

    @Override
    public List<Booking> getBookingsByStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return bookingRepository.findAllSortedWithPendingFirst();
        }
        return bookingRepository.findByStatusOrderByBookingDateAsc(status.trim().toUpperCase());
    }

    @Override
    public java.util.Map<String, Long> getBookingStatusCounts() {
        java.util.Map<String, Long> counts = new java.util.LinkedHashMap<>();
        long total = bookingRepository.count();
        long pending = bookingRepository.countByStatusIgnoreCase("PENDING");
        long confirmed = bookingRepository.countByStatusIgnoreCase("CONFIRMED");
        long completed = bookingRepository.countByStatusIgnoreCase("COMPLETED");
        long cancelled = bookingRepository.countByStatusIgnoreCase("CANCELLED");
        counts.put("ALL", total);
        counts.put("PENDING", pending);
        counts.put("CONFIRMED", confirmed);
        counts.put("COMPLETED", completed);
        counts.put("CANCELLED", cancelled);
        return counts;
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
        booking.setStaffMessage("Your booking has been confirmed by our team.");
        Booking saved = bookingRepository.save(booking);
        ensureInvoiceForBooking(saved);
    }

    @Override
    public void declineBooking(Long id, String reason) {
        Booking booking = getBookingById(id);
        booking.setStatus("CANCELLED");
        booking.setStaffMessage(reason != null ? reason.trim() : "Booking request was declined by staff.");

        // Release assigned vehicle back to AVAILABLE if it was BOOKED
        if (booking.getVehicle() != null) {
            Vehicle vehicle = booking.getVehicle();
            if ("BOOKED".equalsIgnoreCase(vehicle.getStatus())) {
                vehicle.setStatus("AVAILABLE");
                vehicleRepository.save(vehicle);
            }
        }

        // Cancel unpaid invoice if present
        try {
            invoiceRepository.findByBooking(booking).ifPresent(inv -> {
                if (!"PAID".equalsIgnoreCase(inv.getStatus())) {
                    inv.setStatus("CANCELLED");
                    invoiceRepository.save(inv);
                }
            });
        } catch (Exception ignored) {}

        bookingRepository.save(booking);
    }

    @Override
    public void cancelBooking(Long id) {
        Booking booking = getBookingById(id);
        booking.setStatus("CANCELLED");
        booking.setStaffMessage(null); // Customer self-cancellation: no staff message
        
        // Release assigned vehicle back to AVAILABLE if it was BOOKED
        if (booking.getVehicle() != null) {
            Vehicle vehicle = booking.getVehicle();
            if ("BOOKED".equalsIgnoreCase(vehicle.getStatus())) {
                vehicle.setStatus("AVAILABLE");
                vehicleRepository.save(vehicle);
            }
        }

        // Cancel unpaid invoice if present
        try {
            invoiceRepository.findByBooking(booking).ifPresent(inv -> {
                if (!"PAID".equalsIgnoreCase(inv.getStatus())) {
                    inv.setStatus("CANCELLED");
                    invoiceRepository.save(inv);
                }
            });
        } catch (Exception ignored) {}

        bookingRepository.save(booking);
    }
}
