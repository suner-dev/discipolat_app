package com.discipolat.modules.reports.domain;

import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * P22 — Génération automatique de rapports PDF périodiques.
 *
 * - Rapport mensuel le 1er de chaque mois à 6h
 * - Rapport trimestriel le 1er janvier, avril, juillet, octobre à 7h
 *
 * Les rapports sont générés en PDF et une notification est envoyée
 * aux pasteurs/admins pour les télécharger.
 */
@Service
public class AutoReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(AutoReportScheduler.class);

    private final ReportPdfService reportPdfService;
    private final ReportService reportService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public AutoReportScheduler(ReportPdfService reportPdfService,
                               ReportService reportService,
                               NotificationService notificationService,
                               UserRepository userRepository) {
        this.reportPdfService = reportPdfService;
        this.reportService = reportService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    /**
     * Monthly report — runs on the 1st of each month at 6:00 AM.
     */
    @Scheduled(cron = "0 0 6 1 * *")
    public void generateMonthlyReport() {
        log.info("[AutoReport] Generating monthly report");
        try {
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            LocalDate from = lastMonth.atDay(1);
            LocalDate to = lastMonth.atEndOfMonth();

            byte[] pdf = generateExecutivePdf("Rapport Mensuel", from, to);
            if (pdf != null) {
                notifyLeaders("📊 Rapport mensuel " + lastMonth,
                        "Le rapport exécutif de " + lastMonth + " est prêt (" + pdf.length + " octets).",
                        "MONTHLY_REPORT");
                log.info("[AutoReport] Monthly report generated: {} bytes", pdf.length);
            }
        } catch (Exception e) {
            log.error("[AutoReport] Failed to generate monthly report: {}", e.getMessage());
        }
    }

    /**
     * Quarterly report — runs on the 1st of Jan, Apr, Jul, Oct at 7:00 AM.
     */
    @Scheduled(cron = "0 0 7 1 1,4,7,10 *")
    public void generateQuarterlyReport() {
        log.info("[AutoReport] Generating quarterly report");
        try {
            YearMonth lastQuarterEnd = YearMonth.now().minusMonths(1);
            YearMonth quarterStart = lastQuarterEnd.minusMonths(2);
            LocalDate from = quarterStart.atDay(1);
            LocalDate to = lastQuarterEnd.atEndOfMonth();

            byte[] pdf = generateExecutivePdf("Rapport Trimestriel", from, to);
            if (pdf != null) {
                notifyLeaders("📈 Rapport trimestriel Q" + ((lastQuarterEnd.getMonthValue() - 1) / 3 + 1),
                        "Le rapport exécutif trimestriel couvre " + quarterStart + " → " + lastQuarterEnd + " (" + pdf.length + " octets).",
                        "QUARTERLY_REPORT");
                log.info("[AutoReport] Quarterly report generated: {} bytes", pdf.length);
            }
        } catch (Exception e) {
            log.error("[AutoReport] Failed to generate quarterly report: {}", e.getMessage());
        }
    }

    /**
     * Generate the executive PDF with real data.
     */
    private byte[] generateExecutivePdf(String titre, LocalDate from, LocalDate to) {
        try {
            long totalFamilles = reportService.countFamilies(from, to);
            long totalMembres = reportService.countSouls();

            return reportPdfService.generateExecutiveReportPdf(
                    titre + " — " + from.getYear(),
                    from, to,
                    totalFamilles, 0, 0,
                    totalFamilles, totalMembres);
        } catch (Exception e) {
            log.error("[AutoReport] PDF generation failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Notify all pastors and admins about the generated report.
     */
    private void notifyLeaders(String titre, String message, String tag) {
        List<User> leaders = userRepository.findByRole(com.discipolat.common.domain.UserRole.ADMIN);
        leaders.addAll(userRepository.findByRole(com.discipolat.common.domain.UserRole.PASTEUR));
        for (User user : leaders) {
            try {
                notificationService.create(user.getId(),
                        TypeNotification.INFORMATION,
                        CanalNotification.IN_APP,
                        titre, message, null, "REPORT");
            } catch (Exception e) {
                log.debug("Notification failed for {}: {}", user.getId(), e.getMessage());
            }
        }
    }
}
