package com.driveflow.demo_driveflow.booking;

/**
 * Valid lifecycle states for a Booking in the DriveFlow Car Rental System.
 * Aligned with the database CHECK constraint dbo.booking.chk_booking_status:
 * ('PENDING', 'APPROVED', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'ACTIVE', 'RETURNED')
 */
public enum BookingStatus {
    PENDING,
    APPROVED,
    CONFIRMED,
    ACTIVE,
    RETURNED,
    COMPLETED,
    CANCELLED;

    // String validation constants matching database check constraint values
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_RETURNED = "RETURNED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    /**
     * Validates whether a given status string matches any valid BookingStatus state.
     *
     * @param status status name to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        for (BookingStatus bs : values()) {
            if (bs.name().equalsIgnoreCase(status.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves a status string to its matching BookingStatus enum constant.
     *
     * @param status status string
     * @return BookingStatus enum constant or null if input is null/blank
     * @throws IllegalArgumentException if string does not match any valid status
     */
    public static BookingStatus fromString(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        for (BookingStatus bs : values()) {
            if (bs.name().equalsIgnoreCase(status.trim())) {
                return bs;
            }
        }
        throw new IllegalArgumentException("Unknown booking status: '" + status + "'");
    }
}
