package com.discipolat.modules.members.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.members.domain.GeofencePingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeofencingControllerTest {

    @Mock private GeofencePingRepository pingRepository;
    @Mock private SecurityUtils securityUtils;

    private GeofencingController controller;
    private UUID userId;

    @BeforeEach
    void setUp() {
        controller = new GeofencingController(securityUtils, pingRepository);
        userId = UUID.randomUUID();
        TenantContext.setTenantId(UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void checkIn_WithinGeofence_ReturnsCheckedIn() {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        var request = new GeofenceCheckInRequest(48.8566, 2.3522, 10.0, "NORMAL");

        ResponseEntity<Map<String, Object>> response = controller.checkIn(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("CHECKED_IN", response.getBody().get("status"));
        assertNotNull(response.getBody().get("timestamp"));
        assertEquals(userId.toString(), response.getBody().get("memberId"));
    }

    @Test
    void checkIn_IncludesTimestamp() {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        var request = new GeofenceCheckInRequest(48.8566, 2.3522, 10.0, "NORMAL");

        Map<String, Object> body = controller.checkIn(request).getBody();

        assertNotNull(body.get("timestamp"));
        String timestamp = body.get("timestamp").toString();
        assertTrue(timestamp.contains("T")); // ISO format
    }

    @Test
    void checkOut_ReturnsCheckedOut() {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        var request = new GeofenceCheckInRequest(48.8566, 2.3522, 10.0, "NORMAL");

        ResponseEntity<Map<String, Object>> response = controller.checkOut(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("CHECKED_OUT", response.getBody().get("status"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void getConfig_ReturnsGeofenceSettings() {
        ResponseEntity<Map<String, Object>> response = controller.getConfig();

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertEquals(true, body.get("enabled"));
        assertNotNull(body.get("latitude"));
        assertNotNull(body.get("longitude"));
        assertNotNull(body.get("radiusMeters"));
        assertNotNull(body.get("churchName"));
    }

    @Test
    void getConfig_RadiusIsReasonable() {
        Map<String, Object> body = controller.getConfig().getBody();
        Number radiusNum = (Number) body.get("radiusMeters");
        double radius = radiusNum.doubleValue();

        assertTrue(radius > 0 && radius <= 10000, "Radius should be between 0 and 10km");
    }
}
