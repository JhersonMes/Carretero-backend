package com.carretero.service.billing;

import com.carretero.config.SunatProperties;
import com.carretero.model.Invoice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Guarda en disco lo que se envia y lo que SUNAT devuelve.
 *
 * No es un detalle administrativo: ante una fiscalizacion, el XML firmado y su
 * CDR son la prueba de que el comprobante se declaro. Si solo viven en memoria
 * durante el envio, no existen.
 *
 * Los archivos se organizan por mes para que la carpeta siga siendo navegable
 * despues de un ano de operacion.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingDocumentStore {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final SunatProperties properties;

    /**
     * Nombre base que exige SUNAT: {RUC}-{tipo}-{serie}-{correlativo}.
     * De el salen el .xml, el .zip enviado y el R-*.xml del CDR.
     */
    public String baseName(Invoice invoice, String issuerRuc) {
        return String.format("%s-%s-%s-%08d",
                issuerRuc,
                SunatCodes.documentType(invoice.getInvoiceType()),
                invoice.getSeries(),
                invoice.getCorrelativeNumber());
    }

    /** Serializa el DOM ya firmado. Sin indentado: alterarlo invalida la firma. */
    public byte[] toBytes(Document document) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(document), new StreamResult(out));
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el XML del comprobante", e);
        }
    }

    /** Empaqueta el XML en el ZIP con el nombre que espera SUNAT. */
    public byte[] zip(String baseName, byte[] xml) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(out)) {
                zip.putNextEntry(new ZipEntry(baseName + ".xml"));
                zip.write(xml);
                zip.closeEntry();
            }
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo empaquetar el comprobante " + baseName, e);
        }
    }

    /**
     * Escribe el XML firmado y devuelve su ruta relativa, que es lo que se guarda
     * en el comprobante.
     */
    public String saveSignedXml(Invoice invoice, String baseName, byte[] xml) {
        return write(invoice, baseName + ".xml", xml);
    }

    /** Escribe el CDR recibido. */
    public String saveCdr(Invoice invoice, String baseName, byte[] cdr) {
        return write(invoice, "R-" + baseName + ".xml", cdr);
    }

    /**
     * Hash del CDR recibido.
     *
     * Es un SHA-256 calculado por nosotros sobre el archivo, no un valor que
     * emita SUNAT: sirve para detectar que el CDR guardado sea el mismo que
     * llego, y para que el campo solo tenga contenido cuando hubo CDR de verdad.
     */
    public String hash(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el hash del CDR", e);
        }
    }

    private String write(Invoice invoice, String fileName, byte[] content) {
        try {
            String month = invoice.getIssueDate().format(MONTH);
            Path dir = Path.of(properties.getStorageDir(), month);
            Files.createDirectories(dir);

            Path file = dir.resolve(fileName);
            Files.write(file, content);

            return Path.of(month, fileName).toString().replace('\\', '/');
        } catch (Exception e) {
            // Guardar el archivo no puede tumbar una venta ya cobrada: se avisa y
            // se sigue. El estado del comprobante es lo que no se puede perder.
            log.error("No se pudo guardar {} del comprobante {}: {}",
                    fileName, invoice.getFullNumber(), e.getMessage());
            return null;
        }
    }

    /** Texto legible de un XML, para los mensajes de error. */
    public String asText(byte[] content) {
        return new String(content, StandardCharsets.UTF_8);
    }
}
