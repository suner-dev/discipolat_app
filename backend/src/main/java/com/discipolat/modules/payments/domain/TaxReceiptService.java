package com.discipolat.modules.payments.domain;

import com.discipolat.modules.currency.domain.Iso4217CurrencyValidator;
import com.discipolat.modules.tenants.domain.TenantSettings;
import com.discipolat.modules.tenants.domain.TenantSettingsRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * P12 — Reçu fiscal automatique pour les paiements confirmés.
 * PDF généré à la demande (OpenPDF) : identité du donateur, montant, opérateur,
 * référence de transaction — conforme aux mentions d'usage.
 *
 * <p>A3 (M9) — le reçu n'est plus «PLATFORM CENTRIC» : le nom légal émetteur,
 * le numéro fiscal et les mentions légales viennent de {@code tenant_settings}
 * (configurables PAR tenant, jamais une valeur globale unique). Absence de
 * configuration = dégradation gracieuse documentée sur le document, pas de
 * valeur inventée.</p>
 */
@Service
public class TaxReceiptService {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final TenantSettingsRepository tenantSettingsRepository;
    private final Iso4217CurrencyValidator currencyValidator;

    public TaxReceiptService(TenantSettingsRepository tenantSettingsRepository,
                             Iso4217CurrencyValidator currencyValidator) {
        this.tenantSettingsRepository = tenantSettingsRepository;
        this.currencyValidator = currencyValidator;
    }

    public byte[] generatePdf(PaymentIntent payment) {
        TenantSettings settings = findSettings(payment.getTenantId());
        try {
            Document doc = new Document(com.lowagie.text.PageSize.A4, 50, 50, 50, 50);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font h1 = new Font(Font.HELVETICA, 20, Font.BOLD);
            Font label = new Font(Font.HELVETICA, 11, Font.BOLD);
            Font value = new Font(Font.HELVETICA, 11);
            Font small = new Font(Font.HELVETICA, 9, Font.ITALIC);

            // Émetteur : identité légale du tenant (A3 — configurable par tenant)
            String issuer = issuerName(settings);
            if (issuer != null) {
                doc.add(new Paragraph(issuer, label));
                doc.add(new Paragraph(" "));
            }
            doc.add(new Paragraph("REÇU FISCAL", h1));
            doc.add(new Paragraph(" "));
            if (settings != null && hasText(settings.getReceiptTaxNumber())) {
                doc.add(new Paragraph("Numéro fiscal", label));
                doc.add(new Paragraph(settings.getReceiptTaxNumber(), value));
                doc.add(new Paragraph(" "));
            }
            doc.add(new Paragraph("Donateur / Donatrice", label));
            doc.add(new Paragraph(payment.getUserId() != null ? payment.getUserId().toString() : "—", value));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Montant", label));
            doc.add(new Paragraph(formatAmount(payment), value));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Nature du don", label));
            doc.add(new Paragraph(payment.getPurpose() != null ? payment.getPurpose().name() : "—", value));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Opérateur de paiement", label));
            doc.add(new Paragraph(operatorLabel(payment), value));
            doc.add(new Paragraph("Numéro", label));
            doc.add(new Paragraph(payment.getPhoneNumber() != null ? mask(payment.getPhoneNumber()) : "—", value));
            doc.add(new Paragraph("Référence transaction", label));
            doc.add(new Paragraph(payment.getProviderReference() != null
                    ? payment.getProviderReference()
                    : (payment.getTransactionId() != null ? payment.getTransactionId().toString() : "—"), value));
            doc.add(new Paragraph("Date de confirmation", label));
            LocalDateTime confirmed = payment.getConfirmedAt() != null ? payment.getConfirmedAt() : payment.getCreatedAt();
            doc.add(new Paragraph(confirmed != null ? confirmed.format(DF) : "—", value));
            doc.add(new Paragraph(" "));
            // Mentions légales du tenant (A3) : texte libre, jamais une clause imposée.
            if (settings != null && hasText(settings.getReceiptLegalMentions())) {
                doc.add(new Paragraph(settings.getReceiptLegalMentions(), small));
                doc.add(new Paragraph(" "));
            }
            doc.add(new Paragraph("Document généré automatiquement par Discipolat le "
                    + LocalDateTime.now().format(DF) + ". Conservez-le pour votre déclaration.", small));

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Génération du reçu fiscal impossible", e);
        }
    }

    /** Le nom légal déclaré prime ; à défaut le nom commercial ; à défaut rien d'inventé. */
    private static String issuerName(TenantSettings settings) {
        if (settings == null) {
            return null;
        }
        if (hasText(settings.getLegalName())) {
            return settings.getLegalName();
        }
        return hasText(settings.getBusinessName()) ? settings.getBusinessName() : null;
    }

    /** Le libellé Mobile Money n'est qu'un cas particulier : tout canal est rendu. */
    private static String operatorLabel(PaymentIntent payment) {
        if (payment.getProviderName() != null && !payment.getProviderName().isBlank()) {
            return payment.getProviderName();
        }
        return payment.getOperator() != null ? payment.getOperator().name() : "—";
    }

    /**
     * Montant formaté selon les décimales ISO-4217 de SA devise (0 pour XOF,
     * 2 pour EUR…). Devise inconnue du référentiel : rendu brut, sans arrondi
     * inventé — l'honnêteté du document prime sur la belle typographie.
     */
    private String formatAmount(PaymentIntent payment) {
        BigDecimal amount = payment.getAmount();
        String currency = payment.getCurrency();
        if (amount == null) {
            return "—";
        }
        if (currencyValidator.isSupported(currency)) {
            int decimals = currencyValidator.minorUnits(currency);
            return amount.setScale(decimals, java.math.RoundingMode.HALF_UP).toPlainString()
                    + " " + currency.toUpperCase();
        }
        return amount.toPlainString() + (hasText(currency) ? " " + currency : "");
    }

    private TenantSettings findSettings(UUID tenantId) {
        if (tenantId == null) {
            return null;
        }
        try {
            return tenantSettingsRepository.findByTenantId(tenantId).orElse(null);
        } catch (RuntimeException repositoryUnreachable) {
            // Un reçu reste dûment généré si la configuration émettrice est
            // temporairement illisible : sections optionnelles omises.
            return null;
        }
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static String mask(String phone) {
        if (phone == null || phone.length() < 4) return phone;
        return "*".repeat(Math.max(phone.length() - 4, 0)) + phone.substring(phone.length() - 4);
    }
}
