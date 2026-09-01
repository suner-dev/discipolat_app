import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.UUID;
import java.util.Map;

public class ReportServiceTest {

    @Mock
    private MakerReportRepository makerReportRepository;

    @Mock
    private FamilyReportRepository familyReportRepository;

    @InjectMocks
    private ReportService reportService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testSubmitMakerReport() {
        // Arrange
        SubmitMakerReportRequest request = new SubmitMakerReportRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            LocalDate.now(),
            Map.of("presence", true),
            List.of(),
            RaisonAbsence.NONE,
            "No comments",
            "Category",
            "Details",
            0,
            MotifSortie.NONE,
            0,
            0,
            "Challenges",
            "Requests",
            "Suggestions",
            "Additional notes",
            List.of()
        );

        // Act
        reportService.submitMakerReport(request);

        // Assert
        verify(makerReportRepository, times(1)).save(any(MakerReport.class));
    }

    @Test
    public void testGetMakerReport() {
        // Arrange
        UUID reportId = UUID.randomUUID();
        MakerReport report = new MakerReport();
        when(makerReportRepository.findById(reportId)).thenReturn(Optional.of(report));

        // Act
        MakerReport foundReport = reportService.findMakerReportById(reportId);

        // Assert
        assertNotNull(foundReport);
        assertEquals(report, foundReport);
    }
}
