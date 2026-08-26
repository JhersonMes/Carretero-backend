package com.carretero;

import com.carretero.dto.OrderCreateRequestDTO;
import com.carretero.model.Order;
import com.carretero.model.Product;
import com.carretero.model.User;
import com.carretero.model.enums.OrderStatus;
import com.carretero.model.enums.OrderType;
import com.carretero.repository.*;
import com.carretero.service.implementation.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

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

    @InjectMocks
    private OrderService orderService;

    private User mockUser;
    private Product burger;
    private Product drink;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setIdUser(1);
        mockUser.setUsername("cajero1");

        burger = new Product();
        burger.setIdProduct(1);
        burger.setName("Hamburguesa Carretera");
        burger.setPrice(new BigDecimal("16.00"));

        drink = new Product();
        drink.setIdProduct(2);
        drink.setName("Chicha Morada 1L");
        drink.setPrice(new BigDecimal("10.00"));
    }

    @Test
    void testCreateOrderWithCalculations() throws Exception {
        when(orderRepo.countOrdersToday(any())).thenReturn(5L);
        when(productRepo.findById(1)).thenReturn(Optional.of(burger));
        when(productRepo.findById(2)).thenReturn(Optional.of(drink));
        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderCreateRequestDTO dto = new OrderCreateRequestDTO();
        dto.setSaleType(OrderType.VENTA_RAPIDA);
        dto.setDiscount(new BigDecimal("2.00"));
        dto.setDeliveryFee(BigDecimal.ZERO);
        dto.setItems(List.of(
                new OrderCreateRequestDTO.OrderItemRequestDTO(1, 2, "Sin cebolla"), // 2 * 16.00 = 32.00
                new OrderCreateRequestDTO.OrderItemRequestDTO(2, 1, "Helada")      // 1 * 10.00 = 10.00
        )); // Subtotal = 42.00, Total = 42.00 - 2.00 = 40.00

        Order created = orderService.createOrder(dto, mockUser);

        assertNotNull(created);
        assertEquals(new BigDecimal("42.00"), created.getSubtotal());
        assertEquals(new BigDecimal("40.00"), created.getTotal());
        assertEquals(OrderStatus.PENDIENTE, created.getStatus());
        assertEquals(2, created.getDetails().size());
        assertTrue(created.getOrderCode().contains("PED-"));
    }
}
