package com.carretero.repository;

import com.carretero.model.OrderDetail;
import com.carretero.model.enums.KitchenStation;
import com.carretero.model.enums.OrderItemStatus;
import com.carretero.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IOrderDetailRepository extends IGenericRepository<OrderDetail, Integer> {
    List<OrderDetail> findByOrderIdOrder(Integer idOrder);
    List<OrderDetail> findByItemStatus(OrderItemStatus itemStatus);

    /**
     * Comandas vivas de una estacion. Excluye los items cuyo pedido ya fue cobrado o
     * anulado: al cerrarse la venta el item deja de ser trabajo pendiente para la
     * estacion, aunque su propio itemStatus siga en PENDIENTE.
     */
    @Query("SELECT d FROM OrderDetail d JOIN FETCH d.order o LEFT JOIN FETCH o.table " +
            "WHERE d.itemStatus IN :statuses AND d.product.station = :station " +
            "AND o.status NOT IN :excludedOrderStatuses " +
            "ORDER BY o.createdAt ASC")
    List<OrderDetail> findPendingByStation(@Param("statuses") List<OrderItemStatus> statuses,
                                            @Param("station") KitchenStation station,
                                            @Param("excludedOrderStatuses") List<OrderStatus> excludedOrderStatuses);
}
