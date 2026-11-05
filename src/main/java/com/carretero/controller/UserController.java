package com.carretero.controller;

import com.carretero.dto.UserDTO;
import com.carretero.model.User;
import com.carretero.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Administracion de las cuentas del personal. Solo el administrador puede verlas o
 * cambiarlas (HU-16, RF-03): antes bastaba con haber iniciado sesion (CP-03-4-1).
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class UserController {

    private final IUserService service;
    @Qualifier("userMapper")
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<List<UserDTO>> findAll() throws Exception {
        List<UserDTO> list = service.findAll().stream()
                .map(e -> modelMapper.map(e, UserDTO.class))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> findById(@PathVariable("id") Integer id) throws Exception {
        User obj = service.findById(id);
        return ResponseEntity.ok(modelMapper.map(obj, UserDTO.class));
    }

    @PostMapping
    public ResponseEntity<Void> save(@Valid @RequestBody UserDTO dto) throws Exception {
        User obj = service.createUser(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(obj.getIdUser()).toUri();
        return ResponseEntity.created(location).build();
    }

    /**
     * Edita los datos de la cuenta. Lo que no viene en la peticion se conserva (antes se
     * borraban el nombre completo y el telefono, CP-16-2-1). La contrasena solo cambia si
     * se escribe una nueva (CP-01-4-2).
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> update(@Valid @RequestBody UserDTO dto, @PathVariable("id") Integer id) throws Exception {
        User obj = service.updateUser(id, dto);
        return ResponseEntity.ok(modelMapper.map(obj, UserDTO.class));
    }

    /** Baja logica (RN-04): la cuenta queda deshabilitada, nunca se borra. */
    @DeleteMapping("/{id}")
    public ResponseEntity<UserDTO> delete(@PathVariable("id") Integer id) throws Exception {
        User obj = service.setEnabled(id, false, currentUsername());
        return ResponseEntity.ok(modelMapper.map(obj, UserDTO.class));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<UserDTO> activate(@PathVariable("id") Integer id) throws Exception {
        User obj = service.setEnabled(id, true, currentUsername());
        return ResponseEntity.ok(modelMapper.map(obj, UserDTO.class));
    }

    @GetMapping("/pageable")
    public ResponseEntity<Page<User>> listPageable(Pageable pageable) {
        Page<User> page = service.listPage(pageable);
        return ResponseEntity.ok(page);
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
