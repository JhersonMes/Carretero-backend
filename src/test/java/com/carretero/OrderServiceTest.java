package com.carretero;

import com.carretero.dto.OrderCreateRequestDTO;
import com.carretero.model.*;
import com.carretero.model.enums.*;
import com.carretero.repository.*;
import com.carretero.service.ICashShiftService;
import com.carretero.service.implementation.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para OrderService.
 * Guiadas de Semana07_CalProSoft y documentadas en ANEXOS:
 * CP-PED-01, CP-PED-05, CP-PED-06, CP-PED-07, CP-PED-08, CP-PED-09,
 * CP-PED-15, CP-PED-16, CP-COM-03, CP-COM-04, CP-COB-09, CP-08-1.
 */
@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private IOrderRepository orderRepo;

    @Mock
    private IOrderDetailRepository orderDetailRepo;

    @Mock
    private IProductRepository productRepo;

    @Mock
    private IDiningTableRepository tableRepo;

    @Mock
    private IClientRepository clientRepo;

    @Mock
    private IAddressRepository addressRepo;

    @Mock
    private IFlavorRepository flavorRepo;

    @Mock
    private ICashShiftService cashShiftService;

    @InjectMocks
    private OrderService orderService;

    private User mesero1;
    private User mesero2;
    private CashShift turnoActivo;

    @BeforeEach
    void setUp() {
        mesero1 = new User();
        mesero1.setIdUser(1);
        mesero1.setUsername("mesero1");

        mesero2 = new User();
        mesero2.setIdUser(2);
        mesero2.setUsername("mesero2");

        turnoActivo = new CashShift();
        turnoActivo.setIdCashShift(1);
        turnoActivo.setStatus(CashShiftStatus.ABIERTA);
    }

    // CASO DE PRUEBA CP-PED-01: Envío de pedido de mesa sin caja aperturada
    @Test
    void createOrder_sinCajaAbierta_lanzaExcepcionYNoPersisteNada() {
        // preparacion
        when(cashShiftService.getActiveShift()).thenReturn(Optional.empty());

        OrderCreateRequestDTO dto = new OrderCreateRequestDTO();
        dto.setSaleType(OrderType.SALON);
        dto.setIdTable(3);
        dto.setItems(List.of(
                new OrderCreateRequestDTO.OrderItemRequestDTO(1, 2, 1, "BBQ", KitchenStation.COCINA, null)
        ));

        // ejecucion y afirmacion
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> orderService.createOrder(dto, mesero1)
        );

        assertEquals("No hay una caja aperturada. Debes aperturar caja antes de enviar pedidos.", ex.getMessage());
        verify(orderRepo, never()).save(any(Order.class));
        verify(tableRepo, never()).save(any(DiningTable.class));
    }

    // CASO DE PRUEBA CP-PED-05: El servidor recalcula el precio unitario con el ajuste de la opción (Café + Latte = S/ 8.00)
    @Test
    void createOrder_conOpcionConAjuste_unitPriceEsBaseMasDelta() throws Exception {
        // preparacion
        when(cashShiftService.getActiveShift()).thenReturn(Optional.of(turnoActivo));
        when(orderRepo.countOrdersToday(any())).thenReturn(0L);

        Product cafe = new Product();
        cafe.setIdProduct(5);
        cafe.setName("Café");
        cafe.setPrice(new BigDecimal("7.00"));

        Flavor latte = new Flavor();
        latte.setIdFlavor(2);
        latte.setName("Latte");
        latte.setPriceDelta(new BigDecimal("1.00"));

        when(productRepo.findById(5)).thenReturn(Optional.of(cafe));
        when(flavorRepo.findById(2)).thenReturn(Optional.of(latte));
        when(orderRepo.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderCreateRequestDTO dto = new OrderCreateRequestDTO();
        dto.setSaleType(OrderType.VENTA_RAPIDA);
        dto.setItems(List.of(
                new OrderCreateRequestDTO.OrderItemRequestDTO(5, 1, 2, "Latte", null, null)
        ));

        // ejecucion
        Order created = orderService.createOrder(dto, mesero1);

        // afirmacion
        assertNotNull(created);
        assertEquals(1, created.getDetails().size());
        OrderDetail detail = created.getDetails().get(0);
        assertEquals(new BigDecimal("8.00"), detail.getUnitPrice());
        assertEquals(new BigDecimal("8.00"), detail.getSubtotal());
        assertEquals(new BigDecimal("8.00"), created.getTotal());
    }

    // CASO DE PRUEBA CP-PED-06: Valor límite: ajuste de opción negativo nunca deja precio menor a 0
    @Test
    void createOrder_deltaNegativoMayorQueBase_unitPriceEsCero() throws Exception {
        // preparacion
        when(cashShiftService.getActiveShift()).thenReturn(Optional.of(turnoActivo));
        when(orderRepo.countOrdersToday(any())).thenReturn(0L);

        Product cafe = new Product();
        cafe.setIdProduct(5);
        cafe.setName("Café");
        cafe.setPrice(new BigDecimal("7.00"));

        Flavor promo = new Flavor();
        promo.setIdFlavor(9);
        promo.setName("Descuento Especial");
        promo.setPriceDelta(new BigDecimal("-10.00"));

        when(productRepo.findById(5)).thenReturn(Optional.of(cafe));
        when(flavorRepo.findById(9)).thenReturn(Optional.of(promo));
        when(orderRepo.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderCreateRequestDTO dto = new OrderCreateRequestDTO();
        dto.setSaleType(OrderType.VENTA_RAPIDA);
        dto.setItems(List.of(
                new OrderCreateRequestDTO.OrderItemRequestDTO(5, 2, 9, "Promo", null, null)
        ));

        // ejecucion
        Order created = orderService.createOrder(dto, mesero1);

        // afirmacion
        assertNotNull(created);
        OrderDetail detail = created.getDetails().get(0);
        assertEquals(BigDecimal.ZERO, detail.getUnitPrice());
        assertEquals(BigDecimal.ZERO, detail.getSubtotal());
        assertEquals(BigDecimal.ZERO, created.getTotal());
    }

    // CASO DE PRUEBA CP-PED-07: Envío del pedido de la Mesa 3: código del día, mesa OCUPADA, items EN_PREPARACION y totales
    @Test
    void createOrder_salonMesaLibre_generaCodigoOcupaMesaYCalculaTotal() throws Exception {
        // preparacion
        when(cashShiftService.getActiveShift()).thenReturn(Optional.of(turnoActivo));
        when(orderRepo.countOrdersToday(any())).thenReturn(5L); // Ya hay 5 pedidos hoy -> siguiente es 0006

        DiningTable mesa3 = new DiningTable();
        mesa3.setIdTable(3);
        mesa3.setName("Mesa 3");
        mesa3.setStatus(TableStatus.LIBRE);

        Product p1 = new Product();
        p1.setIdProduct(1);
        p1.setName("Alitas De Pollo X 6");
        p1.setPrice(new BigDecimal("24.50"));

        Product p2 = new Product();
        p2.setIdProduct(2);
        p2.setName("Alitas De Cerdo X 4 Unds");
        p2.setPrice(new BigDecimal("18.00"));

        Product p3 = new Product();
        p3.setIdProduct(3);
        p3.setName("Gaseosas");
        p3.setPrice(new BigDecimal("3.50"));

        when(tableRepo.findById(3)).thenReturn(Optional.of(mesa3));
        when(productRepo.findById(1)).thenReturn(Optional.of(p1));
        when(productRepo.findById(2)).thenReturn(Optional.of(p2));
        when(productRepo.findById(3)).thenReturn(Optional.of(p3));
        when(orderRepo.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderCreateRequestDTO dto = new OrderCreateRequestDTO();
        dto.setSaleType(OrderType.SALON);
        dto.setIdTable(3);
        dto.setDiscount(BigDecimal.ZERO);
        dto.setDeliveryFee(BigDecimal.ZERO);
        dto.setItems(List.of(
                new OrderCreateRequestDTO.OrderItemRequestDTO(1, 2, null, "BBQ", KitchenStation.COCINA, null),       // 2 * 24.50 = 49.00
                new OrderCreateRequestDTO.OrderItemRequestDTO(2, 1, null, "Maracuyá", KitchenStation.PARRILLA, null), // 1 * 18.00 = 18.00
                new OrderCreateRequestDTO.OrderItemRequestDTO(3, 2, null, "Inca Cola", KitchenStation.BEBIDAS, null)  // 2 * 3.50 = 7.00
        )); // Subtotal = 74.00, Total = 74.00

        // ejecucion
        Order created = orderService.createOrder(dto, mesero1);

        // afirmacion
        assertNotNull(created);
        String hoyStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertEquals("PED-" + hoyStr + "-0006", created.getOrderCode());
        assertEquals(OrderStatus.PENDIENTE, created.getStatus());
        assertEquals(OrderType.SALON, created.getSaleType());
        assertEquals(mesa3, created.getTable());
        assertEquals(TableStatus.OCUPADA, mesa3.getStatus());
        verify(tableRepo).save(mesa3);

        assertEquals(3, created.getDetails().size());
        for (OrderDetail d : created.getDetails()) {
            assertEquals(OrderItemStatus.EN_PREPARACION, d.getItemStatus());
            assertNotNull(d.getSentAt());
            assertEquals(mesero1, d.getUser());
        }

        assertEquals(new BigDecimal("74.00"), created.getSubtotal());
        assertEquals(new BigDecimal("74.00"), created.getTotal());
    }

    // CASO DE PRUEBA CP-PED-08: Valor límite del correlativo: primer pedido del día es -0001
    @Test
    void createOrder_primerPedidoDelDia_codigoTerminaEn0001() throws Exception {
        // preparacion
        when(cashShiftService.getActiveShift()).thenReturn(Optional.of(turnoActivo));
        when(orderRepo.countOrdersToday(any())).thenReturn(0L);

        Product cafe = new Product();
        cafe.setIdProduct(5);
        cafe.setName("Café");
        cafe.setPrice(new BigDecimal("7.00"));

        when(productRepo.findById(5)).thenReturn(Optional.of(cafe));
        when(orderRepo.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderCreateRequestDTO dto = new OrderCreateRequestDTO();
        dto.setSaleType(OrderType.VENTA_RAPIDA);
        dto.setItems(List.of(new OrderCreateRequestDTO.OrderItemRequestDTO(5, 1, null, "Americano", null, null)));

        // ejecucion
        Order created = orderService.createOrder(dto, mesero1);

        // afirmacion
        assertNotNull(created);
        String hoyStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertEquals("PED-" + hoyStr + "-0001", created.getOrderCode());
    }

    // CASO DE PRUEBA CP-PED-09: Valor límite: descuento mayor que el subtotal deja el total en 0.00, nunca negativo
    @Test
    void createOrder_descuentoMayorAlSubtotal_totalEsCero() throws Exception {
        // preparacion
        when(cashShiftService.getActiveShift()).thenReturn(Optional.of(turnoActivo));
        when(orderRepo.countOrdersToday(any())).thenReturn(1L);

        Product gaseosa = new Product();
        gaseosa.setIdProduct(3);
        gaseosa.setName("Gaseosas");
        gaseosa.setPrice(new BigDecimal("3.50"));

        when(productRepo.findById(3)).thenReturn(Optional.of(gaseosa));
        when(orderRepo.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderCreateRequestDTO dto = new OrderCreateRequestDTO();
        dto.setSaleType(OrderType.VENTA_RAPIDA);
        dto.setDiscount(new BigDecimal("10.00")); // Descuento 10.00 > Subtotal 7.00
        dto.setItems(List.of(new OrderCreateRequestDTO.OrderItemRequestDTO(3, 2, null, null, null, null)));

        // ejecucion
        Order created = orderService.createOrder(dto, mesero1);

        // afirmacion
        assertNotNull(created);
        assertEquals(new BigDecimal("7.00"), created.getSubtotal());
        assertEquals(BigDecimal.ZERO, created.getTotal());
    }

    // CASO DE PRUEBA CP-PED-15: Agregar platos al pedido abierto de la Mesa 3 por otro mesero
    @Test
    void addItemsToOrder_pedidoAbierto_agregaConUsuarioNuevoYRecalculaTotal() throws Exception {
        // preparacion
        Order p1 = new Order();
        p1.setIdOrder(1);
        p1.setOrderCode("PED-20261001-0001");
        p1.setStatus(OrderStatus.PENDIENTE);
        p1.setSubtotal(new BigDecimal("74.00"));
        p1.setTotal(new BigDecimal("74.00"));
        p1.setDetails(new ArrayList<>());

        OrderDetail d1 = new OrderDetail();
        d1.setIdOrderDetail(1);
        d1.setSubtotal(new BigDecimal("74.00"));
        p1.getDetails().add(d1);

        Product cafe = new Product();
        cafe.setIdProduct(5);
        cafe.setName("Café");
        cafe.setPrice(new BigDecimal("7.00"));

        Flavor latte = new Flavor();
        latte.setIdFlavor(2);
        latte.setName("Latte");
        latte.setPriceDelta(new BigDecimal("1.00"));

        when(orderRepo.findById(1)).thenReturn(Optional.of(p1));
        when(productRepo.findById(5)).thenReturn(Optional.of(cafe));
        when(flavorRepo.findById(2)).thenReturn(Optional.of(latte));
        when(orderDetailRepo.save(any(OrderDetail.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepo.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        List<OrderCreateRequestDTO.OrderItemRequestDTO> itemsNuevos = List.of(
                new OrderCreateRequestDTO.OrderItemRequestDTO(5, 1, 2, "Latte", KitchenStation.BEBIDAS, null)
        );

        // ejecucion
        Order updated = orderService.addItemsToOrder(1, itemsNuevos, mesero2);

        // afirmacion
        assertNotNull(updated);
        assertEquals(2, updated.getDetails().size());
        OrderDetail addedDetail = updated.getDetails().get(1);
        assertEquals(mesero2, addedDetail.getUser());
        assertEquals(OrderItemStatus.EN_PREPARACION, addedDetail.getItemStatus());
        assertNotNull(addedDetail.getSentAt());
        assertEquals(new BigDecimal("82.00"), updated.getSubtotal());
        assertEquals(new BigDecimal("82.00"), updated.getTotal());
    }

    // CASO DE PRUEBA CP-PED-16: No se pueden agregar platos a un pedido ya PAGADO
    @Test
    void addItemsToOrder_pedidoPagado_lanzaIllegalStateException() {
        // preparacion
        Order p1 = new Order();
        p1.setIdOrder(1);
        p1.setStatus(OrderStatus.PAGADO);

        when(orderRepo.findById(1)).thenReturn(Optional.of(p1));

        List<OrderCreateRequestDTO.OrderItemRequestDTO> items = List.of(
                new OrderCreateRequestDTO.OrderItemRequestDTO(3, 1, null, null, null, null)
        );

        // ejecucion y afirmacion
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> orderService.addItemsToOrder(1, items, mesero1)
        );

        assertTrue(ex.getMessage().contains("ya cerrado"));
        verify(orderDetailRepo, never()).save(any(OrderDetail.class));
    }

    // CASO DE PRUEBA CP-COM-03: Despachar un item: LISTO sella readyAt y deja de aparecer en la estación
    @Test
    void updateItemStatus_listo_sellaReadyAt() throws Exception {
        // preparacion
        OrderDetail item = new OrderDetail();
        item.setIdOrderDetail(10);
        item.setItemStatus(OrderItemStatus.EN_PREPARACION);
        item.setReadyAt(null);

        when(orderDetailRepo.findById(10)).thenReturn(Optional.of(item));
        when(orderDetailRepo.save(any(OrderDetail.class))).thenAnswer(i -> i.getArgument(0));

        // ejecucion
        OrderDetail result = orderService.updateItemStatus(10, OrderItemStatus.LISTO);

        // afirmacion
        assertNotNull(result);
        assertEquals(OrderItemStatus.LISTO, result.getItemStatus());
        assertNotNull(result.getReadyAt());
    }

    // CASO DE PRUEBA CP-COM-04: Devolver un item a preparación borra readyAt (el cronómetro del salón vuelve a correr)
    @Test
    void updateItemStatus_vuelveAEnPreparacion_limpiaReadyAt() throws Exception {
        // preparacion
        OrderDetail item = new OrderDetail();
        item.setIdOrderDetail(10);
        item.setItemStatus(OrderItemStatus.LISTO);
        item.setReadyAt(LocalDateTime.of(2026, 10, 1, 12, 30));

        when(orderDetailRepo.findById(10)).thenReturn(Optional.of(item));
        when(orderDetailRepo.save(any(OrderDetail.class))).thenAnswer(i -> i.getArgument(0));

        // ejecucion
        OrderDetail result = orderService.updateItemStatus(10, OrderItemStatus.EN_PREPARACION);

        // afirmacion
        assertNotNull(result);
        assertEquals(OrderItemStatus.EN_PREPARACION, result.getItemStatus());
        assertNull(result.getReadyAt());
    }

    // CASO DE PRUEBA CP-COB-09: Pagar un pedido no libera la mesa si le queda otro pedido abierto
    @Test
    void updateOrderStatus_pagadoConOtroPedidoAbierto_mesaSigueOcupada() throws Exception {
        // preparacion
        DiningTable mesa3 = new DiningTable();
        mesa3.setIdTable(3);
        mesa3.setName("Mesa 3");
        mesa3.setStatus(TableStatus.OCUPADA);

        Order p1 = new Order();
        p1.setIdOrder(1);
        p1.setTable(mesa3);
        p1.setStatus(OrderStatus.PENDIENTE);

        Order p2 = new Order();
        p2.setIdOrder(2);
        p2.setTable(mesa3);
        p2.setStatus(OrderStatus.PENDIENTE);

        when(orderRepo.findById(1)).thenReturn(Optional.of(p1));
        when(orderRepo.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        // Hay dos pedidos no cerrados en la mesa (P1 y P2)
        when(orderRepo.findByTableIdTableAndStatusNotIn(eq(3), anyList()))
                .thenReturn(List.of(p1, p2));

        // ejecucion
        Order result = orderService.updateOrderStatus(1, OrderStatus.PAGADO);

        // afirmacion: P1 queda PAGADO, pero la Mesa 3 NO se libera porque P2 sigue abierto
        assertNotNull(result);
        assertEquals(OrderStatus.PAGADO, result.getStatus());
        assertEquals(TableStatus.OCUPADA, mesa3.getStatus());
        verify(tableRepo, never()).save(any(DiningTable.class));
    }

    // CASO DE PRUEBA CP-08-1: Cada estación recibe solo los items de su área
    @Test
    void findPendingItemsByStation_filtraSoloItemsDeLaEstacion() {
        // preparacion
        OrderDetail dCocina = new OrderDetail();
        dCocina.setIdOrderDetail(1);
        dCocina.setStation(KitchenStation.COCINA);
        dCocina.setItemStatus(OrderItemStatus.EN_PREPARACION);

        when(orderDetailRepo.findPendingByStation(anyList(), eq(KitchenStation.COCINA), anyList()))
                .thenReturn(List.of(dCocina));

        // ejecucion
        List<OrderDetail> itemsCocina = orderService.findPendingItemsByStation(KitchenStation.COCINA);

        // afirmacion
        assertNotNull(itemsCocina);
        assertEquals(1, itemsCocina.size());
        assertEquals(KitchenStation.COCINA, itemsCocina.get(0).getStation());
    }
}
