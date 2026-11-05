package com.carretero;

import com.carretero.dto.PaymentCreateRequestDTO;
import com.carretero.model.*;
import com.carretero.model.enums.CashShiftStatus;
import com.carretero.model.enums.OrderStatus;
import com.carretero.repository.IOrderRepository;
import com.carretero.repository.IPaymentMethodRepository;
import com.carretero.repository.IPaymentRepository;
import com.carretero.service.ICashShiftService;
import com.carretero.service.IOrderService;
import com.carretero.service.OrderBoardNotifier;
import com.carretero.service.implementation.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para PaymentService.
 * Guiadas de Semana07_CalProSoft y documentadas en ANEXOS:
 * CP-COB-01, CP-COB-03, CP-COB-05, CP-COB-06, CP-COB-10, CP-COB-11.
 */
@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private IPaymentRepository repo;

    @Mock
    private IOrderRepository orderRepo;

    @Mock
    private IPaymentMethodRepository paymentMethodRepo;

    @Mock
    private ICashShiftService cashShiftService;

    @Mock
    private IOrderService orderService;

    private PaymentService paymentService;
    private boolean boardNotified;

    private User cajero;
    private CashShift turnoActivo;
    private PaymentMethod metodoEfectivo;
    private PaymentMethod metodoPlin;

    @BeforeEach
    void setUp() {
        cajero = new User();
        cajero.setIdUser(1);
        cajero.setUsername("caja1");

        turnoActivo = new CashShift();
        turnoActivo.setIdCashShift(1);
        turnoActivo.setStatus(CashShiftStatus.ABIERTA);

        metodoEfectivo = new PaymentMethod();
        metodoEfectivo.setIdPaymentMethod(1);
        metodoEfectivo.setName("EFECTIVO");

        metodoPlin = new PaymentMethod();
        metodoPlin.setIdPaymentMethod(4);
        metodoPlin.setName("PLIN");

        boardNotified = false;
        OrderBoardNotifier boardNotifier = new OrderBoardNotifier(null) {
            @Override
            public void notifyBoards() {
                boardNotified = true;
            }
        };

        paymentService = new PaymentService(repo, orderRepo, paymentMethodRepo, cashShiftService, orderService, boardNotifier);
    }

    // CASO DE PRUEBA CP-COB-01: Cobro que supera el saldo pendiente se rechaza y no guarda ningún pago
    @Test
    void registerPayments_montoMayorAlSaldo_lanzaExcepcionSinGuardar() {
        // preparacion
        Order p1 = new Order();
        p1.setIdOrder(1);
        p1.setTotal(new BigDecimal("74.00"));
        p1.setStatus(OrderStatus.PENDIENTE);

        when(orderRepo.findById(1)).thenReturn(Optional.of(p1));
        when(cashShiftService.getActiveShift()).thenReturn(Optional.of(turnoActivo));
        when(repo.findByOrderIdOrder(1)).thenReturn(Collections.emptyList());

        PaymentCreateRequestDTO request = new PaymentCreateRequestDTO();
        request.setIdOrder(1);
        PaymentCreateRequestDTO.PaymentItemDTO item = new PaymentCreateRequestDTO.PaymentItemDTO();
        item.setIdPaymentMethod(1);
        item.setAmount(new BigDecimal("80.00")); // 80.00 > saldo 74.00
        request.setPayments(List.of(item));

        // ejecucion y afirmacion
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> paymentService.registerPayments(request, cajero)
        );

        assertTrue(ex.getMessage().contains("supera el saldo pendiente"));
        verify(repo, never()).save(any(Payment.class));
        assertFalse(boardNotified);
    }

    // CASO DE PRUEBA CP-COB-03: Valor límite: pago exactamente igual al saldo (74.00) cierra el pedido
    @Test
    void registerPayments_montoIgualAlSaldo_marcaPagado() throws Exception {
        // preparacion
        Order p1 = new Order();
        p1.setIdOrder(1);
        p1.setTotal(new BigDecimal("74.00"));
        p1.setStatus(OrderStatus.PENDIENTE);

        Payment pagoGuardado = new Payment();
        pagoGuardado.setAmount(new BigDecimal("74.00"));

        when(orderRepo.findById(1)).thenReturn(Optional.of(p1));
        when(cashShiftService.getActiveShift()).thenReturn(Optional.of(turnoActivo));
        when(repo.findByOrderIdOrder(1))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(pagoGuardado));
        when(paymentMethodRepo.findById(1)).thenReturn(Optional.of(metodoEfectivo));
        when(repo.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentCreateRequestDTO request = new PaymentCreateRequestDTO();
        request.setIdOrder(1);
        PaymentCreateRequestDTO.PaymentItemDTO item = new PaymentCreateRequestDTO.PaymentItemDTO();
        item.setIdPaymentMethod(1);
        item.setAmount(new BigDecimal("74.00"));
        request.setPayments(List.of(item));

        // ejecucion
        List<Payment> result = paymentService.registerPayments(request, cajero);

        // afirmacion
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(new BigDecimal("74.00"), result.get(0).getAmount());
        verify(orderService).updateOrderStatus(1, OrderStatus.PAGADO);
        assertTrue(boardNotified);
    }

    // CASO DE PRUEBA CP-COB-05: Pago parcial: el pedido sigue abierto y la mesa ocupada
    @Test
    void registerPayments_pagoParcial_noMarcaPagado() throws Exception {
        // preparacion
        Order p1 = new Order();
        p1.setIdOrder(1);
        p1.setTotal(new BigDecimal("74.00"));
        p1.setStatus(OrderStatus.PENDIENTE);

        Payment pagoParcial = new Payment();
        pagoParcial.setAmount(new BigDecimal("30.00"));

        when(orderRepo.findById(1)).thenReturn(Optional.of(p1));
        when(cashShiftService.getActiveShift()).thenReturn(Optional.of(turnoActivo));
        when(repo.findByOrderIdOrder(1))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(pagoParcial));
        when(paymentMethodRepo.findById(1)).thenReturn(Optional.of(metodoEfectivo));
        when(repo.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentCreateRequestDTO request = new PaymentCreateRequestDTO();
        request.setIdOrder(1);
        PaymentCreateRequestDTO.PaymentItemDTO item = new PaymentCreateRequestDTO.PaymentItemDTO();
        item.setIdPaymentMethod(1);
        item.setAmount(new BigDecimal("30.00"));
        request.setPayments(List.of(item));

        // ejecucion
        List<Payment> result = paymentService.registerPayments(request, cajero);

        // afirmacion
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(orderService, never()).updateOrderStatus(eq(1), eq(OrderStatus.PAGADO));
        assertTrue(boardNotified);
    }

    // CASO DE PRUEBA CP-COB-06: Segundo pago que completa el saldo cierra el pedido
    @Test
    void registerPayments_segundoPagoCompletaSaldo_marcaPagado() throws Exception {
        // preparacion
        Order p1 = new Order();
        p1.setIdOrder(1);
        p1.setTotal(new BigDecimal("74.00"));
        p1.setStatus(OrderStatus.PENDIENTE);

        Payment pAnterior = new Payment();
        pAnterior.setAmount(new BigDecimal("30.00"));

        Payment pNuevo = new Payment();
        pNuevo.setAmount(new BigDecimal("44.00"));

        when(orderRepo.findById(1)).thenReturn(Optional.of(p1));
        when(cashShiftService.getActiveShift()).thenReturn(Optional.of(turnoActivo));
        when(repo.findByOrderIdOrder(1))
                .thenReturn(List.of(pAnterior))
                .thenReturn(List.of(pAnterior, pNuevo));
        when(paymentMethodRepo.findById(4)).thenReturn(Optional.of(metodoPlin));
        when(repo.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentCreateRequestDTO request = new PaymentCreateRequestDTO();
        request.setIdOrder(1);
        PaymentCreateRequestDTO.PaymentItemDTO item = new PaymentCreateRequestDTO.PaymentItemDTO();
        item.setIdPaymentMethod(4);
        item.setAmount(new BigDecimal("44.00"));
        request.setPayments(List.of(item));

        // ejecucion
        List<Payment> result = paymentService.registerPayments(request, cajero);

        // afirmacion
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(new BigDecimal("44.00"), result.get(0).getAmount());
        verify(orderService).updateOrderStatus(1, OrderStatus.PAGADO);
    }

    // CASO DE PRUEBA CP-COB-10: Cobro sin turno de caja abierto se rechaza
    @Test
    void registerPayments_sinTurnoAbierto_lanzaExcepcion() {
        // preparacion
        Order p1 = new Order();
        p1.setIdOrder(1);
        p1.setTotal(new BigDecimal("74.00"));
        p1.setStatus(OrderStatus.PENDIENTE);

        when(orderRepo.findById(1)).thenReturn(Optional.of(p1));
        when(cashShiftService.getActiveShift()).thenReturn(Optional.empty()); // Caja cerrada

        PaymentCreateRequestDTO request = new PaymentCreateRequestDTO();
        request.setIdOrder(1);
        PaymentCreateRequestDTO.PaymentItemDTO item = new PaymentCreateRequestDTO.PaymentItemDTO();
        item.setIdPaymentMethod(1);
        item.setAmount(new BigDecimal("74.00"));
        request.setPayments(List.of(item));

        // ejecucion y afirmacion
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> paymentService.registerPayments(request, cajero)
        );

        assertTrue(ex.getMessage().contains("No hay un turno de caja abierto"));
        verify(repo, never()).save(any(Payment.class));
    }

    // CASO DE PRUEBA CP-COB-11: No se puede cobrar un pedido CANCELADO
    @Test
    void registerPayments_pedidoCancelado_lanzaExcepcion() {
        // preparacion
        Order pCancelado = new Order();
        pCancelado.setIdOrder(5);
        pCancelado.setOrderCode("PED-20261001-0005");
        pCancelado.setTotal(new BigDecimal("30.00"));
        pCancelado.setStatus(OrderStatus.CANCELADO);

        when(orderRepo.findById(5)).thenReturn(Optional.of(pCancelado));

        PaymentCreateRequestDTO request = new PaymentCreateRequestDTO();
        request.setIdOrder(5);
        PaymentCreateRequestDTO.PaymentItemDTO item = new PaymentCreateRequestDTO.PaymentItemDTO();
        item.setIdPaymentMethod(1);
        item.setAmount(new BigDecimal("30.00"));
        request.setPayments(List.of(item));

        // ejecucion y afirmacion
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> paymentService.registerPayments(request, cajero)
        );

        assertTrue(ex.getMessage().contains("cancelado"));
        verify(repo, never()).save(any(Payment.class));
    }
}
