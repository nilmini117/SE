package com.driveflow.demo_driveflow.promotion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class PromotionImmutabilityTest {

    private MockMvc mockMvc;

    @Mock
    private PromotionService promotionService;

    @InjectMocks
    private PromotionController promotionController;

    private Promotion existingPromo;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(promotionController).build();

        existingPromo = new Promotion();
        existingPromo.setPromotionId(1L);
        existingPromo.setTitle("Summer Park Super Saver");
        existingPromo.setCouponId("SUMMER15");
        existingPromo.setCouponCode("SUMMER15");
        existingPromo.setVehicleSeason("SUMMER");
        existingPromo.setVehicleCategory("SEDAN");
        existingPromo.setDiscountRate(new BigDecimal("15.00"));
        existingPromo.setStartDate(LocalDate.of(2026, 6, 1));
        existingPromo.setEndDate(LocalDate.of(2026, 8, 31));
        existingPromo.setStatus("ACTIVE");
    }

    @Test
    @DisplayName("Staff request attempting to update discount percentage via form POST is rejected with 400 Bad Request")
    void staffAttemptingUpdateDiscountPercentage_formPost_rejectedWith400() throws Exception {
        when(promotionService.updatePromotion(eq(1L), any(Promotion.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modifying promotion discount percentage is strictly prohibited."));

        mockMvc.perform(post("/promotions/1")
                        .param("discountRate", "25.00")
                        .param("endDate", "2026-09-30"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Staff request attempting to update discount percentage via REST PUT is rejected with 400 Bad Request")
    void staffAttemptingUpdateDiscountPercentage_restPut_rejectedWith400() throws Exception {
        when(promotionService.updatePromotion(eq(1L), any(Promotion.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modifying promotion discount percentage is strictly prohibited."));

        String jsonPayload = """
            {
                "discountRate": 30.00,
                "endDate": "2026-10-31"
            }
            """;

        mockMvc.perform(put("/promotions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Staff request attempting to update start date via form POST is rejected with 400 Bad Request")
    void staffAttemptingUpdateStartDate_formPost_rejectedWith400() throws Exception {
        when(promotionService.updatePromotion(eq(1L), any(Promotion.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modifying promotion start date is strictly prohibited."));

        mockMvc.perform(post("/promotions/1")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-09-30"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Staff request attempting to update start date via REST PUT is rejected with 400 Bad Request")
    void staffAttemptingUpdateStartDate_restPut_rejectedWith400() throws Exception {
        when(promotionService.updatePromotion(eq(1L), any(Promotion.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modifying promotion start date is strictly prohibited."));

        String jsonPayload = """
            {
                "startDate": "2026-05-01",
                "endDate": "2026-09-30"
            }
            """;

        mockMvc.perform(put("/promotions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Staff request updating only end date via form POST succeeds with redirect")
    void staffUpdatingEndDate_formPost_succeeds() throws Exception {
        existingPromo.setEndDate(LocalDate.of(2026, 12, 31));
        when(promotionService.updatePromotion(eq(1L), any(Promotion.class)))
                .thenReturn(existingPromo);

        mockMvc.perform(post("/promotions/1")
                        .param("endDate", "2026-12-31"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/promotions"));

        verify(promotionService, times(1)).updatePromotion(eq(1L), any(Promotion.class));
    }

    @Test
    @DisplayName("Staff request updating only end date via REST PUT succeeds with 200 OK")
    void staffUpdatingEndDate_restPut_succeeds() throws Exception {
        existingPromo.setEndDate(LocalDate.of(2026, 12, 31));
        when(promotionService.updatePromotion(eq(1L), any(Promotion.class)))
                .thenReturn(existingPromo);

        String jsonPayload = """
            {
                "endDate": "2026-12-31"
            }
            """;

        mockMvc.perform(put("/promotions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk());

        verify(promotionService, times(1)).updatePromotion(eq(1L), any(Promotion.class));
    }
}
