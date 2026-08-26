package com.carretero.controller;

import com.carretero.dto.CategoryDTO;
import com.carretero.model.Category;
import com.carretero.service.ICategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final ICategoryService service;
    @Qualifier("categoryMapper")
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> findAll() {
        List<CategoryDTO> list = service.findActiveOrdered().stream()
                .map(e -> modelMapper.map(e, CategoryDTO.class))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/all")
    public ResponseEntity<List<CategoryDTO>> findAllRaw() throws Exception {
        List<CategoryDTO> list = service.findAll().stream()
                .map(e -> modelMapper.map(e, CategoryDTO.class))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDTO> findById(@PathVariable("id") Integer id) throws Exception {
        Category obj = service.findById(id);
        return ResponseEntity.ok(modelMapper.map(obj, CategoryDTO.class));
    }

    @PostMapping
    public ResponseEntity<CategoryDTO> save(@Valid @RequestBody CategoryDTO dto) throws Exception {
        Category obj = service.save(modelMapper.map(dto, Category.class));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(obj.getIdCategory()).toUri();
        return ResponseEntity.created(location).body(modelMapper.map(obj, CategoryDTO.class));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryDTO> update(@Valid @RequestBody CategoryDTO dto, @PathVariable("id") Integer id) throws Exception {
        Category obj = service.update(modelMapper.map(dto, Category.class), id);
        return ResponseEntity.ok(modelMapper.map(obj, CategoryDTO.class));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) throws Exception {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
