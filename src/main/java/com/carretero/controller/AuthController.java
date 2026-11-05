package com.carretero.controller;

import com.carretero.service.IPasswordResetService;
import com.carretero.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Contrasenas: cambio por el propio colaborador (CP-16-4-1) y recuperacion por correo
 * desde la pantalla de login (RF-06, CP-01-4-1).
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    static final String FORGOT_MESSAGE =
            "Si el correo esta registrado, te enviamos un enlace para crear una nueva contrasena. Revisa tu bandeja de entrada.";

    private final IUserService userService;
    private final IPasswordResetService passwordResetService;

    /** Requiere sesion: el colaborador cambia su propia contrasena. */
    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> body) throws Exception {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.changeOwnPassword(username, body.get("currentPassword"), body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Contrasena actualizada."));
    }

    /** Publico. Responde siempre lo mismo para no revelar que correos existen. */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> body) {
        passwordResetService.requestReset(body.get("email"));
        return ResponseEntity.ok(Map.of("message", FORGOT_MESSAGE));
    }

    /** Publico: se autoriza con el enlace de un solo uso recibido por correo. */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> body) throws Exception {
        passwordResetService.resetPassword(body.get("token"), body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Tu contrasena fue cambiada. Ya puedes iniciar sesion."));
    }
}
