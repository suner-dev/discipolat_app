package com.discipolat.modules.payments.domain;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * P12 — Reçu fiscal automatique pour les paiements confirmés.
 * PDF généré à la demande (OpenPDF) : identité du donateur, montant, opérateur,
 * référence de transaction — conforme aux mentions d'usage.
 */
@Service
public class TaxReceiptService {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generatePdf(PaymentIntent payment) {
        try {
            Document doc = new Document(com.lowagie.text.PageSize.A4, 50, 50, 50, 50);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font h1 = new Font(Font.HELVETICA, 20, Font.BOLD);
            Font label = new Font(Font.HELVETICA, 11, Font.BOLD);
            Font value = new Font(Font.HELVETICA, 11);
            Font small = new Font(Font.HELVETICA, 9, Font.ITALIC);

            doc.add(new Paragraph("REÇU FISCAL", h1));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Donateur / Donatrice", label));
            doc.add(new Paragraph(payment.getUserId() != null ? payment.getUserId().toString() : "—", value));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Montant", label));
            doc.add(new Paragraph(payment.getAmount() + " " + payment.getCurrency(), value));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Nature du don", label));
            doc.add(new Paragraph(payment.getPurpose() != null ? payment.getPurpose().name() : "—", value));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Opérateur Mobile Money", label));
            doc.add(new Paragraph(payment.getOperator() != null ? payment.getOperator().name() : "—", value));
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
            doc.add(new Paragraph("Document généré automatiquement par Discipolat le "
                    + LocalDateTime.now().format(DF) + ". Conservez-le pour votre déclaration.", small));

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Génération du reçu fiscal impossible", e);
        }
    }

    private static String mask(String phone) {
        if (phone == null || phone.length() < 4) return phone;
        return "*".repeat(Math.max(phone.length() - 4, 0)) + phone.substring(phone.length() - 4);
    }
}
