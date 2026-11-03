package com.carretero;

import com.carretero.model.*;
import com.carretero.model.enums.KitchenStation;
import com.carretero.model.enums.OrderItemStatus;
import com.carretero.model.enums.OrderStatus;
import com.carretero.model.enums.OrderType;
import com.carretero.repository.IOrderDetailRepository;
import com.carretero.repository.IPriceHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de las consultas a la base de datos que causaban defectos, ejecutadas sobre
 * una base H2 en memoria (no basta con mocks: el error estaba en la consulta misma).
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.database=h2",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=prueba"
})
public class ConsultasRepositorioTest {

    @Autowired private TestEntityManager em;
    @Autowired private IOrderDetailRepository detailRepo;
    @Autowired private IPriceHistoryRepository priceHistoryRepo;

    private User caja1;
    private Product alitas;

    @BeforeEach
    void datos() {
        Role role = new Role();
        role.setName("CAJERO");
        em.persist(role);
        caja1 = new User();
        caja1.setUsername("caja1");
        caja1.setEmail("caja1@carretero.com");
        caja1.setPassword("x");
        caja1.setRole(role);
        em.persist(caja1);

        Category cat = new Category();
        cat.setName("ALITAS DE POLLO");
        cat.setStation(KitchenStation.COCINA);
        em.persist(cat);
        alitas = new Product();
        alitas.setName("Alitas De Pollo X 6");
        alitas.setPrice(new BigDecimal("24.50"));
        alitas.setCategory(cat);
        em.persist(alitas);
    }

    private Order pedido(String code, OrderType type, OrderStatus status) {
        Order o = new Order();
        o.setOrderCode(code);
        o.setSaleType(type);
        o.setStatus(status);
        o.setUser(caja1);
        o.setTotal(new BigDecimal("24.50"));
        o.setSubtotal(new BigDecimal("24.50"));
        em.persist(o);
        OrderDetail d = new OrderDetail();
        d.setOrder(o);
        d.setProduct(alitas);
        d.setProductName(alitas.getName());
        d.setQuantity(1);
        d.setUnitPrice(new BigDecimal("24.50"));
        d.setSubtotal(new BigDecimal("24.50"));
        d.setStation(KitchenStation.COCINA);
        d.setItemStatus(OrderItemStatus.EN_PREPARACION);
        d.setSentAt(LocalDateTime.now());
        em.persist(d);
        return o;
    }

    private List<String> codigosEnCocina() {
        em.flush();
        em.clear();
        return detailRepo.findPendingByStation(
                        List.of(OrderItemStatus.PENDIENTE, OrderItemStatus.EN_PREPARACION),
                        KitchenStation.COCINA, OrderStatus.CANCELADO, OrderStatus.PAGADO, OrderType.SALON)
                .stream().map(d -> d.getOrder().getOrderCode()).toList();
    }

    // CASO DE PRUEBA CP-09-2-1 (b) · DEF-01: la venta rapida ya cobrada SI llega a la cocina
    @Test
    void ventaRapidaPagada_apareceEnLaEstacion() {
        pedido("VR-PAGADA", OrderType.VENTA_RAPIDA, OrderStatus.PAGADO);
        pedido("DEL-PAGADO", OrderType.DELIVERY, OrderStatus.PAGADO);

        List<String> codigos = codigosEnCocina();

        assertTrue(codigos.contains("VR-PAGADA"));
        assertTrue(codigos.contains("DEL-PAGADO"));
    }

    // CASO DE PRUEBA CP-08-1-4: un pedido de Salon cobrado antes de despachar ya no aparece
    @Test
    void salonPagado_noApareceEnLaEstacion() {
        pedido("SALON-PAGADO", OrderType.SALON, OrderStatus.PAGADO);
        pedido("SALON-ABIERTO", OrderType.SALON, OrderStatus.PENDIENTE);
        pedido("VR-ANULADA", OrderType.VENTA_RAPIDA, OrderStatus.CANCELADO);

        List<String> codigos = codigosEnCocina();

        assertEquals(List.of("SALON-ABIERTO"), codigos);
    }

    // CASO DE PRUEBA CP-15-3-1: el historico de precios se consulta con su producto y usuario
    @Test
    void historicoDePrecios_traeProductoYUsuario() {
        PriceHistory h = new PriceHistory();
        h.setProduct(alitas);
        h.setPreviousPrice(new BigDecimal("24.50"));
        h.setNewPrice(new BigDecimal("26.00"));
        h.setUser(caja1);
        em.persist(h);
        em.flush();
        em.clear();

        List<PriceHistory> list = priceHistoryRepo.findHistoryWithDetails(alitas.getIdProduct());
        em.clear(); // fuera de la sesion: antes aqui fallaba con "could not initialize proxy"

        assertEquals(1, list.size());
        assertEquals("Alitas De Pollo X 6", list.get(0).getProduct().getName());
        assertEquals("caja1", list.get(0).getUser().getUsername());
        assertEquals(0, new BigDecimal("26.00").compareTo(list.get(0).getNewPrice()));
    }
}
