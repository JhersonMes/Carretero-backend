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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
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
        User obj = service.save(modelMapper.map(dto, User.class));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(obj.getIdUser()).toUri();
        return ResponseEntity.created(location).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> update(@Valid @RequestBody UserDTO dto, @PathVariable("id") Integer id) throws Exception {
        User obj = service.update(modelMapper.map(dto, User.class), id);
        return ResponseEntity.ok(modelMapper.map(obj, UserDTO.class));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) throws Exception {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pageable")
    public ResponseEntity<Page<User>> listPageable(Pageable pageable) {
        Page<User> page = service.listPage(pageable);
        return ResponseEntity.ok(page);
    }
}
