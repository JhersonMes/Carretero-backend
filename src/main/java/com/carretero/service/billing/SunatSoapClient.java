package com.carretero.service.billing;

import com.carretero.config.SunatProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Habla con el servicio SOAP de facturacion de SUNAT.
 *
 * Se arma el sobre a mano en vez de generar un cliente WSDL porque la seguridad
 * que pide SUNAT es un UsernameToken en texto plano dentro de la cabecera: no
 * hay firma del sobre ni negociacion, y traer una pila de web services completa
 * para eso es cargar mucho andamio para un solo POST.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SunatSoapClient {

    private final SunatProperties properties;

    /**
     * Resultado del envio, separando los tres desenlaces que se tratan distinto.
     *
     * Un rechazo de SUNAT y una caida de red no son lo mismo aunque ambos
     * terminen sin CDR: del rechazo hay que avisar y corregir; del corte hay que
     * reintentar. Mezclarlos haria que un problema de internet quedara guardado
     * para siempre como un comprobante rechazado.
     */
    public sealed interface SoapResult {

        /** SUNAT respondio con la constancia. */
        record Cdr(byte[] content) implements SoapResult {
        }

        /** SUNAT contesto, y rechazo el comprobante. */
        record Fault(String code, String message) implements SoapResult {
        }

        /** No se pudo hablar con SUNAT. Se reintenta despues. */
        record Unreachable(String message) implements SoapResult {
        }
    }

    /** Envia un comprobante individual (sendBill) y espera su CDR. */
    public SoapResult sendBill(String endpoint, String fileName, byte[] zip) {
        String envelope = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" \
                xmlns:ser="http://service.sunat.gob.pe" \
                xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
                  <soapenv:Header>
                    <wsse:Security>
                      <wsse:UsernameToken>
                        <wsse:Username>%s</wsse:Username>
                        <wsse:Password>%s</wsse:Password>
                      </wsse:UsernameToken>
                    </wsse:Security>
                  </soapenv:Header>
                  <soapenv:Body>
                    <ser:sendBill>
                      <fileName>%s</fileName>
                      <contentFile>%s</contentFile>
                    </ser:sendBill>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(
                escape(properties.getSolUser()),
                escape(properties.getSolPassword()),
                escape(fileName),
                Base64.getEncoder().encodeToString(zip));

        return post(endpoint, envelope);
    }

    private SoapResult post(String endpoint, String envelope) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Content-Type", "text/xml;charset=UTF-8")
                    .header("SOAPAction", "")
                    .POST(HttpRequest.BodyPublishers.ofString(envelope, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<byte[]> response =
                    client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            return interpret(response.body());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new SoapResult.Unreachable("Envio interrumpido");
        } catch (Exception e) {
            // Timeout, DNS, sin internet: se reintenta, no se rechaza.
            return new SoapResult.Unreachable(
                    "No se pudo conectar con SUNAT: " + e.getMessage());
        }
    }

    /**
     * Lee la respuesta.
     *
     * SUNAT devuelve el CDR dentro de applicationResponse cuando acepta, y un
     * Fault de SOAP cuando el comprobante esta mal formado o duplicado. Un HTTP
     * 500 con Fault es una respuesta valida del negocio, no una falla de red.
     */
    private SoapResult interpret(byte[] body) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(body));

            String encoded = firstText(document, "applicationResponse");
            if (encoded != null && !encoded.isBlank()) {
                byte[] zip = Base64.getDecoder().decode(encoded.trim());
                byte[] cdr = firstXmlInZip(zip);
                if (cdr == null) {
                    return new SoapResult.Unreachable("SUNAT devolvio un ZIP sin CDR dentro");
                }
                return new SoapResult.Cdr(cdr);
            }

            String faultString = firstText(document, "faultstring");
            if (faultString != null) {
                String code = firstText(document, "faultcode");
                return new SoapResult.Fault(normalizeCode(code), faultString.trim());
            }

            return new SoapResult.Unreachable(
                    "Respuesta de SUNAT no reconocida: " + truncate(new String(body, StandardCharsets.UTF_8)));

        } catch (Exception e) {
            return new SoapResult.Unreachable("No se pudo leer la respuesta de SUNAT: " + e.getMessage());
        }
    }

    /**
     * El faultcode llega como "soap-env:Client.1033". A SUNAT lo identifica el
     * numero del final, que es el codigo de error de su catalogo.
     */
    private String normalizeCode(String faultCode) {
        if (faultCode == null || faultCode.isBlank()) {
            return null;
        }
        int dot = faultCode.lastIndexOf('.');
        return dot >= 0 && dot < faultCode.length() - 1
                ? faultCode.substring(dot + 1)
                : faultCode.trim();
    }

    private byte[] firstXmlInZip(byte[] zip) {
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (entry.isDirectory() || !entry.getName().toLowerCase().endsWith(".xml")) {
                    continue;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                in.transferTo(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            log.error("No se pudo abrir el ZIP del CDR: {}", e.getMessage());
        }
        return null;
    }

    /** Primer elemento con ese nombre local, sin importar el prefijo que use. */
    private String firstText(Document document, String localName) {
        NodeList nodes = document.getElementsByTagNameNS("*", localName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String truncate(String value) {
        return value.length() <= 300 ? value : value.substring(0, 300) + "...";
    }
}
