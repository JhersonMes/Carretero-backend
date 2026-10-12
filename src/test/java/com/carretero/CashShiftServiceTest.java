package com.carretero;

import com.carretero.dto.CashMovementDTO;
import com.carretero.model.CashMovement;
import com.carretero.model.CashShift;
import com.carretero.model.DiningTable;
import com.carretero.model.Order;
import com.carretero.model.Payment;
import com.carretero.model.PaymentMethod;
import com.carretero.model.User;
import com.carretero.model.enums.CashMovementType;
import com.carretero.model.enums.CashShiftStatus;
import com.carretero.repository.ICashMovementRepository;
import com.carretero.repository.ICashShiftRepository;
import com.carretero.repository.IOrderRepository;
import com.carretero.repository.IPaymentRepository;
import com.carretero.service.implementation.CashShiftService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para CashShiftService.
 * Guiadas de Semana07_CalProSoft y especificadas en ANEXOS:
 * CP-CAJ-01, CP-CAJ-02, CP-CAJ-03, CP-CAJ-04, CP-04-4, CP-05-1, CP-05-2.
 */
@ExtendWith(MockitoExtension.class)
public class CashShiftServiceTest {

    @Mock
    private ICashShiftRepository shiftRepo;

    @Mock
    private ICashMovementRepository movementRepo;

    @Mock
    private IPaymentRepository paymentRepo;

    @Mock
    private IOrderRepository orderRepo;

