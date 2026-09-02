package com.carretero;

import com.carretero.dto.InvoiceEmitRequestDTO;
import com.carretero.model.*;
import com.carretero.model.enums.DocumentType;
import com.carretero.model.enums.InvoiceType;
import com.carretero.repository.*;
import com.carretero.service.ISunatService;
import com.carretero.service.implementation.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.carretero.model.enums.SunatStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

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

    private Order mockOrder;
    private BusinessConfig mockConfig;
    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUsername("cajero1");

        mockOrder = new Order();
        mockOrder.setIdOrder(100);
        mockOrder.setOrderCode("PED-20260823-0001");
        mockOrder.setTotal(new BigDecimal("118.00")); // Total 118 -> Gravada 100.00, IGV 18.00

        mockConfig = new BusinessConfig();
        mockConfig.setBoletaSeries("B001");
        mockConfig.setFacturaSeries("F001");
        mockConfig.setNotaVentaSeries("NV01");
    }

    @Test
    void testEmitBoletaCalculatesIgvCorrectly() throws Exception {
        when(orderRepo.findById(100)).thenReturn(Optional.of(mockOrder));
        when(invoiceRepo.findByOrderIdOrderAndSunatStatusNot(100, SunatStatus.ANULADO))
                .thenReturn(List.of());
        when(configRepo.findFirstByActiveTrue()).thenReturn(Optional.of(mockConfig));
        when(invoiceRepo.findMaxCorrelativeBySeries("B001")).thenReturn(4);
        when(sunatService.dispatchToSunat(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceRepo.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceEmitRequestDTO request = new InvoiceEmitRequestDTO();
        request.setIdOrder(100);
        request.setInvoiceType(InvoiceType.BOLETA);
        request.setDocNumber("71234567");
        request.setDocType(DocumentType.DNI);
        request.setClientName("Juan Perez");

        Invoice emitted = invoiceService.emitInvoice(request, mockUser);

        assertNotNull(emitted);
        assertEquals("B001", emitted.getSeries());
        assertEquals(5, emitted.getCorrelativeNumber());
        assertEquals("B001-00000005", emitted.getFullNumber());
        assertEquals(new BigDecimal("100.00"), emitted.getTaxableAmount());
        assertEquals(new BigDecimal("18.00"), emitted.getIgvAmount());
        assertEquals(new BigDecimal("118.00"), emitted.getTotalAmount());
    }

    @Test
    void testEmitFacturaRequires11DigitRuc() {
        when(orderRepo.findById(100)).thenReturn(Optional.of(mockOrder));
        when(invoiceRepo.findByOrderIdOrderAndSunatStatusNot(100, SunatStatus.ANULADO))
                .thenReturn(List.of());
        when(configRepo.findFirstByActiveTrue()).thenReturn(Optional.of(mockConfig));

        InvoiceEmitRequestDTO request = new InvoiceEmitRequestDTO();
        request.setIdOrder(100);
        request.setInvoiceType(InvoiceType.FACTURA);
        request.setDocNumber("12345"); // RUC inválido

        assertThrows(IllegalArgumentException.class, () -> invoiceService.emitInvoice(request, mockUser));
    }
}
