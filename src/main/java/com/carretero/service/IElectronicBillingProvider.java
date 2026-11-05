package com.carretero.service;

import com.carretero.model.Invoice;
import com.carretero.service.billing.BillingOutcome;

import java.util.List;

/**
 * Camino por el que un comprobante llega (o no) a SUNAT.
 *
 * Existe para que cambiar de emision directa a un PSE contratado sea cambiar la
 * implementacion y no reescribir la facturacion. Quien la usa no sabe si detras
 * hay un SOAP a SUNAT, una API REST de un tercero o nada en absoluto.
 *
 * Ninguno de estos metodos lanza excepcion por un rechazo de SUNAT: un
 * comprobante rechazado es una respuesta valida del negocio, no una falla del
 * sistema, y tiene que quedar guardado con su motivo. Las excepciones quedan
 * para lo que si es una falla: no poder leer el certificado, no poder armar el
 * XML.
 */
public interface IElectronicBillingProvider {

    /**
     * Envia un comprobante individual. Es el camino de las facturas.
     *
     * Las boletas no se declaran una por una: van agrupadas en el resumen
     * diario, de modo que aqui devuelven PENDIENTE a la espera de ese resumen.
     */
    BillingOutcome send(Invoice invoice);

    /**
     * Envia el resumen diario con las boletas del dia.
     *
     * Es asincrono: SUNAT responde con un ticket, y el CDR se consulta despues
     * con {@link #checkTicket(String)}.
     */
    BillingOutcome sendDailySummary(List<Invoice> invoices);

    /** Consulta el resultado de un envio asincrono a partir de su ticket. */
    BillingOutcome checkTicket(String ticket);

    /** Nombre del proveedor, para los mensajes y el log. */
    String name();
}
