package com.carretero.controller;

import com.carretero.dto.*;
import com.carretero.model.*;
import com.carretero.model.enums.KitchenStation;
import com.carretero.model.enums.OrderItemStatus;
import com.carretero.model.enums.OrderStatus;
import com.carretero.model.enums.OrderType;
import com.carretero.repository.IUserRepository;
import com.carretero.service.IOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final String KITCHEN_TOPIC = "/topic/kitchen";

    private final IOrderService service;
    private final IUserRepository userRepo;
    private final SimpMessagingTemplate messagingTemplate;

    @Qualifier("orderMapper")
    private final ModelMapper orderMapper;
    @Qualifier("diningTableMapper")
    private final ModelMapper tableMapper;
    @Qualifier("userMapper")
    private final ModelMapper userMapper;
    @Qualifier("clientMapper")
    private final ModelMapper clientMapper;
    @Qualifier("addressMapper")
    private final ModelMapper addressMapper;

    @GetMapping
    public ResponseEntity<List<OrderDTO>> findActiveOrders() {
        List<OrderDTO> list = service.findActiveOrders().stream()
                .map(this::mapToDTO)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/all")
    public ResponseEntity<List<OrderDTO>> findAll() throws Exception {
        List<OrderDTO> list = service.findAll().stream()
                .map(this::mapToDTO)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> findById(@PathVariable("id") Integer id) throws Exception {
        Order obj = service.findById(id);
        return ResponseEntity.ok(mapToDTO(obj));
    }

    @GetMapping("/code/{orderCode}")
    public ResponseEntity<OrderDTO> findByCode(@PathVariable("orderCode") String orderCode) throws Exception {
        Order obj = service.findByOrderCode(orderCode);
        return ResponseEntity.ok(mapToDTO(obj));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderDTO>> findByStatus(@PathVariable("status") OrderStatus status) {
        List<OrderDTO> list = service.findByStatus(status).stream()
                .map(this::mapToDTO)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/sale-type/{saleType}")
    public ResponseEntity<List<OrderDTO>> findBySaleType(@PathVariable("saleType") OrderType saleType) {
        List<OrderDTO> list = service.findBySaleType(saleType).stream()
                .map(this::mapToDTO)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/table/{idTable}/active")
    public ResponseEntity<OrderDTO> findActiveOrderForTable(@PathVariable("idTable") Integer idTable) {
        return service.findActiveOrderForTable(idTable)
                .map(order -> ResponseEntity.ok(mapToDTO(order)))
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/items/station/{station}")
    public ResponseEntity<List<KitchenItemDTO>> findPendingItemsByStation(@PathVariable("station") KitchenStation station) {
        List<KitchenItemDTO> list = service.findPendingItemsByStation(station).stream()
                .map(d -> new KitchenItemDTO(
                        d.getIdOrderDetail(),
                        d.getOrder().getOrderCode(),
                        d.getOrder().getTable() != null ? d.getOrder().getTable().getName() : "Venta rapida",
                        d.getProductName(),
                        d.getQuantity(),
                        d.getFlavorName(),
                        d.getNotes(),
                        d.getItemStatus(),
                        // Los items creados antes de existir sentAt caen al createdAt del pedido.
                        d.getSentAt() != null ? d.getSentAt() : d.getOrder().getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@Valid @RequestBody OrderCreateRequestDTO request) throws Exception {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepo.findOneByUsername(username);

        Order created = service.createOrder(request, currentUser);
        notifyKitchen();
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDTO(created));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<OrderDTO> addItemsToOrder(
            @PathVariable("id") Integer id,
            @Valid @RequestBody List<OrderCreateRequestDTO.OrderItemRequestDTO> items) throws Exception {
        Order updated = service.addItemsToOrder(id, items);
        notifyKitchen();
        return ResponseEntity.ok(mapToDTO(updated));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MESERO') or hasAuthority('CAJERO')")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable("id") Integer id,
            @RequestBody Map<String, OrderStatus> body) throws Exception {
        OrderStatus status = body.get("status");
        if (status == null) {
            return ResponseEntity.badRequest().build();
        }
        Order updated = service.updateOrderStatus(id, status);
        notifyKitchen();
        return ResponseEntity.ok(mapToDTO(updated));
    }

    @PatchMapping("/items/{idOrderDetail}/status")
    public ResponseEntity<OrderDetailDTO> updateItemStatus(
            @PathVariable("idOrderDetail") Integer idOrderDetail,
            @RequestBody Map<String, OrderItemStatus> body) throws Exception {
        OrderItemStatus status = body.get("status");
        if (status == null) {
            return ResponseEntity.badRequest().build();
        }
        OrderDetail updated = service.updateItemStatus(idOrderDetail, status);
        OrderDetailDTO dto = new OrderDetailDTO(
                updated.getIdOrderDetail(),
                updated.getProduct() != null ? updated.getProduct().getIdProduct() : null,
                updated.getProductName(),
                updated.getQuantity(),
                updated.getUnitPrice(),
                updated.getFlavorName(),
                updated.getNotes(),
                updated.getSubtotal(),
                updated.getItemStatus()
        );
        notifyKitchen();
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) throws Exception {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pageable")
    public ResponseEntity<Page<Order>> listPageable(Pageable pageable) {
        return ResponseEntity.ok(service.listPage(pageable));
    }

    private void notifyKitchen() {
        messagingTemplate.convertAndSend(KITCHEN_TOPIC, Map.of("event", "KITCHEN_UPDATED"));
    }

    private OrderDTO mapToDTO(Order order) {
        if (order == null) return null;
        OrderDTO dto = new OrderDTO();
        dto.setIdOrder(order.getIdOrder());
        dto.setOrderCode(order.getOrderCode());
        dto.setSaleType(order.getSaleType());
        dto.setStatus(order.getStatus());
        dto.setDeliveryFee(order.getDeliveryFee());
        dto.setSubtotal(order.getSubtotal());
        dto.setDiscount(order.getDiscount());
        dto.setTotal(order.getTotal());
        dto.setNotes(order.getNotes());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        if (order.getTable() != null) {
            dto.setTable(tableMapper.map(order.getTable(), DiningTableDTO.class));
        }
        if (order.getUser() != null) {
            dto.setUser(userMapper.map(order.getUser(), UserDTO.class));
        }
        if (order.getClient() != null) {
            dto.setClient(clientMapper.map(order.getClient(), ClientDTO.class));
        }
        if (order.getDeliveryAddress() != null) {
            dto.setDeliveryAddress(addressMapper.map(order.getDeliveryAddress(), AddressDTO.class));
        }
        if (order.getDetails() != null) {
            dto.setDetails(order.getDetails().stream()
                    .map(d -> new OrderDetailDTO(
                            d.getIdOrderDetail(),
                            d.getProduct() != null ? d.getProduct().getIdProduct() : null,
                            d.getProductName(),
                            d.getQuantity(),
                            d.getUnitPrice(),
                            d.getFlavorName(),
                            d.getNotes(),
                            d.getSubtotal(),
                            d.getItemStatus()
                    ))
                    .toList());
        }

        return dto;
    }
}
