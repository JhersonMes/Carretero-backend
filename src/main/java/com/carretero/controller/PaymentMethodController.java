package com.carretero.controller;

import com.carretero.dto.PaymentMethodDTO;
import com.carretero.model.PaymentMethod;
import com.carretero.service.IPaymentMethodService;
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
@RequestMapping("/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final IPaymentMethodService service;
    @Qualifier("paymentMethodMapper")
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<List<PaymentMethodDTO>> findAll() throws Exception {
        List<PaymentMethodDTO> list = service.findAll().stream()
                .map(e -> modelMapper.map(e, PaymentMethodDTO.class))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentMethodDTO> findById(@PathVariable("id") Integer id) throws Exception {
        PaymentMethod obj = service.findById(id);
        return ResponseEntity.ok(modelMapper.map(obj, PaymentMethodDTO.class));
    }

    @PostMapping
    public ResponseEntity<Void> save(@Valid @RequestBody PaymentMethodDTO dto) throws Exception {
        PaymentMethod obj = service.save(modelMapper.map(dto, PaymentMethod.class));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(obj.getIdPaymentMethod()).toUri();
        return ResponseEntity.created(location).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentMethodDTO> update(@Valid @RequestBody PaymentMethodDTO dto, @PathVariable("id") Integer id) throws Exception {
        PaymentMethod obj = service.update(modelMapper.map(dto, PaymentMethod.class), id);
        return ResponseEntity.ok(modelMapper.map(obj, PaymentMethodDTO.class));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) throws Exception {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pageable")
    public ResponseEntity<Page<PaymentMethod>> listPageable(Pageable pageable) {
        Page<PaymentMethod> page = service.listPage(pageable);
        return ResponseEntity.ok(page);
    }
}
