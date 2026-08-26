package com.carretero.controller;

import com.carretero.dto.DniRucQueryResponseDTO;
import com.carretero.service.ISunatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sunat")
@RequiredArgsConstructor
public class SunatController {

    private final ISunatService sunatService;

    @GetMapping("/query/{docNumber}")
    public ResponseEntity<DniRucQueryResponseDTO> queryDocument(@PathVariable("docNumber") String docNumber) {
        DniRucQueryResponseDTO result = sunatService.queryDocument(docNumber);
        return ResponseEntity.ok(result);
    }
}
