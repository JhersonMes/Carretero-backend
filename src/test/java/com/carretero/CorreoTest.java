package com.carretero;

import com.carretero.service.mail.MailProperties;
import com.carretero.service.mail.MailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas unitarias del envio de correos de recuperacion de contrasena (RF-06). */
public class CorreoTest {

    // CASO DE PRUEBA CP-01-4-1: en modo ARCHIVO el correo queda guardado como .eml
    @Test
    void modoArchivo_guardaElCorreo(@TempDir Path dir) throws Exception {
        MailProperties props = new MailProperties();
        props.setMode("ARCHIVO");
        props.setOutboxDir(dir.toString());

        new MailService(props).send("caja2@carretero.com", "Crea tu nueva contrasena", "Enlace: http://x/reset-password?token=abc");

        try (Stream<Path> files = Files.list(dir)) {
            List<Path> emls = files.toList();
            assertEquals(1, emls.size());
            String eml = Files.readString(emls.get(0), StandardCharsets.UTF_8);
            assertTrue(eml.contains("To: caja2@carretero.com"));
            assertTrue(eml.contains("reset-password?token=abc"));
        }
    }

    // CASO DE PRUEBA CP-01-4-1: en modo SMTP conversa con el servidor y entrega el mensaje
    @Test
    void modoSmtp_entregaElMensajeAlServidor() throws Exception {
        try (ServerSocket smtp = new ServerSocket(0)) {
            List<String> conversacion = new ArrayList<>();
            CompletableFuture<Void> servidor = CompletableFuture.runAsync(() -> atender(smtp, conversacion));

            MailProperties props = new MailProperties();
            props.setMode("SMTP");
            props.setHost("127.0.0.1");
            props.setPort(smtp.getLocalPort());
            props.setSsl(false);
            props.setUsername("carretero");
            props.setPassword("clave");
            new MailService(props).send("caja2@carretero.com", "Asunto", "Hola caja2");

            servidor.get(5, TimeUnit.SECONDS);
            assertTrue(conversacion.contains("AUTH LOGIN"));
            assertTrue(conversacion.contains("RCPT TO:<caja2@carretero.com>"));
            assertTrue(conversacion.contains("Hola caja2"));
            assertEquals("QUIT", conversacion.get(conversacion.size() - 1));
        }
    }

    /** Servidor SMTP minimo para la prueba. */
    private static void atender(ServerSocket server, List<String> log) {
        try (Socket s = server.accept();
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {
            out.print("220 prueba\r\n"); out.flush();
            String line;
            boolean data = false;
            int auth = 0;
            while ((line = in.readLine()) != null) {
                log.add(line);
                if (data) {
                    if (line.equals(".")) { data = false; out.print("250 ok\r\n"); out.flush(); }
                    continue;
                }
                if (auth > 0) { out.print(auth == 2 ? "334 x\r\n" : "235 ok\r\n"); out.flush(); auth--; continue; }
                if (line.startsWith("EHLO")) { out.print("250-prueba\r\n250 AUTH LOGIN\r\n"); }
                else if (line.equals("AUTH LOGIN")) { out.print("334 x\r\n"); auth = 2; }
                else if (line.equals("DATA")) { out.print("354 go\r\n"); data = true; }
                else if (line.equals("QUIT")) { out.print("221 bye\r\n"); out.flush(); break; }
                else { out.print("250 ok\r\n"); }
                out.flush();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
