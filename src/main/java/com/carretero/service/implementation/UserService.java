package com.carretero.service.implementation;

import com.carretero.dto.UserDTO;
import com.carretero.exception.ModelNotFoundException;
import com.carretero.model.Role;
import com.carretero.model.User;
import com.carretero.repository.IGenericRepository;
import com.carretero.repository.IRoleRepository;
import com.carretero.repository.IUserRepository;
import com.carretero.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService extends GenericService<User, Integer> implements IUserService {

    /** Minimo de caracteres para cualquier contrasena nueva. */
    public static final int MIN_PASSWORD_LENGTH = 6;

    private final IUserRepository repo;
    private final IRoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    protected IGenericRepository<User, Integer> getRepo() {
        return repo;
    }

    @Override
    public User save(User user) throws Exception {
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return super.save(user);
    }

    @Override
    public User update(User user, Integer id) throws Exception {
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            User existingUser = repo.findById(id).orElse(null);
            if (existingUser != null) {
                user.setPassword(existingUser.getPassword());
            }
        }
        return super.update(user, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User createUser(UserDTO dto) throws Exception {
        String username = clean(dto.getUsername());
        String email = clean(dto.getEmail());
        requireUniqueUsernameAndEmail(username, email, null);
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new IllegalStateException("Escribe una contrasena para la cuenta nueva.");
        }
        validateNewPassword(dto.getPassword());

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(blankToNull(dto.getFullName()));
        user.setPhone(blankToNull(dto.getPhone()));
        user.setRole(resolveRole(dto));
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        return repo.save(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User updateUser(Integer id, UserDTO dto) throws Exception {
        User user = repo.findById(id).orElseThrow(() -> new ModelNotFoundException("Usuario no encontrado: " + id));

        if (dto.getUsername() != null) {
            String username = clean(dto.getUsername());
            requireUniqueUsernameAndEmail(username, null, id);
            user.setUsername(username);
        }
        if (dto.getEmail() != null) {
            String email = clean(dto.getEmail());
            requireUniqueUsernameAndEmail(null, email, id);
            user.setEmail(email);
        }
        // Lo que el formulario no envia se conserva: antes se perdian el nombre y el telefono.
        if (dto.getFullName() != null) user.setFullName(blankToNull(dto.getFullName()));
        if (dto.getPhone() != null) user.setPhone(blankToNull(dto.getPhone()));
        if (dto.getRole() != null && (dto.getRole().getIdRole() != null || dto.getRole().getName() != null)) {
            user.setRole(resolveRole(dto));
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            validateNewPassword(dto.getPassword());
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        // enabled no se toca aqui: tiene su propia accion (baja / reactivar).
        return repo.save(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User setEnabled(Integer id, boolean enabled, String actorUsername) throws Exception {
        User user = repo.findById(id).orElseThrow(() -> new ModelNotFoundException("Usuario no encontrado: " + id));
        if (!enabled && Objects.equals(user.getUsername(), actorUsername)) {
            throw new IllegalStateException("No puedes dar de baja tu propia cuenta.");
        }
        user.setEnabled(enabled);
        return repo.save(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeOwnPassword(String username, String currentPassword, String newPassword) throws Exception {
        User user = repo.findOneByUsername(username);
        if (user == null) {
            throw new ModelNotFoundException("Usuario no encontrado: " + username);
        }
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalStateException("La contrasena actual no es correcta.");
        }
        validateNewPassword(newPassword);
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalStateException("La contrasena nueva debe ser distinta de la actual.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        repo.save(user);
    }

    @Override
    public void validateNewPassword(String newPassword) {
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("La contrasena debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres.");
        }
    }

    // ---------------------------------------------------------------------------

    private void requireUniqueUsernameAndEmail(String username, String email, Integer idBeingEdited) {
        if (username != null) {
            User other = repo.findOneByUsername(username);
            if (other != null && !Objects.equals(other.getIdUser(), idBeingEdited)) {
                throw new IllegalStateException("Ya existe un usuario llamado \"" + username + "\".");
            }
        }
        if (email != null) {
            User other = repo.findOneByEmail(email);
            if (other != null && !Objects.equals(other.getIdUser(), idBeingEdited)) {
                throw new IllegalStateException("El correo " + email + " ya esta registrado en otra cuenta.");
            }
        }
    }

    private Role resolveRole(UserDTO dto) {
        Role role = null;
        if (dto.getRole() != null) {
            if (dto.getRole().getIdRole() != null) role = roleRepo.findById(dto.getRole().getIdRole()).orElse(null);
            if (role == null && dto.getRole().getName() != null) role = roleRepo.findOneByName(dto.getRole().getName());
        }
        if (role == null) {
            throw new IllegalStateException("Elige el rol de la cuenta.");
        }
        return role;
    }

    private static String clean(String s) {
        return s == null ? null : s.trim();
    }

    private static String blankToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }
}
