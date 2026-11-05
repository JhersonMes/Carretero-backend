package com.carretero;

import com.carretero.dto.RoleDTO;
import com.carretero.dto.UserDTO;
import com.carretero.model.PasswordResetToken;
import com.carretero.model.Role;
import com.carretero.model.User;
import com.carretero.repository.IPasswordResetTokenRepository;
import com.carretero.repository.IRoleRepository;
import com.carretero.repository.IUserRepository;
import com.carretero.security.JwtUserDetailsService;
import com.carretero.service.implementation.PasswordResetService;
import com.carretero.service.implementation.UserService;
import com.carretero.service.mail.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de las cuentas del personal (HU-01, HU-16):
 * baja logica, edicion sin perder datos, cambio y recuperacion de contrasena.
 */
@ExtendWith(MockitoExtension.class)
public class CuentasYContrasenasTest {

    @Mock private IUserRepository userRepo;
    @Mock private IRoleRepository roleRepo;
    @Mock private IPasswordResetTokenRepository tokenRepo;
    @Mock private MailService mailService;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private UserService userService;
    private PasswordResetService resetService;
    private User mesero3;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepo, roleRepo, encoder);
        resetService = new PasswordResetService(userRepo, tokenRepo, userService, encoder, mailService);
        ReflectionTestUtils.setField(resetService, "frontendUrl", "http://localhost:4200");

        Role mesero = new Role();
        mesero.setIdRole(3);
        mesero.setName("MESERO");
        mesero3 = new User();
        mesero3.setIdUser(8);
        mesero3.setUsername("mesero3");
        mesero3.setEmail("mesero3@carretero.com");
        mesero3.setFullName("Mesero 3");
        mesero3.setPhone("976111222");
        mesero3.setPassword(encoder.encode("mesero3123"));
        mesero3.setRole(mesero);
    }

    // CASO DE PRUEBA CP-01-3-1: una cuenta dada de baja queda deshabilitada para Spring Security
    @Test
    void cuentaDadaDeBaja_noPuedeIniciarSesion() {
        // preparacion
        mesero3.setEnabled(false);
        when(userRepo.findOneByUsernameOrEmail("mesero3", "mesero3")).thenReturn(mesero3);

        // ejecucion
        UserDetails details = new JwtUserDetailsService(userRepo).loadUserByUsername("mesero3");

        // afirmacion: Spring Security rechaza las cuentas con isEnabled() = false
        assertFalse(details.isEnabled());
    }

    // CASO DE PRUEBA CP-01-3-1: el administrador no puede darse de baja a si mismo
    @Test
    void darDeBajaLaPropiaCuenta_seRechaza() {
        User admin = new User();
        admin.setIdUser(1);
        admin.setUsername("admin");
        when(userRepo.findById(1)).thenReturn(Optional.of(admin));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> userService.setEnabled(1, false, "admin"));
        assertEquals("No puedes dar de baja tu propia cuenta.", ex.getMessage());
        verify(userRepo, never()).save(any());
    }

    // CASO DE PRUEBA CP-16-2-1: corregir solo el correo conserva nombre, telefono y contrasena
    @Test
    void editarSoloElCorreo_conservaNombreTelefonoYContrasena() throws Exception {
        // preparacion
        String hashAnterior = mesero3.getPassword();
        when(userRepo.findById(8)).thenReturn(Optional.of(mesero3));
        when(userRepo.findOneByUsername("mesero3")).thenReturn(mesero3);
        when(userRepo.findOneByEmail("mesero3.nuevo@carretero.com")).thenReturn(null);
        when(userRepo.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserDTO dto = new UserDTO();
        dto.setUsername("mesero3");
        dto.setEmail("mesero3.nuevo@carretero.com");
        dto.setPassword("");

        // ejecucion
        User saved = userService.updateUser(8, dto);

        // afirmacion
        assertEquals("mesero3.nuevo@carretero.com", saved.getEmail());
        assertEquals("Mesero 3", saved.getFullName());
        assertEquals("976111222", saved.getPhone());
        assertEquals(hashAnterior, saved.getPassword());
    }

    // CASO DE PRUEBA CP-01-4-2: el administrador cambia la contrasena de caja2 desde Editar
    @Test
    void administradorCambiaLaContrasena_seGuardaCifrada() throws Exception {
        when(userRepo.findById(8)).thenReturn(Optional.of(mesero3));
        when(userRepo.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserDTO dto = new UserDTO();
        dto.setPassword("Caja2nueva");

        User saved = userService.updateUser(8, dto);

        assertTrue(encoder.matches("Caja2nueva", saved.getPassword()));
        assertFalse(encoder.matches("mesero3123", saved.getPassword()));
    }

    // CASO DE PRUEBA CP-16-1-1: no se crea una cuenta con un correo ya registrado
    @Test
    void crearCuentaConCorreoRepetido_seRechaza() {
        when(userRepo.findOneByUsername("caja5")).thenReturn(null);
        when(userRepo.findOneByEmail("mesero3@carretero.com")).thenReturn(mesero3);
        UserDTO dto = new UserDTO();
        dto.setUsername("caja5");
        dto.setEmail("mesero3@carretero.com");
        dto.setPassword("Caja5123");
        dto.setRole(new RoleDTO(2, "CAJERO", null));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> userService.createUser(dto));
        assertTrue(ex.getMessage().contains("ya esta registrado"));
    }

    // CASO DE PRUEBA CP-16-4-1: el colaborador cambia su contrasena con la actual correcta
    @Test
    void cambiarPropiaContrasena_conLaActualCorrecta() throws Exception {
        when(userRepo.findOneByUsername("mesero3")).thenReturn(mesero3);
        when(userRepo.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.changeOwnPassword("mesero3", "mesero3123", "Mesero3nueva");

        assertTrue(encoder.matches("Mesero3nueva", mesero3.getPassword()));
    }

    // CASO DE PRUEBA CP-16-4-1: con la contrasena actual equivocada no se cambia nada
    @Test
    void cambiarPropiaContrasena_conLaActualEquivocada_seRechaza() {
        when(userRepo.findOneByUsername("mesero3")).thenReturn(mesero3);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> userService.changeOwnPassword("mesero3", "otra", "Mesero3nueva"));
        assertEquals("La contrasena actual no es correcta.", ex.getMessage());
        verify(userRepo, never()).save(any());
    }

    // CASO DE PRUEBA CP-01-4-1: un correo que no existe no envia nada (no revela cuentas)
    @Test
    void recuperarConCorreoDesconocido_noEnviaNada() throws Exception {
        when(userRepo.findOneByEmail("nadie@gmail.com")).thenReturn(null);

        resetService.requestReset("nadie@gmail.com");

        verify(mailService, never()).send(anyString(), anyString(), anyString());
        verify(tokenRepo, never()).save(any());
    }

    // CASO DE PRUEBA CP-01-4-1: el correo trae un enlace que sirve una sola vez
    @Test
    void recuperarContrasena_enlaceFuncionaUnaSolaVez() throws Exception {
        // preparacion: pedir el enlace
        when(userRepo.findOneByEmail("mesero3@carretero.com")).thenReturn(mesero3);
        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        when(tokenRepo.save(saved.capture())).thenAnswer(i -> i.getArgument(0));
        resetService.requestReset("mesero3@carretero.com");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mailService).send(eq("mesero3@carretero.com"), anyString(), body.capture());
        Matcher m = Pattern.compile("reset-password\\?token=([A-Za-z0-9_-]+)").matcher(body.getValue());
        assertTrue(m.find(), "El correo debe traer el enlace");
        String token = m.group(1);
        PasswordResetToken entity = saved.getValue();
        assertNotEquals(token, entity.getTokenHash(), "En la base se guarda el hash, no el token");

        // ejecucion: usar el enlace
        when(tokenRepo.findByTokenHash(entity.getTokenHash())).thenReturn(Optional.of(entity));
        when(userRepo.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        resetService.resetPassword(token, "Mesero3nueva");

        // afirmacion
        assertTrue(encoder.matches("Mesero3nueva", mesero3.getPassword()));
        assertNotNull(entity.getUsedAt());
        IllegalStateException reuse = assertThrows(IllegalStateException.class,
                () -> resetService.resetPassword(token, "OtraClave1"));
        assertEquals("El enlace no es valido o ya fue usado.", reuse.getMessage());
    }

    // CASO DE PRUEBA CP-01-4-1: un enlace vencido no cambia la contrasena
    @Test
    void recuperarContrasena_enlaceVencido_seRechaza() {
        PasswordResetToken vencido = new PasswordResetToken();
        vencido.setUser(mesero3);
        vencido.setTokenHash("x");
        vencido.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(tokenRepo.findByTokenHash(anyString())).thenReturn(Optional.of(vencido));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> resetService.resetPassword("cualquiera", "Mesero3nueva"));
        assertTrue(ex.getMessage().startsWith("El enlace vencio"));
    }
}
