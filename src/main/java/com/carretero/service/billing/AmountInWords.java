package com.carretero.service.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Importe en letras, como lo exige SUNAT en la leyenda 1000 del comprobante.
 *
 * El frontend ya escribe esta misma leyenda para el ticket impreso, pero el XML
 * no puede depender de eso: lo que se declara se arma en el servidor.
 */
public final class AmountInWords {

    private AmountInWords() {
    }

    private static final String[] UNITS = {
            "", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
            "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISEIS", "DIECISIETE",
            "DIECIOCHO", "DIECINUEVE", "VEINTE", "VEINTIUNO", "VEINTIDOS", "VEINTITRES",
            "VEINTICUATRO", "VEINTICINCO", "VEINTISEIS", "VEINTISIETE", "VEINTIOCHO", "VEINTINUEVE"
    };

    private static final String[] TENS = {
            "", "", "VEINTE", "TREINTA", "CUARENTA", "CINCUENTA",
            "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"
    };

    private static final String[] HUNDREDS = {
            "", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
            "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"
    };

    /** Devuelve, por ejemplo, "SON: CIENTO DIECIOCHO CON 50/100 SOLES". */
    public static String of(BigDecimal amount) {
        BigDecimal rounded = (amount == null ? BigDecimal.ZERO : amount).setScale(2, RoundingMode.HALF_UP);
        long whole = rounded.longValue();
        int cents = rounded.subtract(BigDecimal.valueOf(whole)).movePointRight(2).abs().intValue();
        return String.format("SON: %s CON %02d/100 SOLES", words(whole), cents);
    }

    private static String words(long n) {
        if (n == 0) {
            return "CERO";
        }
        StringBuilder sb = new StringBuilder();

        long millions = n / 1_000_000;
        long thousands = (n % 1_000_000) / 1_000;
        long rest = n % 1_000;

        if (millions > 0) {
            sb.append(millions == 1 ? "UN MILLON" : belowThousand((int) millions) + " MILLONES");
        }
        if (thousands > 0) {
            append(sb, thousands == 1 ? "MIL" : belowThousand((int) thousands) + " MIL");
        }
        if (rest > 0) {
            append(sb, belowThousand((int) rest));
        }
        return sb.toString();
    }

    private static void append(StringBuilder sb, String part) {
        if (sb.length() > 0) {
            sb.append(" ");
        }
        sb.append(part);
    }

    private static String belowThousand(int n) {
        if (n == 0) {
            return "";
        }
        // "CIEN" solo cuando son cien exactos; 101 es "CIENTO UNO".
        if (n == 100) {
            return "CIEN";
        }

        StringBuilder sb = new StringBuilder();
        int hundreds = n / 100;
        int rest = n % 100;

        if (hundreds > 0) {
            sb.append(HUNDREDS[hundreds]);
        }
        if (rest > 0) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            if (rest < 30) {
                sb.append(UNITS[rest]);
            } else {
                sb.append(TENS[rest / 10]);
                if (rest % 10 > 0) {
                    sb.append(" Y ").append(UNITS[rest % 10]);
                }
            }
        }
        return sb.toString();
    }
}
