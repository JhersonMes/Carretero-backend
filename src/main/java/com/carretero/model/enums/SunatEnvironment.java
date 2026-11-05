package com.carretero.model.enums;

/**
 * Ambiente contra el que se emiten los comprobantes electronicos.
 *
 * Es una decision explicita del administrador, no algo que se deduzca de si hay
 * un token cargado en la configuracion. El dia que el local pase a produccion
 * tiene que ser un cambio visible y deliberado, no el efecto secundario de haber
 * llenado un campo.
 */
public enum SunatEnvironment {

    /**
     * Sin envio. El comprobante se genera, se numera y se imprime, pero no sale
     * del equipo. Es el modo por defecto mientras el sistema esta en desarrollo.
     */
    SIMULADO,

    /**
     * Homologacion de SUNAT: gratuita, sin PSE y sin valor tributario.
     *
     * Valida los mismos XML que produccion, de modo que probar aqui no le cuesta
     * nada al cliente. Usuario SOL {RUC}MODDATOS, clave MODDATOS.
     */
    BETA,

    /**
     * Emision real, con valor tributario. Exige certificado digital vigente y el
     * usuario SOL secundario del cliente.
     */
    PRODUCCION
}
