package com.carretero;

import com.carretero.dto.InvoiceEmitRequestDTO;
import com.carretero.model.*;
import com.carretero.model.enums.DocumentType;
import com.carretero.model.enums.InvoiceType;
import com.carretero.model.enums.OrderStatus;
import com.carretero.model.enums.SunatStatus;
import com.carretero.repository.*;
import com.carretero.service.ISunatService;
import com.carretero.service.implementation.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
 * Pruebas unitarias para InvoiceService.
 * Guiadas de Semana07_CalProSoft y documentadas en ANEXOS:
 * CP-CPE-01, CP-CPE-02, CP-CPE-03, CP-CPE-05, CP-CPE-06, CP-CPE-09.
 */
@ExtendWith(MockitoExtension.class)
public class InvoiceServiceTest {

    @Mock
    private IInvoiceRepository invoiceRepo;

    @Mock
    private IOrderRepository orderRepo;

    @Mock
    private IClientRepository clientRepo;

    @Mock
    private IBusinessConfigRepository configRepo;

    @Mock
    private ISunatService sunatService;

    @InjectMocks
    private InvoiceService invoiceService;

    private User mockUser;
    private BusinessConfig mockConfig;
    private Order p1Pagado74;
    private Order pPagado118;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUsername("caja1");

        mockConfig = new BusinessConfig();
        mockConfig.setBoletaSeries("B001");
        mockConfig.setFacturaSeries("F001");
        mockConfig.setNotaVentaSeries("NV01");

        p1Pagado74 = new Order();
        p1Pagado74.setIdOrder(1);
        p1Pagado74.setOrderCode("PED-20261001-0001");
        p1Pagado74.setTotal(new BigDecimal("74.00"));
        p1Pagado74.setStatus(OrderStatus.PAGADO);

