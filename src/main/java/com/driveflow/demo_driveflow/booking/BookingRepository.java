package com.driveflow.demo_driveflow.booking;

import com.driveflow.demo_driveflow.users.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByStatus(String status);

    List<Booking> findByStatusOrderByBookingDateAsc(String status);

    @Query("SELECT b FROM Booking b ORDER BY CASE WHEN UPPER(b.status) = 'PENDING' THEN 1 WHEN UPPER(b.status) = 'CONFIRMED' THEN 2 WHEN UPPER(b.status) = 'COMPLETED' THEN 3 ELSE 4 END, b.bookingDate DESC")
    List<Booking> findAllSortedWithPendingFirst();

    @Query("SELECT b FROM Booking b WHERE b.customer.systemId = :customerId")
    List<Booking> findByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT b FROM Booking b WHERE b.customer.systemId = :customerId ORDER BY CASE WHEN UPPER(b.status) = 'PENDING' THEN 1 WHEN UPPER(b.status) = 'CONFIRMED' THEN 2 WHEN UPPER(b.status) = 'COMPLETED' THEN 3 ELSE 4 END, b.bookingDate DESC")
    List<Booking> findByCustomerIdSorted(@Param("customerId") Long customerId);

    long countByStatusIgnoreCase(String status);

    /**
     * Strict Concurrency Limit Check:
     * Counts currently active bookings (PENDING, CONFIRMED, or APPROVED).
     * A customer can only hold 0 active bookings to proceed with a new booking.
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.customer.systemId = :customerId AND UPPER(b.status) IN ('PENDING', 'CONFIRMED', 'APPROVED')")
    long countActiveBookingsByCustomerId(@Param("customerId") Long customerId);

    @Query(value = "SELECT COUNT(*) FROM booking WHERE customer_id = :customerId AND UPPER(status) IN ('PENDING', 'CONFIRMED', 'APPROVED')", nativeQuery = true)
    long countActiveBookingsByCustomerIdNative(@Param("customerId") Long customerId);

    @Query("SELECT b FROM Booking b WHERE b.customer.systemId = :customerId AND UPPER(b.status) IN ('PENDING', 'CONFIRMED', 'APPROVED')")
    List<Booking> findActiveBookingsByCustomerId(@Param("customerId") Long customerId);

    /**
     * Vehicle date overlap check for active reservations.
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.vehicle.vehicleId = :vehicleId " +
           "AND UPPER(b.status) IN ('PENDING', 'CONFIRMED', 'APPROVED') " +
           "AND (:startDate <= b.endDate AND :endDate >= b.bookingDate)")
    long countOverlappingActiveBookings(@Param("vehicleId") Long vehicleId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);

    default List<Booking> findByCustomer(Customer customer) {
        if (customer == null || customer.getSystemId() == null) {
            return List.of();
        }
        return findByCustomerIdSorted(customer.getSystemId());
    }
}
