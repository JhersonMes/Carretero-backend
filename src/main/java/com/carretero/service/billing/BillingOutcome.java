package com.carretero.service.billing;

import com.carretero.model.enums.SunatStatus;

/**
 * Resultado de intentar declarar un comprobante.
 *
 * Es lo unico que un proveedor devuelve, y de aqui salen los campos que se
 * guardan en el comprobante. Que el CDR sea un campo aparte y no un texto
 * cualquiera es a proposito: el hash solo se llena cuando hubo un CDR de
 * verdad, para que "tiene constancia" y "no la tiene" nunca se confundan.
 *
 * @param status      estado en el que queda el comprobante
 * @param responseCode codigo devuelto por SUNAT; null si no hubo respuesta
 * @param description  motivo legible, tanto de la aceptacion como del rechazo
 * @param cdrHash      hash del CDR. Solo se llena si SUNAT respondio
 * @param xmlPath      ruta del XML firmado en disco; null si no se llego a firmar
 * @param ticket       ticket de un envio asincrono; null en los sincronos
 */
public record BillingOutcome(
        SunatStatus status,
        String responseCode,
        String description,
        String cdrHash,
        String xmlPath,
        String ticket
) {

    /** El comprobante se genero pero no se envio a ninguna parte. */
    public static BillingOutcome simulated() {
        return new BillingOutcome(
                SunatStatus.SIMULADO, null,
                "Modo local: comprobante no enviado a SUNAT",
                null, null, null);
    }

    /** SUNAT respondio con un CDR de conformidad. */
    public static BillingOutcome accepted(String code, String description, String cdrHash, String xmlPath) {
        return new BillingOutcome(SunatStatus.ACEPTADO, code, description, cdrHash, xmlPath, null);
    }

    /** SUNAT rechazo el comprobante. El motivo es lo que hay que mostrar. */
    public static BillingOutcome rejected(String code, String description, String xmlPath) {
        return new BillingOutcome(SunatStatus.RECHAZADO, code, description, null, xmlPath, null);
    }

    /**
     * Queda a la espera: por un corte de red, porque falta el resumen diario o
     * porque el envio es asincrono y todavia no hay CDR.
     */
    public static BillingOutcome pending(String description, String xmlPath, String ticket) {
        return new BillingOutcome(SunatStatus.PENDIENTE, null, description, null, xmlPath, ticket);
    }
}
