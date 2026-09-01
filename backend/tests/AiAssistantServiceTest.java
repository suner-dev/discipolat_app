import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.Map;

public class AiAssistantServiceTest {

    @Mock
    private AiAssistantService aiAssistantService;

    @InjectMocks
    private AiAssistantController aiAssistantController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testChat() {
        // Arrange
        String message = "How can I help you?";
        when(aiAssistantService.chat(message, any())).thenReturn(Map.of("response", "Here is the answer."));

        // Act
        ResponseEntity<Map<String, Object>> response = aiAssistantController.chat(Map.of("message", message));

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Here is the answer.", response.getBody().get("response"));
    }

    @Test
    public void testContext() {
        // Arrange
        String query = "What is the family status?";
        when(aiAssistantService.getContextForQuery(query)).thenReturn(Map.of("context", "Family context data."));

        // Act
        ResponseEntity<Map<String, Object>> response = aiAssistantController.context(query);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Family context data.", response.getBody().get("context"));
    }
}
