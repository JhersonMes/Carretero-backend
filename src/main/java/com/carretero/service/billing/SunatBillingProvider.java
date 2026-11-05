package com.carretero.service.billing;

import com.carretero.config.SunatProperties;
import com.carretero.model.BusinessConfig;
import com.carretero.model.Invoice;
import com.carretero.model.enums.InvoiceType;
import com.carretero.model.enums.SunatEnvironment;
import com.carretero.repository.IBusinessConfigRepository;
import com.carretero.service.IElectronicBillingProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import java.util.List;

/**
 * Emision electronica contra SUNAT: arma el XML, lo firma, lo envia y lee el CDR.
 *
 * Una regla gobierna todo el metodo: <b>una falla al declarar no puede tumbar una
 * venta ya cobrada</b>. El dinero entro a la caja antes de que este codigo
 * corriera, asi que cualquier problema —el certificado vencido, SUNAT caida, el
 * XML mal armado— termina en un comprobante PENDIENTE con el motivo escrito, no
 * en una excepcion que le explote en la cara al cajero con la cola esperando.
 * Lo que queda pendiente se reintenta despues (F1-7).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SunatBillingProvider implements IElectronicBillingProvider {

    private final IBusinessConfigRepository configRepository;
    private final SunatProperties properties;
    private final UblInvoiceBuilder builder;
    private final UblSigner signer;
    private final BillingDocumentStore store;
    private final SunatSoapClient soapClient;
    private final CdrReader cdrReader;

    @Override
    public BillingOutcome send(Invoice invoice) {
        BusinessConfig config = configRepository.findFirstByActiveTrue().orElse(null);
        if (config == null || config.getRuc() == null || config.getRuc().isBlank()) {
            return BillingOutcome.pending(
                    "No hay RUC configurado en el negocio: no se puede declarar.", null, null);
        }

        String baseName = store.baseName(invoice, config.getRuc());
        String xmlPath = null;

        try {
            Document document = builder.build(invoice, config);
            signer.sign(document, signer.loadMaterial());

            byte[] xml = store.toBytes(document);
            xmlPath = store.saveSignedXml(invoice, baseName, xml);

            // Las boletas no se declaran una por una: se acumulan y salen en el
            // resumen diario. Queda firmada y guardada, esperando ese resumen.
            if (invoice.getInvoiceType() != InvoiceType.FACTURA) {
                return BillingOutcome.pending(
                        "Boleta firmada y guardada. Se declara en el resumen diario.",
                        xmlPath, null);
            }

            byte[] zip = store.zip(baseName, xml);
            SunatSoapClient.SoapResult result =
                    soapClient.sendBill(endpoint(config), baseName + ".zip", zip);

            return interpret(invoice, baseName, xmlPath, result);

        } catch (Exception e) {
            // Incluye el certificado ausente o vencido y el XML que no se pudo
            // armar. Se registra completo en el log y resumido en el comprobante.
            log.error("No se pudo preparar el envio del comprobante {}: {}",
                    invoice.getFullNumber(), e.getMessage(), e);
            return BillingOutcome.pending(
                    "Pendiente de envio: " + e.getMessage(), xmlPath, null);
        }
    }

    private BillingOutcome interpret(Invoice invoice, String baseName, String xmlPath,
                                     SunatSoapClient.SoapResult result) {

        if (result instanceof SunatSoapClient.SoapResult.Cdr received) {
            String cdrPath = store.saveCdr(invoice, baseName, received.content());
            log.info("CDR de {} guardado en {}", invoice.getFullNumber(), cdrPath);

            CdrReader.CdrContent cdr = cdrReader.read(received.content());
            if (cdr.accepted()) {
                return BillingOutcome.accepted(
                        cdr.responseCode(), cdr.description(),
                        store.hash(received.content()), xmlPath);
            }
            return BillingOutcome.rejected(cdr.responseCode(), cdr.description(), xmlPath);
        }

        if (result instanceof SunatSoapClient.SoapResult.Fault fault) {
            log.warn("SUNAT rechazo {}: [{}] {}",
                    invoice.getFullNumber(), fault.code(), fault.message());
            return BillingOutcome.rejected(fault.code(), fault.message(), xmlPath);
        }

        SunatSoapClient.SoapResult.Unreachable unreachable =
                (SunatSoapClient.SoapResult.Unreachable) result;
        log.warn("Comprobante {} queda pendiente: {}",
                invoice.getFullNumber(), unreachable.message());
        return BillingOutcome.pending(unreachable.message(), xmlPath, null);
    }

    /**
     * A donde se envia.
     *
     * business_config.sunatApiUrl permite apuntar a otro endpoint —un PSE, un
     * ambiente propio— sin recompilar. Si esta vacio manda el ambiente.
     */
    private String endpoint(BusinessConfig config) {
        String override = config.getSunatApiUrl();
        if (override != null && !override.isBlank()) {
            return override.trim();
        }
        return config.getSunatEnvironment() == SunatEnvironment.PRODUCCION
                ? properties.getProductionUrl()
                : properties.getBetaUrl();
    }

    @Override
    public BillingOutcome sendDailySummary(List<Invoice> invoices) {
        throw new UnsupportedOperationException(
                "El resumen diario de boletas todavia no esta implementado (F1-6 del plan).");
    }

    @Override
    public BillingOutcome checkTicket(String ticket) {
        throw new UnsupportedOperationException(
                "La consulta de ticket todavia no esta implementada (F1-6 del plan).");
    }

    @Override
    public String name() {
        return "SUNAT";
    }
}
