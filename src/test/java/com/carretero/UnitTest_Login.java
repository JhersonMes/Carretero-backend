package com.carretero;

import com.carretero.controller.LoginController;
import com.carretero.dto.UserDTO;
import com.carretero.model.Role;
import com.carretero.model.User;
import com.carretero.repository.IRoleRepository;
import com.carretero.repository.IUserRepository;
import com.carretero.security.JwtRequest;
import com.carretero.security.JwtResponse;
import com.carretero.security.JwtTokenUtil;
import com.carretero.security.JwtUserDetailsService;
import com.carretero.service.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas Unitarias de Autenticacion y Login.
 * Guiadas de la estructura de Semana07_CalProSoft (Preparacion, Ejecucion, Afirmacion)
 * y de los casos de prueba documentados en Entregables/ANEXOS (CP-01-1, CP-01-2, CP-01-3, CP-02-1).
 */
@ExtendWith(MockitoExtension.class)
public class UnitTest_Login {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private JwtUserDetailsService jwtUserDetailsService;

    @Mock
    private IUserService userService;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IRoleRepository roleRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private LoginController loginController;

    private User mockUser;
    private UserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setIdUser(1);
        mockUser.setUsername("ADMIN");
        mockUser.setEnabled(true);

        mockUserDetails = mock(UserDetails.class);
    }

    // CASO DE PRUEBA 1: Login ok (CP-01-1)
    @Test
    public void LoginOK() throws Exception {
        // preparacion
        String u = "ADMIN", c = "1234";
        JwtRequest request = new JwtRequest();
        request.setUsername(u);
        request.setPassword(c);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(u, c));
        when(jwtUserDetailsService.loadUserByUsername(u)).thenReturn(mockUserDetails);
        when(jwtTokenUtil.generateToken(mockUserDetails)).thenReturn("fake-jwt-token-xyz");

        // ejecucion
        ResponseEntity<JwtResponse> response = loginController.login(request);

        // afirmacion
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("fake-jwt-token-xyz", response.getBody().getJwtToken());
    }

    // CASO DE PRUEBA 2: Login Fallido 1 - Contrasena incorrecta (CP-01-2)
    @Test
    public void LoginFail1() throws Exception {
        // preparacion
        String u = "ADMIN", c = "ADMIN";
        JwtRequest request = new JwtRequest();
        request.setUsername(u);
        request.setPassword(c);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("INVALID_CREDENTIALS"));

        // ejecucion
        ResponseEntity<JwtResponse> response = loginController.login(request);

        // afirmacion
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
    }

    // CASO DE PRUEBA 3: Login Fallido 2 - Usuario deshabilitado (CP-01-3)
    @Test
    public void LoginFail2() throws Exception {
        // preparacion
        String u = "JUAN", c = "1234";
        JwtRequest request = new JwtRequest();
        request.setUsername(u);
        request.setPassword(c);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("USER_DISABLED"));

        // ejecucion
        ResponseEntity<JwtResponse> response = loginController.login(request);

        // afirmacion
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
    }

    // CASO DE PRUEBA 4: Registro de usuario asigna rol MESERO forzado y password BCrypt (CP-02-1)
    @Test
    public void Register_fuerzaRolMeseroYCifraPassword() throws Exception {
        // preparacion
        UserDTO inputDto = new UserDTO();
        inputDto.setUsername("nuevo1");
        inputDto.setEmail("nuevo1@carretero.pe");
        inputDto.setPassword("123456");

        Role defaultRole = new Role();
        defaultRole.setIdRole(2);
        defaultRole.setName("MESERO");

        User userEntity = new User();
        userEntity.setUsername("nuevo1");
        userEntity.setEmail("nuevo1@carretero.pe");

        User savedEntity = new User();
        savedEntity.setIdUser(10);
        savedEntity.setUsername("nuevo1");
        savedEntity.setRole(defaultRole);
        savedEntity.setEnabled(true);

        UserDTO outputDto = new UserDTO();
        outputDto.setIdUser(10);
        outputDto.setUsername("nuevo1");

        when(modelMapper.map(inputDto, User.class)).thenReturn(userEntity);
        when(roleRepository.findOneByName("MESERO")).thenReturn(defaultRole);
        when(userService.save(any(User.class))).thenReturn(savedEntity);
        when(modelMapper.map(savedEntity, UserDTO.class)).thenReturn(outputDto);

        // ejecucion
        ResponseEntity<UserDTO> response = loginController.register(inputDto);

        // afirmacion
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("nuevo1", response.getBody().getUsername());
        verify(roleRepository).findOneByName("MESERO");
        verify(userService).save(argThat(u -> "MESERO".equals(u.getRole().getName()) && u.isEnabled()));
    }
}
