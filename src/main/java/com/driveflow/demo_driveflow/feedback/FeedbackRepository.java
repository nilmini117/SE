package com.driveflow.demo_driveflow.feedback;

import com.driveflow.demo_driveflow.users.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByCustomer(Customer customer);
}
