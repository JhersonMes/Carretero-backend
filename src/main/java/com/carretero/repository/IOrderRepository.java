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
