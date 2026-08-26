package com.carretero.controller;

import com.carretero.dto.BusinessConfigDTO;
import com.carretero.model.BusinessConfig;
import com.carretero.service.IBusinessConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PutMapping
    public ResponseEntity<BusinessConfigDTO> updateConfig(@Valid @RequestBody BusinessConfigDTO dto) throws Exception {
        BusinessConfig config = modelMapper.map(dto, BusinessConfig.class);
        BusinessConfig updated = service.updateConfig(config);
        return ResponseEntity.ok(modelMapper.map(updated, BusinessConfigDTO.class));
    }
}
