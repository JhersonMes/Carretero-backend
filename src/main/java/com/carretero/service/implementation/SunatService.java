package com.carretero.service.implementation;

import com.carretero.dto.DniRucQueryResponseDTO;
import com.carretero.model.BusinessConfig;
import com.carretero.model.Client;
import com.carretero.model.Invoice;
import com.carretero.model.enums.DocumentType;
import com.carretero.model.enums.InvoiceType;
import com.carretero.model.enums.SunatEnvironment;
import com.carretero.model.enums.SunatStatus;
import com.carretero.repository.IBusinessConfigRepository;
import com.carretero.repository.IClientRepository;
import com.carretero.service.IElectronicBillingProvider;
import com.carretero.service.ISunatService;
import com.carretero.service.billing.BillingOutcome;
import com.carretero.service.billing.SimulatedBillingProvider;
import com.carretero.service.billing.SunatBillingProvider;
import com.carretero.service.billing.SunatCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SunatService implements ISunatService {

    private final IClientRepository clientRepository;
    private final IBusinessConfigRepository businessConfigRepository;

    // Los dos caminos posibles. Se eligen por ambiente, no por si hay token: ver
    // BusinessConfig.sunatEnvironment.
    private final SimulatedBillingProvider simulatedProvider;
    private final SunatBillingProvider sunatProvider;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Transaccional porque la respuesta lee las direcciones del cliente local, que
     * son LAZY: fuera de una sesion abierta esa lectura falla.
     */
    @Override
    @Transactional(readOnly = true)
    public DniRucQueryResponseDTO queryDocument(String docNumber) {
        if (docNumber == null || docNumber.trim().isEmpty()) {
            return new DniRucQueryResponseDTO(false, null, docNumber, null, null, null, "Número de documento vacío", false);
        }

        String doc = docNumber.trim();
        DocumentType docType = (doc.length() == 8) ? DocumentType.DNI : (doc.length() == 11 ? DocumentType.RUC : DocumentType.CE);

        // 1. Consultar en Base de Datos local primero (Caché sin costo ni internet)
        Optional<Client> localClient = clientRepository.findAllByDocNumber(doc).stream().findFirst();
        if (localClient.isPresent()) {
            Client c = localClient.get();
            return new DniRucQueryResponseDTO(
                    true,
                    c.getDocType(),
                    c.getDocNumber(),
                    c.getName(),
                    c.getAddresses().isEmpty() ? null : c.getAddresses().get(0).getStreet(),
                    "ACTIVO",
                    "Obtenido desde la base de datos local",
                    true
            );
        }

        // 2. Consultar mediante API externa configurada (ej. Decolecta / ApiPeru / Apis.net.pe)
        Optional<BusinessConfig> configOpt = businessConfigRepository.findFirstByActiveTrue();
        if (configOpt.isPresent() && configOpt.get().getDniRucApiToken() != null && !configOpt.get().getDniRucApiToken().trim().isEmpty()) {
            BusinessConfig config = configOpt.get();
            try {
                String url = config.getDniRucApiUrl();
                if (url == null || url.trim().isEmpty()) {
                    url = "https://apiperu.dev/api";
                }

                String endpoint = (docType == DocumentType.DNI) ? url + "/dni/" + doc : url + "/ruc/" + doc;

                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(config.getDniRucApiToken());
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Void> entity = new HttpEntity<>(headers);
                ResponseEntity<Map> response = restTemplate.exchange(endpoint, HttpMethod.GET, entity, Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map<String, Object> body = response.getBody();
                    Map<String, Object> data = (Map<String, Object>) body.get("data");

                    if (data != null) {
                        String name = (docType == DocumentType.DNI)
                                ? String.format("%s %s %s",
                                data.getOrDefault("nombres", ""),
                                data.getOrDefault("apellido_paterno", ""),
                                data.getOrDefault("apellido_materno", "")).trim()
                                : String.valueOf(data.getOrDefault("nombre_o_razon_social", ""));

                        String address = String.valueOf(data.getOrDefault("direccion_completa", ""));
                        String condition = String.valueOf(data.getOrDefault("condicion", "HABIDO"));

                        // Auto-guardar en BD local para futuras consultas offline
                        Client newClient = new Client();
                        newClient.setDocType(docType);
                        newClient.setDocNumber(doc);
                        newClient.setName(name);
                        clientRepository.save(newClient);

                        return new DniRucQueryResponseDTO(true, docType, doc, name, address, condition, "Consulta exitosa en línea", false);
                    }
                }
            } catch (Exception e) {
                log.warn("Error al consultar API externa de DNI/RUC para {}: {}", doc, e.getMessage());
            }
        }

        // 3. Fallback simulado si no hay internet o no hay token configurado
        String mockName = (docType == DocumentType.DNI) ? "CLIENTE DNI " + doc : "EMPRESA RUC " + doc + " S.A.C.";
        return new DniRucQueryResponseDTO(
                true,
                docType,
                doc,
                mockName,
                "Av. Principal 123",
                "ACTIVO",
                "Consulta local de prueba (configure el token de API para datos de Reniec/Sunat en tiempo real)",
                false
        );
    }

    /**
     * Declara el comprobante y le deja el estado que devolvio el envio.
     *
     * Aqui solo se decide por que camino va y se vuelca el resultado; el trabajo
     * de armar, firmar y enviar vive detras de {@link IElectronicBillingProvider},
     * para que cambiar a un PSE contratado no obligue a tocar la facturacion.
     *
     * Este metodo no lanza excepcion nunca: el comprobante se emite despues de
     * cobrar, y una falla al declarar tiene que quedar escrita en el comprobante,
     * no interrumpir el cobro.
     */
    @Override
    public Invoice dispatchToSunat(Invoice invoice) {
        // La nota de venta es un documento interno del local: no se declara nunca.
        if (invoice.getInvoiceType() == InvoiceType.NOTA_VENTA) {
            invoice.setSunatStatus(SunatStatus.NO_ENVIADO);
            invoice.setSunatResponseCode(null);
            invoice.setSunatDescription("Nota de venta interna (no se declara a SUNAT)");
            invoice.setCdrHash(null);
            return invoice;
        }

        BusinessConfig config = businessConfigRepository.findFirstByActiveTrue().orElse(null);
        SunatEnvironment environment = (config != null && config.getSunatEnvironment() != null)
                ? config.getSunatEnvironment()
                : SunatEnvironment.SIMULADO;

        // El QR se arma con los datos del propio comprobante, no con la respuesta
        // de SUNAT: va impreso en el ticket se haya enviado o no.
        invoice.setQrData(buildQrData(invoice, config));

        IElectronicBillingProvider provider = (environment == SunatEnvironment.SIMULADO)
                ? simulatedProvider
                : sunatProvider;

        return apply(invoice, provider.send(invoice));
    }

    /**
     * Vuelca el resultado del envio sobre el comprobante.
     *
     * Las descripciones de SUNAT pueden ser largas y las columnas tienen tope;
     * se recortan aqui y no en cada proveedor, porque el limite es de la tabla.
     */
    private Invoice apply(Invoice invoice, BillingOutcome outcome) {
        invoice.setSunatStatus(outcome.status());
        invoice.setSunatResponseCode(truncate(outcome.responseCode(), 20));
        invoice.setSunatDescription(truncate(outcome.description(), 500));
        invoice.setCdrHash(outcome.cdrHash());

        // Solo se pisa si hubo XML: un reintento fallido no debe borrar la ruta
        // del que ya se habia firmado y guardado.
        if (outcome.xmlPath() != null) {
            invoice.setXmlUrl(outcome.xmlPath());
        }
        return invoice;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    /**
     * Cadena del codigo QR del comprobante, en el orden que define SUNAT:
     * RUC | tipo | serie | correlativo | IGV | total | fecha | tipo doc. | nro. doc.
     *
     * Antes habia dos formatos distintos segun la rama que se tomara, uno de siete
     * campos y otro de nueve. Dos comprobantes del mismo tipo salian impresos con
     * QR de formato distinto segun como estuviera configurado el sistema ese dia.
     */
    private String buildQrData(Invoice invoice, BusinessConfig config) {
        String issuerRuc = (config != null && config.getRuc() != null && !config.getRuc().isBlank())
                ? config.getRuc()
                : "00000000000";

        Client buyer = invoice.getClient();
        boolean identified = buyer != null
                && buyer.getDocNumber() != null
                && !buyer.getDocNumber().isBlank();

        return String.format("%s|%s|%s|%08d|%.2f|%.2f|%s|%s|%s",
                issuerRuc,
                SunatCodes.documentType(invoice.getInvoiceType()),
                invoice.getSeries(),
                invoice.getCorrelativeNumber(),
                invoice.getIgvAmount(),
                invoice.getTotalAmount(),
                invoice.getIssueDate().toLocalDate(),
                identified ? SunatCodes.identityType(buyer.getDocType()) : "0",
                identified ? buyer.getDocNumber() : "-");
    }
}
