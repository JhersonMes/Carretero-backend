package com.carretero.service.implementation;

import com.carretero.model.PasswordResetToken;
import com.carretero.model.User;
import com.carretero.repository.IPasswordResetTokenRepository;
import com.carretero.repository.IUserRepository;
import com.carretero.service.IPasswordResetService;
import com.carretero.service.IUserService;
import com.carretero.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/** Recuperacion de contrasena por correo (RF-06, CP-01-4-1). */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService implements IPasswordResetService {

    /** Vigencia del enlace enviado por correo. */
    public static final int TOKEN_MINUTES = 30;

    private final IUserRepository userRepo;
    private final IPasswordResetTokenRepository tokenRepo;
    private final IUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final SecureRandom random = new SecureRandom();

    @Value("${carretero.app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    @Transactional
    public void requestReset(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Escribe el correo con el que te registraron.");
        }
        User user = userRepo.findOneByEmail(email.trim());
        if (user == null || !user.isEnabled()) {
            log.info("Recuperacion pedida para un correo sin cuenta activa; no se envia nada.");
            return;
        }

        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        PasswordResetToken entity = new PasswordResetToken();
        entity.setUser(user);
        entity.setTokenHash(sha256(token));
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(TOKEN_MINUTES));
        tokenRepo.save(entity);

        String link = frontendUrl.replaceAll("/+$", "") + "/reset-password?token=" + token;
        String name = user.getFullName() != null ? user.getFullName() : user.getUsername();
        String body = "Hola " + name + ":\n\n"
                + "Recibimos un pedido para crear una nueva contrasena de tu cuenta \"" + user.getUsername()
                + "\" en el Sistema de Ventas El Carretero.\n\n"
                + "Abre este enlace para crearla (vence en " + TOKEN_MINUTES + " minutos y sirve una sola vez):\n"
                + link + "\n\n"
                + "Si no lo pediste, ignora este correo: tu contrasena actual sigue funcionando.\n";
        try {
            mailService.send(user.getEmail(), "Crea tu nueva contrasena - El Carretero", body);
        } catch (Exception e) {
            log.error("No se pudo enviar el correo de recuperacion a {}: {}", user.getEmail(), e.getMessage());
            throw new IllegalStateException("No se pudo enviar el correo en este momento. Intenta de nuevo o pide el cambio al administrador.");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String token, String newPassword) throws Exception {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("El enlace no es valido.");
        }
        PasswordResetToken entity = tokenRepo.findByTokenHash(sha256(token))
                .orElseThrow(() -> new IllegalStateException("El enlace no es valido o ya fue usado."));
        if (entity.getUsedAt() != null) {
            throw new IllegalStateException("El enlace no es valido o ya fue usado.");
        }
        if (entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("El enlace vencio. Pide uno nuevo desde \"¿Olvidaste tu contrasena?\".");
        }
        userService.validateNewPassword(newPassword);

        User user = entity.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        entity.setUsedAt(LocalDateTime.now());
        tokenRepo.save(entity);
    }

    static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
