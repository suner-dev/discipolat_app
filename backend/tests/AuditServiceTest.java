import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;

public class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private AuditService auditService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testLog() {
        // Arrange
        String action = "CREATE_MEMBER";
        String entiteType = "Member";
        UUID entiteId = UUID.randomUUID();
        Map<String, Object> ancienValeur = Map.of();
        Map<String, Object> nouvelleValeur = Map.of("name", "John Doe");
        HttpServletRequest request = mock(HttpServletRequest.class);

        // Act
        auditService.log(action, entiteType, entiteId, ancienValeur, nouvelleValeur, request);

        // Assert
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    public void testFindById() {
        // Arrange
        UUID id = UUID.randomUUID();
        AuditLog log = new AuditLog();
        when(auditLogRepository.findById(id)).thenReturn(Optional.of(log));

        // Act
        AuditLog foundLog = auditService.findById(id);

        // Assert
        assertNotNull(foundLog);
        assertEquals(log, foundLog);
    }
}
