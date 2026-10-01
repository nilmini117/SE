package com.driveflow.demo_driveflow.booking;

import com.driveflow.demo_driveflow.booking.exception.ActiveBookingLimitExceededException;
import com.driveflow.demo_driveflow.booking.exception.BookingCancellationNotAllowedException;
import com.driveflow.demo_driveflow.booking.exception.BookingImmutabilityException;
import com.driveflow.demo_driveflow.booking.exception.BranchSelectionRequiredException;
import com.driveflow.demo_driveflow.booking.pricing.PricingBreakdown;
import com.driveflow.demo_driveflow.booking.pricing.PricingEngineService;
import com.driveflow.demo_driveflow.branch.Branch;
import com.driveflow.demo_driveflow.branch.BranchRepository;
import com.driveflow.demo_driveflow.payment.Invoice;
import com.driveflow.demo_driveflow.payment.InvoiceRepository;
import com.driveflow.demo_driveflow.users.Customer;
import com.driveflow.demo_driveflow.vehicle.Vehicle;
import com.driveflow.demo_driveflow.vehicle.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private PricingEngineService pricingEngineService;

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
        } catch (Exception ignored) {
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
    public Map<String, Long> getBookingStatusCounts() {
        Map<String, Long> counts = new java.util.LinkedHashMap<>();
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
    public long getActiveBookingCount(Long customerId) {
        if (customerId == null) return 0;
        return bookingRepository.countActiveBookingsByCustomerId(customerId);
    }

    @Override
    public boolean hasActiveBooking(Long customerId) {
        return getActiveBookingCount(customerId) > 0;
    }

    @Override
    public PricingBreakdown calculatePricing(Long vehicleId, Long branchId, LocalDate startDate, LocalDate endDate, String couponCode) {
        return pricingEngineService.calculatePricing(vehicleId, branchId, startDate, endDate, couponCode);
    }

    @Override
    @Transactional
    public Booking createBooking(Booking booking) {
        // 1. Mandatory Branch Selection:
        // The booking flow must mandate the user to select a specific pickup branch before validating availability.
        if (booking.getPickupBranch() == null || booking.getPickupBranch().getBranchId() == null) {
            throw new BranchSelectionRequiredException(
                    "Branch Selection Required: A specific pickup branch must be selected before validating vehicle availability."
            );
        }

        // Verify pickup branch exists in DB
        Branch pickupBranch = branchRepository.findById(booking.getPickupBranch().getBranchId())
                .orElseThrow(() -> new BranchSelectionRequiredException("Invalid pickup branch selected."));
        booking.setPickupBranch(pickupBranch);

        if (booking.getReturnBranch() == null || booking.getReturnBranch().getBranchId() == null) {
            booking.setReturnBranch(pickupBranch);
        } else {
            Branch returnBranch = branchRepository.findById(booking.getReturnBranch().getBranchId())
                    .orElse(pickupBranch);
            booking.setReturnBranch(returnBranch);
        }

        // 2. Strict Concurrency Limit:
        // Query the database to ensure the customer has 0 currently active bookings.
        // A single customer can only book exactly one vehicle at a time.
        // If an active booking exists, block the transaction and return a conflict error.
        if (booking.getCustomer() == null || booking.getCustomer().getSystemId() == null) {
            throw new IllegalArgumentException("Customer account is required to place a reservation.");
        }
        Long customerId = booking.getCustomer().getSystemId();
        long activeBookings = bookingRepository.countActiveBookingsByCustomerId(customerId);
        if (activeBookings > 0) {
            throw new ActiveBookingLimitExceededException(
                    "Active booking limit exceeded: Customer already has " + activeBookings +
                    " active booking(s). A single customer can only book exactly one vehicle at a time."
            );
        }

        // 3. Vehicle Availability & Branch Validation:
        if (booking.getVehicle() != null && booking.getVehicle().getVehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findById(booking.getVehicle().getVehicleId())
                    .orElseThrow(() -> new IllegalArgumentException("Selected vehicle not found with ID: " + booking.getVehicle().getVehicleId()));

            // Verify vehicle is stationed at the selected pickup branch
            if (vehicle.getBranch() != null && !vehicle.getBranch().getBranchId().equals(pickupBranch.getBranchId())) {
                throw new IllegalArgumentException(
                        "Vehicle '" + vehicle.getModel() + "' is not stationed at the selected pickup branch '" +
                        pickupBranch.getBranchName() + "'."
                );
            }

            // Verify vehicle status is AVAILABLE
            if (vehicle.getStatus() != null && !"AVAILABLE".equalsIgnoreCase(vehicle.getStatus())) {
                throw new IllegalStateException(
                        "Selected vehicle '" + vehicle.getModel() + "' is currently not available (status: " + vehicle.getStatus() + ")."
                );
            }

            // Check date overlap for active bookings
            LocalDate start = booking.getBookingDate() != null ? booking.getBookingDate() : LocalDate.now();
            LocalDate end = booking.getEndDate() != null ? booking.getEndDate() : start.plusDays(1);
            long overlapping = bookingRepository.countOverlappingActiveBookings(vehicle.getVehicleId(), start, end);
            if (overlapping > 0) {
                throw new IllegalStateException("Selected vehicle is already reserved for the chosen date interval.");
            }

            booking.setVehicle(vehicle);
        } else {
            throw new IllegalArgumentException("A vehicle must be selected from our park.");
        }

        // 4. Booking Dates & Duration:
        if (booking.getBookingDate() == null) {
            booking.setBookingDate(LocalDate.now());
        }
        if (booking.getEndDate() == null) {
            booking.setEndDate(booking.getBookingDate().plusDays(1));
        }
        if (booking.getEndDate().isBefore(booking.getBookingDate())) {
            booking.setEndDate(booking.getBookingDate());
        }
        long days = ChronoUnit.DAYS.between(booking.getBookingDate(), booking.getEndDate());
        int durationDays = days > 0 ? (int) days : 1;
        booking.setDuration(durationDays);
        booking.setQuantity(1); // Single vehicle constraint

        // 5. Pricing Engine:
        // Calculate the total cost by applying active seasonal promotions or coupon discount
        if (booking.getChargedRate() == null || booking.getChargedRate().compareTo(BigDecimal.ZERO) <= 0) {
            PricingBreakdown breakdown = pricingEngineService.calculatePricing(
                    booking.getVehicle().getVehicleId(),
                    pickupBranch.getBranchId(),
                    booking.getBookingDate(),
                    booking.getEndDate(),
                    null
            );
            booking.setChargedRate(breakdown.getFinalTotalCost());
        }

        // 6. Payment Gate:
        // Do not trigger the payment gateway during the initial booking submission.
        // Payment fields should only unlock after the booking status transitions to APPROVED by staff.
        booking.setStatus("PENDING");

        return bookingRepository.save(booking);
    }

    @Override
    public Booking updateBooking(Long id, Booking updatedBooking) {
        // Strict Immutability Constraint:
        // Once the booking payload is submitted and the record is created,
        // lock the record from any customer-initiated edits (updates to dates, vehicles, or branches are strictly prohibited).
        throw new BookingImmutabilityException(
                "Booking records are immutable once submitted. Customer-initiated edits to dates, vehicles, or branches are strictly prohibited."
        );
    }

    @Override
    public void approveBooking(Long id) {
        Booking booking = getBookingById(id);
        booking.setStatus("CONFIRMED"); // Also treated as APPROVED
        booking.setStaffMessage("Your booking has been approved by our team. Payment gate is now unlocked.");
        Booking saved = bookingRepository.save(booking);
        // Unlocks invoice so customer can proceed with payment
        ensureInvoiceForBooking(saved);
    }

    @Override
    public Booking updateBookingStatus(Long id, String newStatus) {
        Booking booking = getBookingById(id);

        // Constraint: Staff can only edit status if NOT paid by customer
        Optional<Invoice> invoiceOpt = invoiceRepository.findByBooking(booking);
        if (invoiceOpt.isPresent() && "PAID".equalsIgnoreCase(invoiceOpt.get().getStatus())) {
            throw new IllegalStateException(
                    "Booking status cannot be edited: Customer has already paid for booking #BK-" + id + "."
            );
        }

        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("New booking status must be specified.");
        }

        String cleanStatus = newStatus.trim().toUpperCase();
        booking.setStatus(cleanStatus);

        if ("CONFIRMED".equalsIgnoreCase(cleanStatus) || "APPROVED".equalsIgnoreCase(cleanStatus)) {
            booking.setStatus("CONFIRMED");
            booking.setStaffMessage("Booking status updated to confirmed/approved by staff.");
            if (booking.getVehicle() != null) {
                Vehicle v = booking.getVehicle();
                v.setStatus("BOOKED");
                vehicleRepository.save(v);
            }
            ensureInvoiceForBooking(booking);
        } else if ("COMPLETED".equalsIgnoreCase(cleanStatus)) {
            booking.setStaffMessage("Rental marked as completed by staff.");
            if (booking.getVehicle() != null) {
                Vehicle v = booking.getVehicle();
                v.setStatus("AVAILABLE");
                vehicleRepository.save(v);
            }
        } else if ("CANCELLED".equalsIgnoreCase(cleanStatus)) {
            booking.setStaffMessage("Booking cancelled by staff.");
            if (booking.getVehicle() != null) {
                Vehicle v = booking.getVehicle();
                v.setStatus("AVAILABLE");
                vehicleRepository.save(v);
            }
            if (invoiceOpt.isPresent()) {
                Invoice inv = invoiceOpt.get();
                if (!"PAID".equalsIgnoreCase(inv.getStatus())) {
                    inv.setStatus("CANCELLED");
                    invoiceRepository.save(inv);
                }
            }
        } else if ("PENDING".equalsIgnoreCase(cleanStatus)) {
            booking.setStaffMessage(null);
            if (booking.getVehicle() != null) {
                Vehicle v = booking.getVehicle();
                v.setStatus("AVAILABLE");
                vehicleRepository.save(v);
            }
        }

        return bookingRepository.save(booking);
    }

    @Override
    public void declineBooking(Long id, String reason) {
        Booking booking = getBookingById(id);

        // Strict Rule: Staff CANNOT decline an approved or confirmed booking
        if ("CONFIRMED".equalsIgnoreCase(booking.getStatus()) || "APPROVED".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException(
                    "Staff cannot decline an approved booking (#BK-" + id + "). Only pending bookings can be declined."
            );
        }

        // Strict Rule: Staff CANNOT decline a paid booking
        Optional<Invoice> invOpt = invoiceRepository.findByBooking(booking);
        if (invOpt.isPresent() && "PAID".equalsIgnoreCase(invOpt.get().getStatus())) {
            throw new IllegalStateException(
                    "Staff cannot decline a paid booking (#BK-" + id + ")."
            );
        }

        booking.setStatus("CANCELLED");
        booking.setStaffMessage(reason != null ? reason.trim() : "Booking request was declined by staff.");

        if (booking.getVehicle() != null) {
            Vehicle vehicle = booking.getVehicle();
            if ("BOOKED".equalsIgnoreCase(vehicle.getStatus())) {
                vehicle.setStatus("AVAILABLE");
                vehicleRepository.save(vehicle);
            }
        }

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
        String currentStatus = booking.getStatus();

        // Strict Cancellation State Check:
        // Customers can only trigger a cancellation if the booking status is PENDING (before staff approval).
        // If APPROVED (or CONFIRMED), disable the cancel action.
        if (currentStatus == null || !"PENDING".equalsIgnoreCase(currentStatus.trim())) {
            throw new BookingCancellationNotAllowedException(
                    "Cancellation prohibited: Customers can only cancel reservations in PENDING status (before staff approval). " +
                    "Booking #BK-" + id + " currently has status '" + currentStatus + "' and cannot be cancelled by customer."
            );
        }

        booking.setStatus("CANCELLED");
        booking.setStaffMessage("Cancelled by customer prior to staff approval.");

        if (booking.getVehicle() != null) {
            Vehicle vehicle = booking.getVehicle();
            if ("BOOKED".equalsIgnoreCase(vehicle.getStatus())) {
                vehicle.setStatus("AVAILABLE");
                vehicleRepository.save(vehicle);
            }
        }

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
