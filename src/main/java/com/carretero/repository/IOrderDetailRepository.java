package com.carretero.repository;

import com.carretero.model.OrderDetail;
import com.carretero.model.enums.KitchenStation;
import com.carretero.model.enums.OrderItemStatus;
import com.carretero.model.enums.OrderStatus;
import com.carretero.model.enums.OrderType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IOrderDetailRepository extends IGenericRepository<OrderDetail, Integer> {
    List<OrderDetail> findByOrderIdOrder(Integer idOrder);
    List<OrderDetail> findByItemStatus(OrderItemStatus itemStatus);

    /**
     * Comandas vivas de una estacion.
     *
     * - Un pedido ANULADO nunca es trabajo pendiente.
     * - Un pedido PAGADO de Salon tampoco: si la mesa se cobro antes de despachar, la
     *   cocina ya no tiene que prepararlo (CP-08-1-4).
     * - Pero Venta rapida y Delivery se cobran en el mismo gesto en que se envian: nacen
     *   PAGADOS y sus platos SI son trabajo pendiente hasta que la estacion los marque
     *   listos (defecto DEF-01, CP-09-2-1 b). Por eso PAGADO solo excluye al canal Salon.
     */
    @Query("SELECT d FROM OrderDetail d JOIN FETCH d.order o LEFT JOIN FETCH o.table " +
            "WHERE d.itemStatus IN :statuses " +
            "AND COALESCE(d.station, d.product.station) = :station " +
            "AND o.status <> :cancelled " +
            "AND (o.status <> :paid OR o.saleType <> :salon) " +
            "ORDER BY o.createdAt ASC")
    List<OrderDetail> findPendingByStation(@Param("statuses") List<OrderItemStatus> statuses,
                                            @Param("station") KitchenStation station,
                                            @Param("cancelled") OrderStatus cancelled,
                                            @Param("paid") OrderStatus paid,
                                            @Param("salon") OrderType salon);

    /**
     * Unidades vendidas por producto, para ordenar las sugerencias del buscador.
     *
     * Es una sola agregacion sobre los detalles, no una consulta por producto:
     * la pantalla la pide una vez al abrirse y con eso ordena todo lo que el
     * usuario teclee despues, sin volver al servidor.
     */
    @Query("SELECT d.product.idProduct, SUM(d.quantity) FROM OrderDetail d " +
            "WHERE d.order.status <> com.carretero.model.enums.OrderStatus.CANCELADO " +
            "GROUP BY d.product.idProduct")
    List<Object[]> sumSoldUnitsByProduct();
}
