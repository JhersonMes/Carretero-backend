package com.carretero.controller;

import com.carretero.dto.CategoryDTO;
import com.carretero.model.Category;
import com.carretero.model.enums.KitchenStation;
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
import java.util.Map;

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

    /**
     * Cambia el area de la categoria. Por defecto arrastra a sus productos: la
     * comanda se enruta por el area del producto, asi que sin eso el cambio no
     * sirve de nada para lo que ya esta cargado.
     */
    @PatchMapping("/{id}/station")
    public ResponseEntity<CategoryDTO> changeStation(
            @PathVariable("id") Integer id,
            @RequestBody Map<String, Object> body) throws Exception {
        KitchenStation station = KitchenStation.valueOf(String.valueOf(body.get("station")));
        boolean moveProducts = !Boolean.FALSE.equals(body.get("moveProducts"));

        Category updated = service.changeStation(id, station, moveProducts);
        return ResponseEntity.ok(modelMapper.map(updated, CategoryDTO.class));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) throws Exception {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
