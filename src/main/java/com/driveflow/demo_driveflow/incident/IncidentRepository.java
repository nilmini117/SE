package com.driveflow.demo_driveflow.incident;

import com.driveflow.demo_driveflow.users.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByCustomerOrderByDateDesc(Customer customer);
    List<Incident> findByStatusIgnoreCase(String status);
    long countByStatusIgnoreCase(String status);
}
