package com.carretero.controller;

import com.carretero.dto.AddressDTO;
import com.carretero.dto.ClientDTO;
import com.carretero.model.Address;
import com.carretero.model.Client;
import com.carretero.service.IAddressService;
import com.carretero.service.IClientService;
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
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private final IClientService service;
    private final IAddressService addressService;
    @Qualifier("clientMapper")
    private final ModelMapper modelMapper;
    @Qualifier("addressMapper")
    private final ModelMapper addressMapper;

    @GetMapping
    public ResponseEntity<List<ClientDTO>> findAll() throws Exception {
        List<ClientDTO> list = service.findAll().stream()
                .map(this::mapToDTO)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ClientDTO>> search(@RequestParam("q") String query) {
        List<ClientDTO> list = service.search(query).stream()
                .map(this::mapToDTO)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientDTO> findById(@PathVariable("id") Integer id) throws Exception {
        Client obj = service.findById(id);
        return ResponseEntity.ok(mapToDTO(obj));
    }

    @GetMapping("/doc/{docNumber}")
    public ResponseEntity<ClientDTO> findByDoc(@PathVariable("docNumber") String docNumber) {
        return service.findByDocNumber(docNumber)
                .map(client -> ResponseEntity.ok(mapToDTO(client)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/phone/{phone}")
    public ResponseEntity<ClientDTO> findByPhone(@PathVariable("phone") String phone) {
        return service.findByPhone(phone)
                .map(client -> ResponseEntity.ok(mapToDTO(client)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ClientDTO> save(@Valid @RequestBody ClientDTO dto) throws Exception {
        Client client = modelMapper.map(dto, Client.class);
        Client saved = service.save(client);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(saved.getIdClient()).toUri();
        return ResponseEntity.created(location).body(mapToDTO(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientDTO> update(@Valid @RequestBody ClientDTO dto, @PathVariable("id") Integer id) throws Exception {
        Client client = modelMapper.map(dto, Client.class);
        Client updated = service.update(client, id);
        return ResponseEntity.ok(mapToDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) throws Exception {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/addresses")
    public ResponseEntity<List<AddressDTO>> getAddresses(@PathVariable("id") Integer id) {
        List<AddressDTO> list = addressService.findByClientId(id).stream()
                .map(a -> addressMapper.map(a, AddressDTO.class))
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{id}/addresses")
    public ResponseEntity<AddressDTO> addAddress(@PathVariable("id") Integer id, @Valid @RequestBody AddressDTO dto) throws Exception {
        Client client = service.findById(id);
        Address address = addressMapper.map(dto, Address.class);
        address.setClient(client);
        Address saved = addressService.save(address);
        return ResponseEntity.ok(addressMapper.map(saved, AddressDTO.class));
    }

    @GetMapping("/pageable")
    public ResponseEntity<Page<Client>> listPageable(Pageable pageable) {
        return ResponseEntity.ok(service.listPage(pageable));
    }

    private ClientDTO mapToDTO(Client client) {
        if (client == null) return null;
        ClientDTO dto = modelMapper.map(client, ClientDTO.class);
        if (client.getAddresses() != null) {
            dto.setAddresses(client.getAddresses().stream()
                    .map(a -> addressMapper.map(a, AddressDTO.class))
                    .toList());
        }
        return dto;
    }
}