    @InjectMocks
    private CashShiftService cashShiftService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setIdUser(1);
        mockUser.setUsername("caja1");
    }

    // CASO DE PRUEBA CP-CAJ-01: Apertura del turno de caja con monto inicial S/ 100.00
    @Test
    void openShift_sinTurnoAbierto_creaTurnoAbiertaConEsperadoIgualAlInicial() throws Exception {
        // preparacion
        BigDecimal initialAmount = new BigDecimal("100.00");
        String notes = "Apertura turno mañana";

        when(shiftRepo.findFirstByStatusOrderByOpenTimeDesc(CashShiftStatus.ABIERTA))
                .thenReturn(Optional.empty());
        when(shiftRepo.save(any(CashShift.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // ejecucion
        CashShift shift = cashShiftService.openShift(initialAmount, mockUser, notes);

        // afirmacion
        assertNotNull(shift);
        assertEquals(CashShiftStatus.ABIERTA, shift.getStatus());
        assertEquals(mockUser, shift.getUser());
        assertEquals(new BigDecimal("100.00"), shift.getInitialAmount());
        assertEquals(new BigDecimal("100.00"), shift.getExpectedCash());
        assertNotNull(shift.getOpenTime());

        ArgumentCaptor<CashShift> captor = ArgumentCaptor.forClass(CashShift.class);
        verify(shiftRepo).save(captor.capture());
        assertEquals(new BigDecimal("100.00"), captor.getValue().getInitialAmount());
    }

    // CASO DE PRUEBA CP-CAJ-02: Rechazo de un segundo turno mientras otro sigue ABIERTA
    @Test
    void openShift_conTurnoAbierto_lanzaIllegalStateException() {
        // preparacion
        BigDecimal initialAmount = new BigDecimal("200.00");
        CashShift turno1 = new CashShift();
        turno1.setIdCashShift(1);
        turno1.setStatus(CashShiftStatus.ABIERTA);

        when(shiftRepo.findFirstByStatusOrderByOpenTimeDesc(CashShiftStatus.ABIERTA))
                .thenReturn(Optional.of(turno1));

        // ejecucion y afirmacion
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> cashShiftService.openShift(initialAmount, mockUser, null)
        );

        assertEquals("Ya existe un turno de caja abierto. Ciérrelo antes de abrir uno nuevo.", exception.getMessage());
        verify(shiftRepo, never()).save(any(CashShift.class));
    }

    // CASO DE PRUEBA CP-CAJ-03: La caja no cierra mientras la Mesa 3 tenga consumo sin cobrar
    @Test
    void closeShift_conMesaSinCobrar_lanzaExcepcionQueNombraLaMesa() {
        // preparacion
        CashShift openShift = new CashShift();
        openShift.setIdCashShift(1);
        openShift.setStatus(CashShiftStatus.ABIERTA);
        openShift.setInitialAmount(new BigDecimal("100.00"));
        openShift.setExpectedCash(new BigDecimal("150.00"));

        DiningTable mesa3 = new DiningTable();
        mesa3.setIdTable(3);
        mesa3.setName("Mesa 3");

        Order p1 = new Order();
        p1.setIdOrder(7);
        p1.setTable(mesa3);

        when(shiftRepo.findById(1)).thenReturn(Optional.of(openShift));
        when(orderRepo.findUncollectedTableOrders(anyList())).thenReturn(List.of(p1));

        // ejecucion y afirmacion
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> cashShiftService.closeShift(1, new BigDecimal("150.00"), null)
        );

        assertTrue(ex.getMessage().contains("Mesa 3"));
        assertTrue(ex.getMessage().contains("falta cobrar 1 mesa"));
        assertEquals(CashShiftStatus.ABIERTA, openShift.getStatus());
        verify(shiftRepo, never()).save(any(CashShift.class));
    }

    // CASO DE PRUEBA CP-CAJ-04: Cierre con arqueo tras pago mixto (esperado 150.00, contado 148.00, faltante -2.00)
    @Test
    void closeShift_trasPagoMixto_calculaDiferenciaFaltante() throws Exception {
        // preparacion
        CashShift openShift = new CashShift();
        openShift.setIdCashShift(1);
        openShift.setStatus(CashShiftStatus.ABIERTA);
        openShift.setInitialAmount(new BigDecimal("100.00"));
        openShift.setExpectedCash(new BigDecimal("100.00"));

        PaymentMethod pmEfectivo = new PaymentMethod();
        pmEfectivo.setName("EFECTIVO");
        Payment pagoEfectivo = new Payment();
        pagoEfectivo.setAmount(new BigDecimal("50.00"));
        pagoEfectivo.setPaymentMethod(pmEfectivo);

        PaymentMethod pmYape = new PaymentMethod();
        pmYape.setName("YAPE");
        Payment pagoYape = new Payment();
        pagoYape.setAmount(new BigDecimal("24.00"));
        pagoYape.setPaymentMethod(pmYape);

        when(shiftRepo.findById(1)).thenReturn(Optional.of(openShift));
        when(orderRepo.findUncollectedTableOrders(anyList())).thenReturn(Collections.emptyList());
        when(paymentRepo.findByCashShiftIdCashShift(1)).thenReturn(List.of(pagoEfectivo, pagoYape));
        when(movementRepo.findByCashShiftIdCashShift(1)).thenReturn(Collections.emptyList());
        when(shiftRepo.save(any(CashShift.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BigDecimal actualCash = new BigDecimal("148.00");
        String notes = "Cierre turno mañana";

        // ejecucion
        CashShift closed = cashShiftService.closeShift(1, actualCash, notes);

        // afirmacion
        assertNotNull(closed);
        assertEquals(CashShiftStatus.CERRADA, closed.getStatus());
        assertEquals(new BigDecimal("50.00"), closed.getCashSales());
        assertEquals(new BigDecimal("24.00"), closed.getYapeSales());
        assertEquals(new BigDecimal("150.00"), closed.getExpectedCash());
        assertEquals(new BigDecimal("148.00"), closed.getActualCash());
        assertEquals(new BigDecimal("-2.00"), closed.getDifference());
        assertNotNull(closed.getCloseTime());
    }

    // CASO DE PRUEBA CP-04-4: Cierre sobre turno cerrado lanza IllegalStateException
    @Test
    void closeShift_turnoYaCerrado_lanzaIllegalStateException() {
        // preparacion
        CashShift closedShift = new CashShift();
        closedShift.setIdCashShift(1);
        closedShift.setStatus(CashShiftStatus.CERRADA);

        when(shiftRepo.findById(1)).thenReturn(Optional.of(closedShift));

        // ejecucion y afirmacion
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> cashShiftService.closeShift(1, new BigDecimal("100.00"), null)
        );

        assertEquals("El turno de caja ya se encuentra cerrado.", ex.getMessage());
    }

    // CASO DE PRUEBA CP-05-1: Recálculo de totales (monto inicial 100.00, EFECTIVO 120.00, YAPE 40.00, egreso 30.00)
    @Test
    void recalculateShiftTotals_conEfectivoYapeYEgreso_calculaEfectivoEsperado() throws Exception {
        // preparacion
        CashShift shift = new CashShift();
        shift.setIdCashShift(2);
        shift.setStatus(CashShiftStatus.ABIERTA);
        shift.setInitialAmount(new BigDecimal("100.00"));

        PaymentMethod pmEfectivo = new PaymentMethod();
        pmEfectivo.setName("EFECTIVO");
        Payment p1 = new Payment();
        p1.setAmount(new BigDecimal("120.00"));
        p1.setPaymentMethod(pmEfectivo);

        PaymentMethod pmYape = new PaymentMethod();
        pmYape.setName("YAPE");
        Payment p2 = new Payment();
        p2.setAmount(new BigDecimal("40.00"));
        p2.setPaymentMethod(pmYape);

        CashMovement egreso = new CashMovement();
        egreso.setType(CashMovementType.EGRESO);
        egreso.setAmount(new BigDecimal("30.00"));

        when(shiftRepo.findById(2)).thenReturn(Optional.of(shift));
        when(paymentRepo.findByCashShiftIdCashShift(2)).thenReturn(List.of(p1, p2));
        when(movementRepo.findByCashShiftIdCashShift(2)).thenReturn(List.of(egreso));
        when(shiftRepo.save(any(CashShift.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ejecucion
        CashShift result = cashShiftService.recalculateShiftTotals(2);

        // afirmacion: expectedCash = 100 + 120 + 0 - 30 = 190.00 (el Yape no suma al efectivo fisico)
        assertNotNull(result);
        assertEquals(new BigDecimal("120.00"), result.getCashSales());
        assertEquals(new BigDecimal("40.00"), result.getYapeSales());
        assertEquals(new BigDecimal("30.00"), result.getCashOut());
        assertEquals(new BigDecimal("190.00"), result.getExpectedCash());
    }

    // CASO DE PRUEBA CP-05-2: Registro de egreso sobre turno cerrado lanza IllegalStateException
    @Test
    void registerMovement_turnoYaCerrado_lanzaIllegalStateException() {
        // preparacion
        CashShift closedShift = new CashShift();
        closedShift.setIdCashShift(3);
        closedShift.setStatus(CashShiftStatus.CERRADA);

        when(shiftRepo.findById(3)).thenReturn(Optional.of(closedShift));

        CashMovementDTO dto = new CashMovementDTO();
        dto.setIdCashShift(3);
        dto.setType(CashMovementType.EGRESO);
        dto.setAmount(new BigDecimal("30.00"));
        dto.setReason("Compra de insumos");

        // ejecucion y afirmacion
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> cashShiftService.registerMovement(dto, mockUser)
        );

        assertEquals("No se pueden registrar movimientos en un turno de caja cerrado.", ex.getMessage());
        verify(movementRepo, never()).save(any(CashMovement.class));
    }
}
