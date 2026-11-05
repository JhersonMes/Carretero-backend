package com.carretero.service.mail;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuracion del envio de correos (recuperacion de contrasena).
 *
 * mode = ARCHIVO (por defecto): el correo se guarda como .eml en outbox-dir. Sirve para
 *        desarrollo y para las pruebas automaticas, que leen el enlace desde ahi.
 * mode = SMTP: se envia de verdad por el servidor indicado (p. ej. smtp.gmail.com:465 con
 *        ssl=true y una "contrasena de aplicacion").
 */
@Data
@Component
@ConfigurationProperties(prefix = "carretero.mail")
public class MailProperties {
    private String mode = "ARCHIVO";
    private String outboxDir = "./correo-saliente";
    private String from = "no-responder@elcarretero.pe";
    private String host;
    private int port = 465;
    private boolean ssl = true;
    private String username;
    private String password;
    private int timeoutSeconds = 15;
}
