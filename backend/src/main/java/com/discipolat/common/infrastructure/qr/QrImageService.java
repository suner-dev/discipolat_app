package com.discipolat.common.infrastructure.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service QR transversal (G5.6 — mobile terrain) : génère des QR PNG (data URL)
 * et décode des QR depuis des photos prises sur le terrain. Aucune dépendance
 * scanner côté mobile : la photo est remontée et décodée ici (ZXing).
 */
@Service
public class QrImageService {

    /** Encode un contenu en PNG QR (data URL base64). */
    public String renderPngDataUrl(String content) throws IOException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        com.google.zxing.common.BitMatrix matrix;
        try {
            matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 300, 300, hints);
        } catch (com.google.zxing.WriterException e) {
            throw new IOException("Génération QR impossible", e);
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", os);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(os.toByteArray());
    }

    /** Décode un QR depuis une photo (bytes PNG/JPEG). Retourne le contenu textuel ou empty. */
    public Optional<String> decodeQrImage(byte[] imageBytes) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (img == null) {
                return Optional.empty();
            }
            var source = new BufferedImageLuminanceSource(img);
            var bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            var result = new MultiFormatReader().decode(bitmap, hints);
            return Optional.ofNullable(result.getText());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
