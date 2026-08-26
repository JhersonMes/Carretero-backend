package com.carretero.controller;

import com.carretero.dto.ClientDTO;
import com.carretero.dto.InvoiceDTO;
import com.carretero.dto.InvoiceEmitRequestDTO;
import com.carretero.model.Invoice;
import com.carretero.model.User;
import com.carretero.model.enums.InvoiceType;
import com.carretero.repository.IUserRepository;
import com.carretero.service.IInvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final IInvoiceService service;
    private final IUserRepository userRepo;

    @Qualifier("invoiceMapper")
    private final ModelMapper invoiceMapper;
    @Qualifier("clientMapper")
    private final ModelMapper clientMapper;

    @GetMapping
    public ResponseEntity<List<InvoiceDTO>> findAll() throws Exception {
        List<InvoiceDTO> list = service.findAll().stream()
                .map(this::mapToDTO)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDTO> findById(@PathVariable("id") Integer id) throws Exception {
        Invoice obj = service.findById(id);
        return ResponseEntity.ok(mapToDTO(obj));
    }

    @GetMapping("/order/{idOrder}")
    public ResponseEntity<InvoiceDTO> findByOrder(@PathVariable("idOrder") Integer idOrder) {
        return service.findByOrderId(idOrder)
                .map(inv -> ResponseEntity.ok(mapToDTO(inv)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{invoiceType}")
    public ResponseEntity<List<InvoiceDTO>> findByType(@PathVariable("invoiceType") InvoiceType invoiceType) {
        List<InvoiceDTO> list = service.findByInvoiceType(invoiceType).stream()
                .map(this::mapToDTO)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/next-number/{invoiceType}")
    public ResponseEntity<Map<String, String>> getNextNumber(@PathVariable("invoiceType") InvoiceType invoiceType) {
        String nextNumber = service.getNextCorrelativeNumber(invoiceType);
        return ResponseEntity.ok(Map.of("nextNumber", nextNumber));
    }

    @PostMapping("/emit")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MESERO') or hasAuthority('CAJERO')")
    public ResponseEntity<InvoiceDTO> emitInvoice(@Valid @RequestBody InvoiceEmitRequestDTO request) throws Exception {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepo.findOneByUsername(username);

        Invoice emitted = service.emitInvoice(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDTO(emitted));
    }

    private InvoiceDTO mapToDTO(Invoice invoice) {
        if (invoice == null) return null;
        InvoiceDTO dto = invoiceMapper.map(invoice, InvoiceDTO.class);
        if (invoice.getOrder() != null) {
            dto.setIdOrder(invoice.getOrder().getIdOrder());
            dto.setOrderCode(invoice.getOrder().getOrderCode());
        }
        if (invoice.getClient() != null) {
            dto.setClient(clientMapper.map(invoice.getClient(), ClientDTO.class));
        }
        return dto;
    }
}
