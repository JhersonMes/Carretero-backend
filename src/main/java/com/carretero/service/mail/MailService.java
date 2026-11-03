package com.carretero.service.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

/**
 * Envio de correos de texto plano sin dependencias externas.
 * Ver {@link MailProperties} para los dos modos (ARCHIVO y SMTP).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final MailProperties props;

    public void send(String to, String subject, String body) throws IOException {
        String message = buildMessage(to, subject, body);
        if ("SMTP".equalsIgnoreCase(props.getMode()) && props.getHost() != null && !props.getHost().isBlank()) {
            sendSmtp(to, message);
            log.info("Correo enviado por SMTP a {}", to);
        } else {
            Path dir = Path.of(props.getOutboxDir());
            Files.createDirectories(dir);
            Path file = dir.resolve(System.currentTimeMillis() + "-" + to.replaceAll("[^A-Za-z0-9@._-]", "_") + ".eml");
            Files.writeString(file, message, StandardCharsets.UTF_8);
            log.info("Correo guardado en {} (modo ARCHIVO)", file.toAbsolutePath());
        }
    }

    private String buildMessage(String to, String subject, String body) {
        String encodedSubject = "=?UTF-8?B?" + Base64.getEncoder().encodeToString(subject.getBytes(StandardCharsets.UTF_8)) + "?=";
        return "From: El Carretero <" + props.getFrom() + ">\r\n"
                + "To: " + to + "\r\n"
                + "Subject: " + encodedSubject + "\r\n"
                + "Date: " + ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME) + "\r\n"
                + "Message-ID: <" + UUID.randomUUID() + "@elcarretero.pe>\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n"
                + "Content-Transfer-Encoding: 8bit\r\n"
                + "\r\n"
                + body.replace("\r\n", "\n").replace("\n", "\r\n")
                // un punto al inicio de linea cierra el mensaje en SMTP: se duplica
                .replace("\r\n.", "\r\n..")
                + "\r\n";
    }

    private void sendSmtp(String to, String message) throws IOException {
        int timeout = props.getTimeoutSeconds() * 1000;
        Socket socket = props.isSsl()
                ? SSLSocketFactory.getDefault().createSocket()
                : new Socket();
        socket.connect(new InetSocketAddress(props.getHost(), props.getPort()), timeout);
        socket.setSoTimeout(timeout);
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             Writer out = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)) {
            expect(in, 220);
            command(out, in, "EHLO elcarretero.pe", 250);
            if (props.getUsername() != null && !props.getUsername().isBlank()) {
                command(out, in, "AUTH LOGIN", 334);
                command(out, in, b64(props.getUsername()), 334);
                command(out, in, b64(props.getPassword() == null ? "" : props.getPassword()), 235);
            }
            command(out, in, "MAIL FROM:<" + props.getFrom() + ">", 250);
            command(out, in, "RCPT TO:<" + to + ">", 250);
            command(out, in, "DATA", 354);
            out.write(message);
            out.write("\r\n.\r\n");
            out.flush();
            expect(in, 250);
            command(out, in, "QUIT", 221);
        }
    }

    private static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static void command(Writer out, BufferedReader in, String cmd, int expectedCode) throws IOException {
        out.write(cmd + "\r\n");
        out.flush();
        expect(in, expectedCode);
    }

    /** Lee la respuesta (incluidas las de varias lineas "250-...") y valida el codigo. */
    private static void expect(BufferedReader in, int expectedCode) throws IOException {
        String line;
        do {
            line = in.readLine();
            if (line == null) throw new IOException("El servidor de correo cerro la conexion");
        } while (line.length() > 3 && line.charAt(3) == '-');
        if (!line.startsWith(String.valueOf(expectedCode))) {
            throw new IOException("Respuesta inesperada del servidor de correo: " + line);
        }
    }
}
