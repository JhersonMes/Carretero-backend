package com.carretero.controller;

import com.carretero.dto.IngredientDTO;
import com.carretero.model.Ingredient;
import com.carretero.service.IIngredientService;
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
@RequestMapping("/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IIngredientService service;
    @Qualifier("ingredientMapper")
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<List<IngredientDTO>> findAll() throws Exception {
        List<IngredientDTO> list = service.findAll().stream()
                .map(e -> modelMapper.map(e, IngredientDTO.class))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IngredientDTO> findById(@PathVariable("id") Integer id) throws Exception {
        Ingredient obj = service.findById(id);
        return ResponseEntity.ok(modelMapper.map(obj, IngredientDTO.class));
    }

    @PostMapping
    public ResponseEntity<Void> save(@Valid @RequestBody IngredientDTO dto) throws Exception {
        Ingredient obj = service.save(modelMapper.map(dto, Ingredient.class));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(obj.getIdIngredient()).toUri();
        return ResponseEntity.created(location).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<IngredientDTO> update(@Valid @RequestBody IngredientDTO dto, @PathVariable("id") Integer id) throws Exception {
        Ingredient obj = service.update(modelMapper.map(dto, Ingredient.class), id);
        return ResponseEntity.ok(modelMapper.map(obj, IngredientDTO.class));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) throws Exception {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pageable")
    public ResponseEntity<Page<Ingredient>> listPageable(Pageable pageable) {
        Page<Ingredient> page = service.listPage(pageable);
        return ResponseEntity.ok(page);
    }
}
