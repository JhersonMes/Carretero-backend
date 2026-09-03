package com.carretero.model.enums;

/**
 * Situacion de un comprobante frente a SUNAT.
 *
 * Estos valores describen el envio, no la validez del documento: una boleta en
 * SIMULADO esta bien emitida y bien numerada, pero nadie la declaro todavia.
 * Confundir SIMULADO con ACEPTADO es exactamente lo que hace que despues no se
 * pueda saber que comprobantes quedaron sin declarar.
 */
public enum SunatStatus {

    /** Emitido, todavia sin intentar el envio. */
    NO_ENVIADO,

    /**
     * Generado en modo local: no salio del equipo y no tiene CDR.
     *
     * Es un estado valido de operacion mientras el sistema esta en desarrollo,
     * pero no equivale a estar declarado.
     */
    SIMULADO,

    /** Enviado o en cola de envio, esperando la respuesta de SUNAT. */
    PENDIENTE,

    /** SUNAT respondio con un CDR de conformidad; el hash queda en cdrHash. */
    ACEPTADO,

    /** SUNAT lo rechazo. El motivo llega en sunatDescription. */
    RECHAZADO,

    /** Dado de baja, por anulacion de la venta o por reemision del comprobante. */
    ANULADO
}
