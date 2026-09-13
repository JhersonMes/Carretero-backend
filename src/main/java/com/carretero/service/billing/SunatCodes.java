package com.carretero.service.billing;

import com.carretero.model.enums.DocumentType;
import com.carretero.model.enums.InvoiceType;

/**
 * Codigos de los catalogos de SUNAT que usa la emision.
 *
 * Estan juntos porque los usan tanto el XML como el QR impreso, y cuando el
 * mismo codigo se escribe en dos sitios tarde o temprano uno de los dos se
 * queda atras.
 */
public final class SunatCodes {

    private SunatCodes() {
    }

    /** Catalogo 01: tipo de comprobante. */
    public static String documentType(InvoiceType type) {
        return type == InvoiceType.FACTURA ? "01" : "03";
    }

    /** Catalogo 06: tipo de documento de identidad del adquirente. */
    public static String identityType(DocumentType type) {
        if (type == null) {
            return "0";
        }
        return switch (type) {
            case DNI -> "1";
            case CE -> "4";
            case RUC -> "6";
            case PASAPORTE -> "7";
            case SIN_DOC -> "0";
        };
    }
}
