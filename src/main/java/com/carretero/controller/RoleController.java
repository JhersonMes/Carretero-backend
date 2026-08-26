package com.carretero.controller;

import com.carretero.dto.RoleDTO;
import com.carretero.model.Role;
import com.carretero.service.IRoleService;
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
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final IRoleService service;
    @Qualifier("roleMapper")
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<List<RoleDTO>> findAll() throws Exception {
        List<RoleDTO> list = service.findAll().stream()
                .map(e -> modelMapper.map(e, RoleDTO.class))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleDTO> findById(@PathVariable("id") Integer id) throws Exception {
        Role obj = service.findById(id);
        return ResponseEntity.ok(modelMapper.map(obj, RoleDTO.class));
    }

    @PostMapping
    public ResponseEntity<Void> save(@Valid @RequestBody RoleDTO dto) throws Exception {
        Role obj = service.save(modelMapper.map(dto, Role.class));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(obj.getIdRole()).toUri();
        return ResponseEntity.created(location).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleDTO> update(@Valid @RequestBody RoleDTO dto, @PathVariable("id") Integer id) throws Exception {
        Role obj = service.update(modelMapper.map(dto, Role.class), id);
        return ResponseEntity.ok(modelMapper.map(obj, RoleDTO.class));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) throws Exception {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pageable")
    public ResponseEntity<Page<Role>> listPageable(Pageable pageable) {
        Page<Role> page = service.listPage(pageable);
        return ResponseEntity.ok(page);
    }
}
