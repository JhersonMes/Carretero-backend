package com.carretero.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Credenciales y rutas de la emision electronica.
 *
 * Viven aqui y no en business_config a proposito: la clave SOL y la del
 * certificado son secretos, y el resto de secretos del sistema (la contrasena de
 * la base, el JWT_SECRET) ya se guardan en el .env. Un secreto en una tabla lo
 * lee cualquiera que abra la base, y ademas viajaria en el DTO de configuracion.
 *
 * Lo que si vive en la base es el ambiente (business_config.sunat_environment),
 * porque es una decision del negocio y no un secreto.
 */
@Data
@Component
@ConfigurationProperties(prefix = "carretero.sunat")
public class SunatProperties {

    /** Endpoint del ambiente de homologacion, gratuito y sin valor tributario. */
    private String betaUrl = "https://e-beta.sunat.gob.pe/ol-ti-itcpfegem-beta/billService";

    /** Endpoint de produccion. */
    private String productionUrl = "https://e-factura.sunat.gob.pe/ol-ti-itcpfegem/billService";

    /**
     * Usuario SOL secundario. En beta es {RUC}MODDATOS.
     * Ejemplo para este local: 20610046097MODDATOS
     */
    private String solUser = "";

    /** Clave SOL. En beta es MODDATOS. */
    private String solPassword = "";

    /** Ruta del certificado .pfx / .p12 con el que se firma. */
    private String certificatePath = "";

    /** Clave del certificado. */
    private String certificatePassword = "";

    /** Carpeta donde se guardan los XML firmados y los CDR recibidos. */
    private String storageDir = "./comprobantes";

    /** Segundos de espera del envio antes de darlo por pendiente. */
    private int timeoutSeconds = 30;
}
