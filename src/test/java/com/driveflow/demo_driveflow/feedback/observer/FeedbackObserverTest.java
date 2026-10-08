package com.driveflow.demo_driveflow.feedback.observer;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class FeedbackObserverTest {

    @Test
    public void testObserverNotification() {
        FeedbackManager manager = new FeedbackManager();
        List<String> receivedMessages = new ArrayList<>();

        Observer mockObserver = receivedMessages::add;
        manager.addObserver(mockObserver);

        manager.receiveNewFeedback("Great service!");
        assertEquals(1, receivedMessages.size());
        assertEquals("Great service!", receivedMessages.get(0));

        manager.removeObserver(mockObserver);
        manager.receiveNewFeedback("Another review");
        assertEquals(1, receivedMessages.size(), "Observer should not receive messages after removal.");
    }

    @Test
    public void testConcreteObservers() {
        FeedbackManager manager = new FeedbackManager();
        EmailAlertObserver emailObserver = new EmailAlertObserver();
        DashboardUpdateObserver dashboardObserver = new DashboardUpdateObserver();

        manager.addObserver(emailObserver);
        manager.addObserver(dashboardObserver);

        assertDoesNotThrow(() -> manager.receiveNewFeedback("Test feedback content"));
    }
}
