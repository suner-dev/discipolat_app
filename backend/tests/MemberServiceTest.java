import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.UUID;
import java.util.Map;

public class MemberServiceTest {

    @Mock
    private MemberService memberService;

    @InjectMocks
    private MemberController memberController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetMyProgression() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(memberService.getMyProgression()).thenReturn(Map.of("etatSpirituel", "Actif"));

        // Act
        ResponseEntity<Map<String, Object>> response = memberController.myProgression();

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Actif", response.getBody().get("etatSpirituel"));
    }

    @Test
    public void testUpdateMyProfile() {
        // Arrange
        UpdateMemberProfileRequest request = new UpdateMemberProfileRequest("1234567890", "url", "Célibataire", LocalDate.now(), "Ingénieur", "Master", 0);

        // Act
        ResponseEntity<MemberDashboardResponse> response = memberController.updateProfile(request);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
    }
}
