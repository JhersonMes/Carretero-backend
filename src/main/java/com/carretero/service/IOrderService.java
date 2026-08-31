package com.carretero.service;

import com.carretero.dto.OrderCreateRequestDTO;
import com.carretero.model.Order;
import com.carretero.model.OrderDetail;
import com.carretero.model.User;
import com.carretero.model.enums.KitchenStation;
import com.carretero.model.enums.OrderItemStatus;
import com.carretero.model.enums.OrderStatus;
import com.carretero.model.enums.OrderType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IOrderService extends IGenericService<Order, Integer> {
    Order createOrder(OrderCreateRequestDTO request, User user) throws Exception;
    Order addItemsToOrder(Integer idOrder, List<OrderCreateRequestDTO.OrderItemRequestDTO> items, User user) throws Exception;
    Order updateOrderStatus(Integer idOrder, OrderStatus status) throws Exception;
    OrderDetail updateItemStatus(Integer idOrderDetail, OrderItemStatus status) throws Exception;
    List<Order> findByStatus(OrderStatus status);
    List<Order> findActiveOrders();
    List<Order> findBySaleType(OrderType saleType);
    List<Order> findOrdersByDateRange(LocalDateTime start, LocalDateTime end);
    Order findByOrderCode(String orderCode) throws Exception;
    void recalculateOrderTotals(Order order);
    Optional<Order> findActiveOrderForTable(Integer idTable);
    List<OrderDetail> findPendingItemsByStation(KitchenStation station);
}
