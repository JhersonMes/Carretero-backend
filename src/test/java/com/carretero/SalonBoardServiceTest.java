package com.carretero;

import com.carretero.dto.TableBoardDTO;
import com.carretero.model.DiningTable;
import com.carretero.model.Order;
import com.carretero.model.OrderDetail;
import com.carretero.model.User;
import com.carretero.model.enums.OrderItemStatus;
import com.carretero.model.enums.OrderStatus;
import com.carretero.model.enums.TableStatus;
import com.carretero.repository.IDiningTableRepository;
import com.carretero.repository.IOrderRepository;
import com.carretero.service.implementation.SalonBoardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para SalonBoardService.
 * Guiadas de Semana07_CalProSoft y documentadas en ANEXOS: CP-07-1.
 */
@ExtendWith(MockitoExtension.class)
public class SalonBoardServiceTest {

    @Mock
    private IDiningTableRepository tableRepo;

    @Mock
    private IOrderRepository orderRepo;

    @InjectMocks
    private SalonBoardService salonBoardService;

    private DiningTable mesa2;
    private User mesero1;
    private User mesero2;

    @BeforeEach
    void setUp() {
        mesa2 = new DiningTable();
        mesa2.setIdTable(2);
        mesa2.setName("Mesa 2");
        mesa2.setCapacity(4);
        mesa2.setStatus(TableStatus.OCUPADA);

        mesero1 = new User();
        mesero1.setIdUser(1);
        mesero1.setUsername("mesero1");

        mesero2 = new User();
        mesero2.setIdUser(2);
        mesero2.setUsername("mesero2");
    }

    // CASO DE PRUEBA CP-07-1: Tablero de salón consolida items, despachados y lista de meseros sin duplicados
    @Test
    void getBoard_acumulaTotalesYAgrupaMeserosSinRepetir() {
        // preparacion
        Order pedido1 = new Order();
        pedido1.setIdOrder(10);
        pedido1.setOrderCode("PED-20261001-0010");
        pedido1.setTable(mesa2);
        pedido1.setUser(mesero1);
        pedido1.setTotal(new BigDecimal("40.00"));
        pedido1.setStatus(OrderStatus.PENDIENTE);
        pedido1.setCreatedAt(LocalDateTime.now().minusMinutes(20));
        pedido1.setDetails(new ArrayList<>());

        // 3 items en pedido 1 (2 despachados, 1 pendiente)
        OrderDetail d1 = new OrderDetail();
        d1.setIdOrderDetail(1);
        d1.setUser(mesero1);
        d1.setItemStatus(OrderItemStatus.LISTO);
        d1.setSentAt(LocalDateTime.now().minusMinutes(18));
        d1.setReadyAt(LocalDateTime.now().minusMinutes(10));

        OrderDetail d2 = new OrderDetail();
        d2.setIdOrderDetail(2);
        d2.setUser(mesero1);
        d2.setItemStatus(OrderItemStatus.LISTO);
        d2.setSentAt(LocalDateTime.now().minusMinutes(17));
        d2.setReadyAt(LocalDateTime.now().minusMinutes(8));

        OrderDetail d3 = new OrderDetail();
        d3.setIdOrderDetail(3);
        d3.setUser(mesero2);
        d3.setItemStatus(OrderItemStatus.EN_PREPARACION);
        d3.setSentAt(LocalDateTime.now().minusMinutes(15));

        pedido1.getDetails().addAll(List.of(d1, d2, d3));

        // Pedido 2 de la misma mesa (2 items pendientes agregados por mesero2)
        Order pedido2 = new Order();
        pedido2.setIdOrder(11);
        pedido2.setOrderCode("PED-20261001-0011");
        pedido2.setTable(mesa2);
        pedido2.setUser(mesero2);
        pedido2.setTotal(new BigDecimal("34.00"));
        pedido2.setStatus(OrderStatus.PENDIENTE);
        pedido2.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        pedido2.setDetails(new ArrayList<>());

        OrderDetail d4 = new OrderDetail();
        d4.setIdOrderDetail(4);
        d4.setUser(mesero2);
        d4.setItemStatus(OrderItemStatus.EN_PREPARACION);
        d4.setSentAt(LocalDateTime.now().minusMinutes(4));

        OrderDetail d5 = new OrderDetail();
        d5.setIdOrderDetail(5);
        d5.setUser(mesero1);
        d5.setItemStatus(OrderItemStatus.EN_PREPARACION);
        d5.setSentAt(LocalDateTime.now().minusMinutes(3));

        pedido2.getDetails().addAll(List.of(d4, d5));

        when(tableRepo.findActiveOrdered()).thenReturn(List.of(mesa2));
        when(orderRepo.findActiveTableOrdersForBoard(anyList())).thenReturn(List.of(pedido1, pedido2));

        // ejecucion
        List<TableBoardDTO> board = salonBoardService.getBoard();

        // afirmacion
        assertNotNull(board);
        assertEquals(1, board.size());
        TableBoardDTO dtoMesa2 = board.get(0);

        assertEquals(2, dtoMesa2.getIdTable());
        assertEquals("Mesa 2", dtoMesa2.getName());
        assertEquals(5, dtoMesa2.getTotalItems());        // 3 de P1 + 2 de P2 = 5 items
        assertEquals(2, dtoMesa2.getDispatchedItems());   // 2 despachados
        assertEquals(3, dtoMesa2.getPendingItems());      // 3 pendientes
        assertEquals(new BigDecimal("74.00"), dtoMesa2.getOrderTotal()); // 40.00 + 34.00 = 74.00
        assertEquals(2, dtoMesa2.getWaiters().size());    // mesero1 y mesero2 sin repetir
        assertTrue(dtoMesa2.getWaiters().contains("mesero1"));
        assertTrue(dtoMesa2.getWaiters().contains("mesero2"));
    }
}
