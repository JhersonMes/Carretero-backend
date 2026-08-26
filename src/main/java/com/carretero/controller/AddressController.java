package com.carretero.controller;

import com.carretero.dto.AddressDTO;
import com.carretero.model.Address;
import com.carretero.service.IAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final IAddressService service;
    @Qualifier("addressMapper")
    private final ModelMapper modelMapper;

    @GetMapping("/client/{idClient}")
    public ResponseEntity<List<AddressDTO>> findByClient(@PathVariable("idClient") Integer idClient) {
        List<AddressDTO> list = service.findByClientId(idClient).stream()
                .map(e -> modelMapper.map(e, AddressDTO.class))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddressDTO> findById(@PathVariable("id") Integer id) throws Exception {
        Address obj = service.findById(id);
        return ResponseEntity.ok(modelMapper.map(obj, AddressDTO.class));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressDTO> update(@Valid @RequestBody AddressDTO dto, @PathVariable("id") Integer id) throws Exception {
        Address obj = service.update(modelMapper.map(dto, Address.class), id);
        return ResponseEntity.ok(modelMapper.map(obj, AddressDTO.class));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) throws Exception {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
