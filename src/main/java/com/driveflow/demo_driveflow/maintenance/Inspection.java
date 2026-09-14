package com.driveflow.demo_driveflow.maintenance;

import com.driveflow.demo_driveflow.booking.Booking;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// NOTE: placed here per the proposal text ("maintain inspection records"),
// but the EER diagram also links this to Booking. Confirm final ownership
// with IT25103933 and IT25103879 before merging to develop.
@Entity
@Table(name = "inspection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Inspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inspection_id")
    private Long inspectionId;

    @Column(name = "fuel_level")
    private Integer fuelLevel;

    @Column(name = "damage_notes")
    private String damageNotes;

    @Column(name = "type")
    private String type; // ROUTINE, ACCIDENT

    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;
}
