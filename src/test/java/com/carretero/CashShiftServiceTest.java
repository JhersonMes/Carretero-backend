package com.carretero;

import com.carretero.dto.CashMovementDTO;
import com.carretero.model.CashMovement;
import com.carretero.model.CashShift;
import com.carretero.model.User;
import com.carretero.model.enums.CashMovementType;
import com.carretero.model.enums.CashShiftStatus;
import com.carretero.model.DiningTable;
import com.carretero.model.Order;
import com.carretero.repository.ICashMovementRepository;
import com.carretero.repository.ICashShiftRepository;
import com.carretero.repository.IOrderRepository;
import com.carretero.repository.IPaymentRepository;
import com.carretero.service.implementation.CashShiftService;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashShiftServiceTest {

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
        mockUser.setUsername("cajero1");
    }

    @Test
    void testOpenShift() throws Exception {
        when(shiftRepo.findFirstByStatusOrderByOpenTimeDesc(CashShiftStatus.ABIERTA)).thenReturn(Optional.empty());
        when(shiftRepo.save(any(CashShift.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CashShift shift = cashShiftService.openShift(new BigDecimal("100.00"), mockUser, "Apertura turno mañana");

        assertNotNull(shift);
        assertEquals(CashShiftStatus.ABIERTA, shift.getStatus());
        assertEquals(new BigDecimal("100.00"), shift.getInitialAmount());
        assertEquals(new BigDecimal("100.00"), shift.getExpectedCash());
    }

    @Test
    void testRegisterMovementAndCloseShift() throws Exception {
        CashShift openShift = new CashShift();
        openShift.setIdCashShift(1);
        openShift.setStatus(CashShiftStatus.ABIERTA);
        openShift.setInitialAmount(new BigDecimal("100.00"));
        openShift.setExpectedCash(new BigDecimal("100.00"));

        when(shiftRepo.findById(1)).thenReturn(Optional.of(openShift));
        when(shiftRepo.save(any(CashShift.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CashMovement mockMov = new CashMovement();
        mockMov.setIdMovement(10);
        mockMov.setCashShift(openShift);
        mockMov.setUser(mockUser);
        mockMov.setType(CashMovementType.EGRESO);
        mockMov.setAmount(new BigDecimal("20.00"));
        mockMov.setReason("Compra de insumos");

        when(paymentRepo.findByCashShiftIdCashShift(1)).thenReturn(Collections.emptyList());
        when(movementRepo.findByCashShiftIdCashShift(1)).thenReturn(List.of(mockMov));
        // Sin mesas pendientes de cobro, que es lo que habilita el cierre.
        when(orderRepo.findUncollectedTableOrders(anyList())).thenReturn(Collections.emptyList());

        CashMovementDTO movDto = new CashMovementDTO();
        movDto.setIdCashShift(1);
        movDto.setType(CashMovementType.EGRESO);
        movDto.setAmount(new BigDecimal("20.00"));
        movDto.setReason("Compra de insumos");

        CashMovement mov = cashShiftService.registerMovement(movDto, mockUser);
        assertNotNull(mov);
        assertEquals(new BigDecimal("20.00"), openShift.getCashOut());
        assertEquals(new BigDecimal("80.00"), openShift.getExpectedCash());

        // Cerrar turno con S/ 85.00 reales -> Diferencia +5.00 (Sobrante)
        CashShift closed = cashShiftService.closeShift(1, new BigDecimal("85.00"), "Cierre de turno");
        assertEquals(CashShiftStatus.CERRADA, closed.getStatus());
        assertEquals(new BigDecimal("85.00"), closed.getActualCash());
        assertEquals(new BigDecimal("80.00"), closed.getExpectedCash());
        assertEquals(new BigDecimal("5.00"), closed.getDifference());
    }

    @Test
    void testCloseShiftBlockedWhenATableIsUncollected() {
        CashShift openShift = new CashShift();
        openShift.setIdCashShift(1);
        openShift.setStatus(CashShiftStatus.ABIERTA);
        openShift.setInitialAmount(new BigDecimal("100.00"));
        openShift.setExpectedCash(new BigDecimal("100.00"));

        DiningTable table = new DiningTable();
        table.setIdTable(3);
        table.setName("Mesa 3");

        Order uncollected = new Order();
        uncollected.setIdOrder(7);
        uncollected.setTable(table);

        when(shiftRepo.findById(1)).thenReturn(Optional.of(openShift));
        when(orderRepo.findUncollectedTableOrders(anyList())).thenReturn(List.of(uncollected));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> cashShiftService.closeShift(1, new BigDecimal("85.00"), null));

        assertTrue(error.getMessage().contains("Mesa 3"));
        // La caja no debe quedar cerrada ni recalculada tras el rechazo.
        assertEquals(CashShiftStatus.ABIERTA, openShift.getStatus());
        verify(shiftRepo, never()).save(any(CashShift.class));
    }
}
