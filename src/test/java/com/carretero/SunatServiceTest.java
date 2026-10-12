package com.carretero;

import com.carretero.model.BusinessConfig;
import com.carretero.model.Client;
import com.carretero.model.Invoice;
import com.carretero.model.enums.DocumentType;
import com.carretero.model.enums.InvoiceType;
import com.carretero.model.enums.SunatEnvironment;
import com.carretero.model.enums.SunatStatus;
import com.carretero.repository.IBusinessConfigRepository;
import com.carretero.repository.IClientRepository;
import com.carretero.service.billing.SimulatedBillingProvider;
import com.carretero.service.billing.SunatBillingProvider;
import com.carretero.service.implementation.SunatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para SunatService.
 * Guiadas de Semana07_CalProSoft y documentadas en ANEXOS: CP-CPE-07.
 */
@ExtendWith(MockitoExtension.class)
public class SunatServiceTest {

    @Mock
    private IClientRepository clientRepository;

    @Mock
    private IBusinessConfigRepository businessConfigRepository;

    private SunatService sunatService;
    private BusinessConfig configSimulado;

    @BeforeEach
    void setUp() {
        configSimulado = new BusinessConfig();
        configSimulado.setRuc("20123456789");
        configSimulado.setSunatEnvironment(SunatEnvironment.SIMULADO);

        SimulatedBillingProvider simulatedProvider = new SimulatedBillingProvider();
        SunatBillingProvider sunatProvider = new SunatBillingProvider(null, null, null, null, null, null, null);

        sunatService = new SunatService(clientRepository, businessConfigRepository, simulatedProvider, sunatProvider);
    }

    // CASO DE PRUEBA CP-CPE-07: Nota de venta no se envía a SUNAT; boleta en modo SIMULADO nunca queda ACEPTADO
    @Test
    void dispatchToSunat_notaVentaYBoletaSimulada_estadosCorrectos() {
        // preparacion
        Invoice notaVenta = new Invoice();
        notaVenta.setIdInvoice(1);
        notaVenta.setInvoiceType(InvoiceType.NOTA_VENTA);
        notaVenta.setSeries("NV01");
        notaVenta.setCorrelativeNumber(1);
        notaVenta.setFullNumber("NV01-00000001");
        notaVenta.setTotalAmount(new BigDecimal("50.00"));
        notaVenta.setTaxableAmount(new BigDecimal("42.37"));
        notaVenta.setIgvAmount(new BigDecimal("7.63"));
        notaVenta.setIssueDate(LocalDateTime.now());

        Invoice boleta = new Invoice();
        boleta.setIdInvoice(2);
        boleta.setInvoiceType(InvoiceType.BOLETA);
        boleta.setSeries("B001");
        boleta.setCorrelativeNumber(27);
        boleta.setFullNumber("B001-00000027");
        boleta.setTotalAmount(new BigDecimal("74.00"));
        boleta.setTaxableAmount(new BigDecimal("62.71"));
        boleta.setIgvAmount(new BigDecimal("11.29"));
        boleta.setIssueDate(LocalDateTime.now());

        Client cliente = new Client();
        cliente.setDocType(DocumentType.DNI);
        cliente.setDocNumber("71234567");
        cliente.setName("Cliente Prueba");
        boleta.setClient(cliente);

        when(businessConfigRepository.findFirstByActiveTrue()).thenReturn(Optional.of(configSimulado));

        // ejecucion: Despachar Nota de Venta (A) y Boleta en Simulado (B)
        Invoice resultadoNotaVenta = sunatService.dispatchToSunat(notaVenta);
        Invoice resultadoBoleta = sunatService.dispatchToSunat(boleta);

        // afirmacion
        // A: Nota de venta interna (no se declara a SUNAT)
        assertNotNull(resultadoNotaVenta);
        assertEquals(SunatStatus.NO_ENVIADO, resultadoNotaVenta.getSunatStatus());
        assertEquals("Nota de venta interna (no se declara a SUNAT)", resultadoNotaVenta.getSunatDescription());
        assertNull(resultadoNotaVenta.getCdrHash());

        // B: Boleta en ambiente SIMULADO
        assertNotNull(resultadoBoleta);
        assertNotEquals(SunatStatus.ACEPTADO, resultadoBoleta.getSunatStatus());
        assertEquals(SunatStatus.SIMULADO, resultadoBoleta.getSunatStatus());
        assertNull(resultadoBoleta.getCdrHash());
        assertNotNull(resultadoBoleta.getQrData());
    }
}
