package com.carretero.controller;

import com.carretero.dto.PaymentCreateRequestDTO;
import com.carretero.dto.PaymentDTO;
import com.carretero.dto.PaymentMethodDTO;
import com.carretero.dto.UserDTO;
import com.carretero.model.Payment;
import com.carretero.model.User;
import com.carretero.repository.IUserRepository;
import com.carretero.service.IPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final IPaymentService service;
    private final IUserRepository userRepo;

    @Qualifier("paymentMapper")
    private final ModelMapper paymentMapper;
    @Qualifier("paymentMethodMapper")
    private final ModelMapper methodMapper;
    @Qualifier("userMapper")
    private final ModelMapper userMapper;

    @GetMapping
    public ResponseEntity<List<PaymentDTO>> findAll() throws Exception {
        List<PaymentDTO> list = service.findAll().stream()
                .map(this::mapToDTO)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/order/{idOrder}")
    public ResponseEntity<List<PaymentDTO>> findByOrder(@PathVariable("idOrder") Integer idOrder) {
        List<PaymentDTO> list = service.findByOrderId(idOrder).stream()
                .map(this::mapToDTO)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/shift/{idCashShift}")
    public ResponseEntity<List<PaymentDTO>> findByShift(@PathVariable("idCashShift") Integer idCashShift) {
        List<PaymentDTO> list = service.findByCashShiftId(idCashShift).stream()
                .map(this::mapToDTO)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDTO> findById(@PathVariable("id") Integer id) throws Exception {
        Payment obj = service.findById(id);
        return ResponseEntity.ok(mapToDTO(obj));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MESERO') or hasAuthority('CAJERO')")
    public ResponseEntity<List<PaymentDTO>> registerPayments(@Valid @RequestBody PaymentCreateRequestDTO request) throws Exception {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepo.findOneByUsername(username);

        List<Payment> created = service.registerPayments(request, currentUser);
        List<PaymentDTO> dtoList = created.stream().map(this::mapToDTO).toList();
        return ResponseEntity.ok(dtoList);
    }

    private PaymentDTO mapToDTO(Payment payment) {
        if (payment == null) return null;
        PaymentDTO dto = new PaymentDTO();
        dto.setIdPayment(payment.getIdPayment());
        dto.setIdOrder(payment.getOrder() != null ? payment.getOrder().getIdOrder() : null);
        dto.setIdCashShift(payment.getCashShift() != null ? payment.getCashShift().getIdCashShift() : null);
        dto.setAmount(payment.getAmount());
        dto.setAmountTendered(payment.getAmountTendered());
        dto.setChangeGiven(payment.getChangeGiven());
        dto.setReferenceNumber(payment.getReferenceNumber());
        dto.setPaymentDate(payment.getPaymentDate());

        if (payment.getPaymentMethod() != null) {
            dto.setPaymentMethod(methodMapper.map(payment.getPaymentMethod(), PaymentMethodDTO.class));
        }
        if (payment.getUser() != null) {
            dto.setUser(userMapper.map(payment.getUser(), UserDTO.class));
        }
        return dto;
    }
}
