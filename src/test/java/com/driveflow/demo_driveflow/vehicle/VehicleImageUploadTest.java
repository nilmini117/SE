package com.driveflow.demo_driveflow.vehicle;

import com.driveflow.demo_driveflow.branch.BranchRepository;
import com.driveflow.demo_driveflow.users.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class VehicleImageUploadTest {

    private MockMvc apiMockMvc;
    private MockMvc webMockMvc;

    @Mock
    private VehicleService vehicleService;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private com.driveflow.demo_driveflow.feedback.FeedbackService feedbackService;

    @InjectMocks
    private VehicleApiController vehicleApiController;

    @InjectMocks
    private VehicleController vehicleController;

    private Authentication staffAuth;
    private Authentication customerAuth;

    @BeforeEach
    void setUp() {
        apiMockMvc = MockMvcBuilders.standaloneSetup(vehicleApiController).build();
        webMockMvc = MockMvcBuilders.standaloneSetup(vehicleController).build();

        staffAuth = new UsernamePasswordAuthenticationToken(
                "staff@driveflow.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_STAFF"))
        );

        customerAuth = new UsernamePasswordAuthenticationToken(
                "customer@driveflow.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
    }

    @Test
    @DisplayName("Verify Spring Boot vehicle controller accepts multipart/form-data request with vehicle details and image file")
    void registerVehicleMultipart_viaApi_shouldSucceedWith201() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "honda_civic.jpg",
                "image/jpeg",
                "dummy image content bytes".getBytes()
        );

        Vehicle savedVehicle = new Vehicle();
        savedVehicle.setVehicleId(101L);
        savedVehicle.setBrand("Honda");
        savedVehicle.setModel("Civic 2024");
        savedVehicle.setRegNo("WP CB-1234");
        savedVehicle.setColor("Sonic Gray");
        savedVehicle.setMileage(15000);
        savedVehicle.setStatus("AVAILABLE");
        savedVehicle.setImageUrl("/uploads/vehicles/vehicle_honda_civic.jpg");

        when(vehicleService.registerVehicle(any(VehicleRegistrationDto.class), any(MultipartFile.class)))
                .thenReturn(savedVehicle);

        apiMockMvc.perform(multipart("/api/vehicles")
                        .file(imageFile)
                        .param("brand", "Honda")
                        .param("model", "Civic 2024")
                        .param("regNo", "WP CB-1234")
                        .param("color", "Sonic Gray")
                        .param("mileage", "15000")
                        .param("transmission", "Automatic (CVT)")
                        .param("capacity", "5 Seats")
                        .param("fuel", "Hybrid 24 km/L")
                        .param("dailyRate", "13500.00")
                        .principal(staffAuth))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vehicleId").value(101))
                .andExpect(jsonPath("$.brand").value("Honda"))
                .andExpect(jsonPath("$.model").value("Civic 2024"))
                .andExpect(jsonPath("$.imageUrl").value("/uploads/vehicles/vehicle_honda_civic.jpg"));

        verify(vehicleService, times(1)).registerVehicle(any(VehicleRegistrationDto.class), any(MultipartFile.class));
    }

    @Test
    @DisplayName("Verify /vehicles/api/register multipart endpoint receives vehicle details and image simultaneously")
    void registerVehicleMultipart_viaWebEndpoint_shouldSucceedWith200() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "toyota_prius.png",
                "image/png",
                "test png image bytes".getBytes()
        );

        Vehicle savedVehicle = new Vehicle();
        savedVehicle.setVehicleId(102L);
        savedVehicle.setBrand("Toyota");
        savedVehicle.setModel("Prius Prime");
        savedVehicle.setImageUrl("/uploads/vehicles/vehicle_prius.png");

        when(vehicleService.registerVehicle(any(VehicleRegistrationDto.class), any(MultipartFile.class)))
                .thenReturn(savedVehicle);

        webMockMvc.perform(multipart("/vehicles/api/register")
                        .file(imageFile)
                        .param("brand", "Toyota")
                        .param("model", "Prius Prime")
                        .param("regNo", "WP CA-5555")
                        .param("color", "White"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId").value(102))
                .andExpect(jsonPath("$.imageUrl").value("/uploads/vehicles/vehicle_prius.png"));

        verify(vehicleService, times(1)).registerVehicle(any(VehicleRegistrationDto.class), any(MultipartFile.class));
    }

    @Test
    @DisplayName("Security: CUSTOMER role attempting to post vehicle to /api/vehicles returns 403 Forbidden")
    void registerVehicleMultipart_customerRole_returns403Forbidden() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "data".getBytes()
        );

        apiMockMvc.perform(multipart("/api/vehicles")
                        .file(imageFile)
                        .param("brand", "Toyota")
                        .param("model", "Prius")
                        .param("regNo", "WP CA-0001")
                        .principal(customerAuth))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        verify(vehicleService, never()).registerVehicle(any(VehicleRegistrationDto.class), any());
    }

    @Test
    @DisplayName("Verify FileStorageService saves file locally and returns correct /uploads/vehicles/ path")
    void fileStorageService_shouldSaveLocallyAndReturnWebPath(@TempDir Path tempDir) throws Exception {
        FileStorageService storageService = new FileStorageService();
        ReflectionTestUtils.setField(storageService, "uploadBaseDir", tempDir.toString());

        MockMultipartFile mockFile = new MockMultipartFile(
                "image",
                "test_car.webp",
                "image/webp",
                "fake image content".getBytes()
        );

        String resultPath = storageService.storeVehicleImage(mockFile);

        assertNotNull(resultPath);
        assertTrue(resultPath.startsWith("/uploads/vehicles/vehicle_"));
        assertTrue(resultPath.endsWith(".webp"));

        File targetDir = tempDir.resolve("vehicles").toFile();
        assertTrue(targetDir.exists() && targetDir.isDirectory());
        File[] savedFiles = targetDir.listFiles();
        assertNotNull(savedFiles);
        assertEquals(1, savedFiles.length);
        assertTrue(savedFiles[0].getName().endsWith(".webp"));
    }

    @Test
    @DisplayName("Verify FileStorageService rejects non-image MIME types")
    void fileStorageService_shouldRejectNonImageMimeTypes(@TempDir Path tempDir) {
        FileStorageService storageService = new FileStorageService();
        ReflectionTestUtils.setField(storageService, "uploadBaseDir", tempDir.toString());

        MockMultipartFile badFile = new MockMultipartFile(
                "document",
                "malicious.exe",
                "application/x-msdownload",
                "bad bytes".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () -> storageService.storeVehicleImage(badFile));
    }
}
