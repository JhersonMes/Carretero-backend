package com.carretero.controller;

import com.carretero.dto.MenuDTO;
import com.carretero.model.Menu;
import com.carretero.service.IMenuService;
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
@RequestMapping("/menus")
@RequiredArgsConstructor
public class MenuController {

    private final IMenuService service;
    @Qualifier("menuMapper")
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<List<MenuDTO>> findAll() throws Exception {
        List<MenuDTO> list = service.findAll().stream()
                .map(e -> modelMapper.map(e, MenuDTO.class))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuDTO> findById(@PathVariable("id") Integer id) throws Exception {
        Menu obj = service.findById(id);
        return ResponseEntity.ok(modelMapper.map(obj, MenuDTO.class));
    }

    @PostMapping
    public ResponseEntity<Void> save(@Valid @RequestBody MenuDTO dto) throws Exception {
        Menu obj = service.save(modelMapper.map(dto, Menu.class));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(obj.getIdMenu()).toUri();
        return ResponseEntity.created(location).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<MenuDTO> update(@Valid @RequestBody MenuDTO dto, @PathVariable("id") Integer id) throws Exception {
        Menu obj = service.update(modelMapper.map(dto, Menu.class), id);
        return ResponseEntity.ok(modelMapper.map(obj, MenuDTO.class));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) throws Exception {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pageable")
    public ResponseEntity<Page<Menu>> listPageable(Pageable pageable) {
        Page<Menu> page = service.listPage(pageable);
        return ResponseEntity.ok(page);
    }
}
