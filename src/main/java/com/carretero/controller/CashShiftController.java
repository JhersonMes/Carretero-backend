package com.carretero.controller;

import com.carretero.dto.CashMovementDTO;
import com.carretero.dto.CashShiftDTO;
import com.carretero.dto.UserDTO;
import com.carretero.model.CashMovement;
import com.carretero.model.CashShift;
import com.carretero.model.User;
import com.carretero.repository.IUserRepository;
import com.carretero.service.ICashShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cash-shifts")
@RequiredArgsConstructor
public class CashShiftController {

    private final ICashShiftService service;
    private final IUserRepository userRepo;

    @Qualifier("cashShiftMapper")
    private final ModelMapper shiftMapper;
    @Qualifier("userMapper")
    private final ModelMapper userMapper;

    @GetMapping("/active")
    public ResponseEntity<CashShiftDTO> getActiveShift() {
        return service.getActiveShift()
                .map(shift -> ResponseEntity.ok(mapToDTO(shift)))
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping
    public ResponseEntity<List<CashShiftDTO>> findAll() throws Exception {
        List<CashShiftDTO> list = service.findAll().stream()
                .map(this::mapToDTO)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CashShiftDTO> findById(@PathVariable("id") Integer id) throws Exception {
        CashShift obj = service.findById(id);
        return ResponseEntity.ok(mapToDTO(obj));
    }

    @PostMapping("/open")
    public ResponseEntity<CashShiftDTO> openShift(@RequestBody Map<String, Object> body) throws Exception {
        BigDecimal initialAmount = new BigDecimal(body.getOrDefault("initialAmount", "0.0").toString());
        String notes = (String) body.get("notes");

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepo.findOneByUsername(username);

        CashShift opened = service.openShift(initialAmount, currentUser, notes);
        return ResponseEntity.ok(mapToDTO(opened));
    }

    @PostMapping("/close")
    public ResponseEntity<CashShiftDTO> closeShift(@RequestBody Map<String, Object> body) throws Exception {
        Integer idCashShift = Integer.parseInt(body.get("idCashShift").toString());
        BigDecimal actualCash = new BigDecimal(body.getOrDefault("actualCash", "0.0").toString());
        String notes = (String) body.get("notes");

        CashShift closed = service.closeShift(idCashShift, actualCash, notes);
        return ResponseEntity.ok(mapToDTO(closed));
    }

    @PostMapping("/movements")
    public ResponseEntity<CashMovementDTO> registerMovement(@Valid @RequestBody CashMovementDTO dto) throws Exception {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepo.findOneByUsername(username);

        CashMovement movement = service.registerMovement(dto, currentUser);
        CashMovementDTO resp = new CashMovementDTO(
                movement.getIdMovement(),
                movement.getCashShift().getIdCashShift(),
                userMapper.map(movement.getUser(), UserDTO.class),
                movement.getType(),
                movement.getAmount(),
                movement.getReason(),
                movement.getCreatedAt()
        );
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{id}/movements")
    public ResponseEntity<List<CashMovementDTO>> getMovements(@PathVariable("id") Integer id) {
        List<CashMovementDTO> list = service.getMovementsByShift(id).stream()
                .map(m -> new CashMovementDTO(
                        m.getIdMovement(),
                        m.getCashShift().getIdCashShift(),
                        userMapper.map(m.getUser(), UserDTO.class),
                        m.getType(),
                        m.getAmount(),
                        m.getReason(),
                        m.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{id}/recalculate")
    public ResponseEntity<CashShiftDTO> recalculate(@PathVariable("id") Integer id) throws Exception {
        CashShift shift = service.recalculateShiftTotals(id);
        return ResponseEntity.ok(mapToDTO(shift));
    }

    private CashShiftDTO mapToDTO(CashShift shift) {
        if (shift == null) return null;
        CashShiftDTO dto = shiftMapper.map(shift, CashShiftDTO.class);
        if (shift.getUser() != null) {
            dto.setUser(userMapper.map(shift.getUser(), UserDTO.class));
        }
        return dto;
    }
}
