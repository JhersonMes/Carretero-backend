package com.carretero.repository;

import com.carretero.model.Order;
import com.carretero.model.enums.OrderStatus;
import com.carretero.model.enums.OrderType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IOrderRepository extends IGenericRepository<Order, Integer> {
    Optional<Order> findByOrderCode(String orderCode);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByStatusIn(List<OrderStatus> statuses);
    List<Order> findBySaleType(OrderType saleType);
    List<Order> findByTableIdTableAndStatusNotIn(Integer idTable, List<OrderStatus> excludedStatuses);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.details " +
            "WHERE o.table.idTable = :idTable AND o.status NOT IN :excludedStatuses")
    List<Order> findActiveOrdersForTableWithDetails(@Param("idTable") Integer idTable,
                                                      @Param("excludedStatuses") List<OrderStatus> excludedStatuses);

    /**
     * Con open-in-view desactivado la sesion se cierra al salir del servicio, asi que
     * los pedidos que luego se mapean a DTO deben traer sus detalles ya cargados.
     */
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.details WHERE o.status IN :statuses")
    List<Order> findByStatusInWithDetails(@Param("statuses") List<OrderStatus> statuses);

    /**
     * Pedidos activos de mesa para el tablero de Salon, con los items y los meseros
     * ya cargados. Se traen en una sola consulta porque el tablero se refresca en
     * cada evento de cocina y un N+1 por mesa se notaria en la laptop del local.
     */
    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.details d " +
            "LEFT JOIN FETCH d.user " +
            "LEFT JOIN FETCH o.user " +
            "WHERE o.table IS NOT NULL AND o.status NOT IN :excludedStatuses")
    List<Order> findActiveTableOrdersForBoard(@Param("excludedStatuses") List<OrderStatus> excludedStatuses);

    /**
     * Mesas con consumo servido que todavia nadie cobro.
     *
     * Es la condicion que bloquea el cierre de caja. Solo mira pedidos de mesa: en
     * salon la cuenta se cobra al final de la comida, asi que un pedido abierto es
     * plata que sigue en el aire. La venta rapida y el delivery se cobran en el
     * momento de pedir y no dejan cuenta pendiente, por lo que no traban el cierre.
     */
    @Query("SELECT o FROM Order o JOIN FETCH o.table " +
            "WHERE o.table IS NOT NULL AND o.status NOT IN :collectedStatuses " +
            "ORDER BY o.createdAt")
    List<Order> findUncollectedTableOrders(@Param("collectedStatuses") List<OrderStatus> collectedStatuses);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.details WHERE o.status = :status")
    List<Order> findByStatusWithDetails(@Param("status") OrderStatus status);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.details WHERE o.saleType = :saleType")
    List<Order> findBySaleTypeWithDetails(@Param("saleType") OrderType saleType);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.details")
    List<Order> findAllWithDetails();

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.details WHERE o.orderCode = :orderCode")
    Optional<Order> findByOrderCodeWithDetails(@Param("orderCode") String orderCode);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.details WHERE o.idOrder = :idOrder")
    Optional<Order> findByIdWithDetails(@Param("idOrder") Integer idOrder);
    
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :start AND :end ORDER BY o.createdAt DESC")
    List<Order> findOrdersByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :startOfDay")
    Long countOrdersToday(@Param("startOfDay") LocalDateTime startOfDay);
}
