package com.carretero.controller;

import com.carretero.dto.BusinessConfigDTO;
import com.carretero.model.BusinessConfig;
import com.carretero.service.IBusinessConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/business-config")
@RequiredArgsConstructor
public class BusinessConfigController {

    private final IBusinessConfigService service;
    @Qualifier("businessConfigMapper")
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<BusinessConfigDTO> getConfig() {
        BusinessConfig config = service.getConfig();
        return ResponseEntity.ok(modelMapper.map(config, BusinessConfigDTO.class));
    }

    /**
     * Comprueba el PIN sin ejecutar nada.
     *
     * Existe solo para la pantalla: permite pedir el PIN antes de abrir el
     * formulario, en vez de hacer que el cajero llene todo para recien enterarse
     * de que no tiene autorizacion. No sustituye la validacion real: anular y
     * reemitir vuelven a comprobar el PIN por su cuenta.
     */
    @PostMapping("/verify-pin")
    public ResponseEntity<Map<String, Boolean>> verifyPin(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(Map.of("valid", service.matchesAdminPin(body.get("pin"))));
    }

    /**
     * Cambia el PIN que autoriza anular ventas. Solo el administrador.
     * El PIN nunca se devuelve: se escribe, no se consulta.
     */
    @PutMapping("/admin-pin")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> changeAdminPin(@RequestBody Map<String, String> body) throws Exception {
        service.changeAdminPin(body.get("pin"));
        return ResponseEntity.noContent().build();
    }

    @PutMapping
    public ResponseEntity<BusinessConfigDTO> updateConfig(@Valid @RequestBody BusinessConfigDTO dto) throws Exception {
        BusinessConfig config = modelMapper.map(dto, BusinessConfig.class);
        BusinessConfig updated = service.updateConfig(config);
        return ResponseEntity.ok(modelMapper.map(updated, BusinessConfigDTO.class));
    }
}