        pPagado118 = new Order();
        pPagado118.setIdOrder(2);
        pPagado118.setOrderCode("PED-20261001-0002");
        pPagado118.setTotal(new BigDecimal("118.00"));
        pPagado118.setStatus(OrderStatus.PAGADO);
    }

    // CASO DE PRUEBA CP-CPE-01: Boleta del pedido P1: serie B001, correlativo siguiente y desglose de IGV exacto
    @Test
    void emitInvoice_boletaPedido74_desglosaIgvYTomaCorrelativoSiguiente() throws Exception {
        // preparacion
        when(orderRepo.findById(1)).thenReturn(Optional.of(p1Pagado74));
        when(invoiceRepo.findByOrderIdOrderAndSunatStatusNot(1, SunatStatus.ANULADO))
                .thenReturn(Collections.emptyList());
        when(configRepo.findFirstByActiveTrue()).thenReturn(Optional.of(mockConfig));
        when(invoiceRepo.findMaxCorrelativeBySeries("B001")).thenReturn(25);
        when(sunatService.dispatchToSunat(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));
        when(invoiceRepo.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        InvoiceEmitRequestDTO request = new InvoiceEmitRequestDTO();
        request.setIdOrder(1);
        request.setInvoiceType(InvoiceType.BOLETA);
        request.setDocNumber(null);

        // ejecucion
        Invoice emitted = invoiceService.emitInvoice(request, mockUser);

        // afirmacion
        assertNotNull(emitted);
        assertEquals("B001", emitted.getSeries());
        assertEquals(26, emitted.getCorrelativeNumber());
        assertEquals("B001-00000026", emitted.getFullNumber());
        assertEquals(new BigDecimal("62.71"), emitted.getTaxableAmount());
        assertEquals(new BigDecimal("11.29"), emitted.getIgvAmount());
        assertEquals(new BigDecimal("74.00"), emitted.getTotalAmount());
        assertEquals(new BigDecimal("74.00"), emitted.getTaxableAmount().add(emitted.getIgvAmount()));
        assertNotNull(emitted.getIssueDate());
    }

    // CASO DE PRUEBA CP-CPE-02: Desglose de IGV con total S/ 118.00 (Base 100.00, IGV 18.00)
    @Test
    void emitInvoice_total118_base100Igv18() throws Exception {
        // preparacion
        when(orderRepo.findById(2)).thenReturn(Optional.of(pPagado118));
        when(invoiceRepo.findByOrderIdOrderAndSunatStatusNot(2, SunatStatus.ANULADO))
                .thenReturn(Collections.emptyList());
        when(configRepo.findFirstByActiveTrue()).thenReturn(Optional.of(mockConfig));
        when(invoiceRepo.findMaxCorrelativeBySeries("B001")).thenReturn(25);
        when(sunatService.dispatchToSunat(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));
        when(invoiceRepo.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        InvoiceEmitRequestDTO request = new InvoiceEmitRequestDTO();
        request.setIdOrder(2);
        request.setInvoiceType(InvoiceType.BOLETA);

        // ejecucion
        Invoice emitted = invoiceService.emitInvoice(request, mockUser);

        // afirmacion
        assertNotNull(emitted);
        assertEquals(new BigDecimal("100.00"), emitted.getTaxableAmount());
        assertEquals(new BigDecimal("18.00"), emitted.getIgvAmount());
        assertEquals(new BigDecimal("118.00"), emitted.getTotalAmount());
        assertEquals("B001-00000026", emitted.getFullNumber());
    }

    // CASO DE PRUEBA CP-CPE-03: Factura con RUC inválido se rechaza
    @Test
    void emitInvoice_facturaConRucInvalido_lanzaIllegalArgumentException() {
        // preparacion
        when(orderRepo.findById(1)).thenReturn(Optional.of(p1Pagado74));
        when(invoiceRepo.findByOrderIdOrderAndSunatStatusNot(1, SunatStatus.ANULADO))
                .thenReturn(Collections.emptyList());
        when(configRepo.findFirstByActiveTrue()).thenReturn(Optional.of(mockConfig));

        InvoiceEmitRequestDTO request = new InvoiceEmitRequestDTO();
        request.setIdOrder(1);
        request.setInvoiceType(InvoiceType.FACTURA);
        request.setDocNumber("2060123456"); // 10 dígitos (inválido)

        // ejecucion y afirmacion
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> invoiceService.emitInvoice(request, mockUser)
        );

        assertTrue(ex.getMessage().contains("RUC válido de 11 dígitos"));
        verify(invoiceRepo, never()).save(any(Invoice.class));
    }

    // CASO DE PRUEBA CP-CPE-05: Factura con RUC válido crea al cliente y usa la serie F001
    @Test
    void emitInvoice_facturaRucValido_creaClienteRucYSerieF001() throws Exception {
        // preparacion
        when(orderRepo.findById(1)).thenReturn(Optional.of(p1Pagado74));
        when(invoiceRepo.findByOrderIdOrderAndSunatStatusNot(1, SunatStatus.ANULADO))
                .thenReturn(Collections.emptyList());
        when(configRepo.findFirstByActiveTrue()).thenReturn(Optional.of(mockConfig));
        when(invoiceRepo.findMaxCorrelativeBySeries("F001")).thenReturn(null); // Primera factura
        when(clientRepo.findAllByDocNumber("20601234567")).thenReturn(Collections.emptyList());
        when(clientRepo.save(any(Client.class))).thenAnswer(i -> i.getArgument(0));
        when(sunatService.dispatchToSunat(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));
        when(invoiceRepo.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        InvoiceEmitRequestDTO request = new InvoiceEmitRequestDTO();
        request.setIdOrder(1);
        request.setInvoiceType(InvoiceType.FACTURA);
        request.setDocNumber("20601234567");
        request.setClientName(null);

        // ejecucion
        Invoice emitted = invoiceService.emitInvoice(request, mockUser);

        // afirmacion
        assertNotNull(emitted);
        assertEquals("F001-00000001", emitted.getFullNumber());
        assertEquals(1, emitted.getCorrelativeNumber());
        assertEquals(InvoiceType.FACTURA, emitted.getInvoiceType());
        assertNotNull(emitted.getClient());
        assertEquals(DocumentType.RUC, emitted.getClient().getDocType());
        assertEquals("20601234567", emitted.getClient().getDocNumber());
        assertEquals("CLIENTE 20601234567", emitted.getClient().getName());
        verify(clientRepo).save(any(Client.class));
    }

    // CASO DE PRUEBA CP-CPE-06: Un pedido no puede tener dos comprobantes vigentes
    @Test
    void emitInvoice_pedidoConBoletaVigente_lanzaExcepcionSinConsumirCorrelativo() {
        // preparacion
        Invoice boletaPrevia = new Invoice();
        boletaPrevia.setFullNumber("B001-00000026");
        boletaPrevia.setSunatStatus(SunatStatus.PENDIENTE);

        when(orderRepo.findById(1)).thenReturn(Optional.of(p1Pagado74));
        when(invoiceRepo.findByOrderIdOrderAndSunatStatusNot(1, SunatStatus.ANULADO))
                .thenReturn(List.of(boletaPrevia));

        InvoiceEmitRequestDTO request = new InvoiceEmitRequestDTO();
        request.setIdOrder(1);
        request.setInvoiceType(InvoiceType.BOLETA);

        // ejecucion y afirmacion
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> invoiceService.emitInvoice(request, mockUser)
        );

        assertTrue(ex.getMessage().contains("ya cuenta con un comprobante emitido: B001-00000026"));
        verify(invoiceRepo, never()).findMaxCorrelativeBySeries(anyString());
        verify(invoiceRepo, never()).save(any(Invoice.class));
    }

    // CASO DE PRUEBA CP-CPE-09: No se emite comprobante para un pedido que aún no está pagado
    @Test
    void emitInvoice_pedidoPendiente_lanzaExcepcion() {
        // preparacion
        Order pPendiente = new Order();
        pPendiente.setIdOrder(3);
        pPendiente.setStatus(OrderStatus.PENDIENTE); // Saldo pendiente

        when(orderRepo.findById(3)).thenReturn(Optional.of(pPendiente));

        InvoiceEmitRequestDTO request = new InvoiceEmitRequestDTO();
        request.setIdOrder(3);
        request.setInvoiceType(InvoiceType.BOLETA);

        // ejecucion y afirmacion
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> invoiceService.emitInvoice(request, mockUser)
        );

        assertTrue(ex.getMessage().contains("pedido pagado"));
        verify(invoiceRepo, never()).save(any(Invoice.class));
    }
}
