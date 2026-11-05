package com.carretero.controller;

import com.carretero.dto.DiningTableDTO;
import com.carretero.dto.TableBoardDTO;
import com.carretero.model.DiningTable;
import com.carretero.model.enums.TableStatus;
import com.carretero.service.IDiningTableService;
import com.carretero.service.ISalonBoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final ISalonBoardService salonBoardService;
    @Qualifier("diningTableMapper")
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<List<DiningTableDTO>> findAll() {
        List<DiningTableDTO> list = service.findActive().stream()
                .map(e -> modelMapper.map(e, DiningTableDTO.class))
                .toList();
        return ResponseEntity.ok(list);
    }

    /** Tablero del salon: mesas en el orden del plano con su pedido activo resumido. */
    @GetMapping("/board")
    public ResponseEntity<List<TableBoardDTO>> getBoard() {
        return ResponseEntity.ok(salonBoardService.getBoard());
    }

    /**
     * Guarda el nuevo orden del plano del salon. Solo el administrador puede
     * reacomodar las mesas: el mesero ve el plano pero no lo edita.
     */
    @PutMapping("/reorder")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<TableBoardDTO>> reorder(@RequestBody Map<String, List<Integer>> body) throws Exception {
        List<Integer> orderedIds = body.get("orderedTableIds");
        if (orderedIds == null || orderedIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        salonBoardService.reorderTables(orderedIds);
        return ResponseEntity.ok(salonBoardService.getBoard());
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

    /** Listado de administracion: incluye las mesas dadas de baja. */
    @GetMapping("/manage")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<DiningTableDTO>> findAllForManagement() {
        List<DiningTableDTO> list = service.findAllForManagement().stream()
                .map(e -> modelMapper.map(e, DiningTableDTO.class))
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<DiningTableDTO> save(@Valid @RequestBody DiningTableDTO dto) throws Exception {
        DiningTable obj = service.createTable(dto.getName(), dto.getCapacity());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(obj.getIdTable()).toUri();
        return ResponseEntity.created(location).body(modelMapper.map(obj, DiningTableDTO.class));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<DiningTableDTO> update(@Valid @RequestBody DiningTableDTO dto, @PathVariable("id") Integer id) throws Exception {
        DiningTable obj = service.renameTable(id, dto.getName(), dto.getCapacity());
        return ResponseEntity.ok(modelMapper.map(obj, DiningTableDTO.class));
    }

    /** Vuelve a poner en servicio una mesa dada de baja. */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<DiningTableDTO> activate(@PathVariable("id") Integer id) throws Exception {
        DiningTable obj = service.activate(id);
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

    /**
     * Da de baja la mesa en lugar de borrarla: sigue referenciada por los pedidos y
     * comprobantes ya emitidos, y eliminarla dejaria ese historial huerfano.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<DiningTableDTO> delete(@PathVariable("id") Integer id) throws Exception {
        DiningTable obj = service.deactivate(id);
        return ResponseEntity.ok(modelMapper.map(obj, DiningTableDTO.class));
    }
}
