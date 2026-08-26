package com.carretero.controller;

import com.carretero.dto.DiningTableDTO;
import com.carretero.model.DiningTable;
import com.carretero.model.enums.TableStatus;
import com.carretero.service.IDiningTableService;
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
@RequestMapping("/tables")
@RequiredArgsConstructor
public class DiningTableController {

    private final IDiningTableService service;
    @Qualifier("diningTableMapper")
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<List<DiningTableDTO>> findAll() {
        List<DiningTableDTO> list = service.findActive().stream()
                .map(e -> modelMapper.map(e, DiningTableDTO.class))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<DiningTableDTO>> findByStatus(@PathVariable("status") TableStatus status) {
        List<DiningTableDTO> list = service.findByStatus(status).stream()
                .map(e -> modelMapper.map(e, DiningTableDTO.class))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiningTableDTO> findById(@PathVariable("id") Integer id) throws Exception {
        DiningTable obj = service.findById(id);
        return ResponseEntity.ok(modelMapper.map(obj, DiningTableDTO.class));
    }

    @PostMapping
    public ResponseEntity<DiningTableDTO> save(@Valid @RequestBody DiningTableDTO dto) throws Exception {
        DiningTable obj = service.save(modelMapper.map(dto, DiningTable.class));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(obj.getIdTable()).toUri();
        return ResponseEntity.created(location).body(modelMapper.map(obj, DiningTableDTO.class));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DiningTableDTO> update(@Valid @RequestBody DiningTableDTO dto, @PathVariable("id") Integer id) throws Exception {
        DiningTable obj = service.update(modelMapper.map(dto, DiningTable.class), id);
        return ResponseEntity.ok(modelMapper.map(obj, DiningTableDTO.class));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DiningTableDTO> updateStatus(@PathVariable("id") Integer id, @RequestBody Map<String, TableStatus> body) throws Exception {
        TableStatus status = body.get("status");
        if (status == null) {
            return ResponseEntity.badRequest().build();
        }
        DiningTable obj = service.updateStatus(id, status);
        return ResponseEntity.ok(modelMapper.map(obj, DiningTableDTO.class));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) throws Exception {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
