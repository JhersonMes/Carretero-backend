package com.carretero.service.implementation;

import com.carretero.dto.OrderCreateRequestDTO;
import com.carretero.exception.ModelNotFoundException;
import com.carretero.model.*;
import com.carretero.model.enums.CashShiftStatus;
import com.carretero.model.enums.KitchenStation;
import com.carretero.model.enums.OrderItemStatus;
import com.carretero.model.enums.OrderStatus;
import com.carretero.model.enums.OrderType;
import com.carretero.model.enums.SunatStatus;
import com.carretero.model.enums.TableStatus;
import com.carretero.repository.*;
import com.carretero.service.IBusinessConfigService;
import com.carretero.service.ICashShiftService;
import com.carretero.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService extends GenericService<Order, Integer> implements IOrderService {

    private final IOrderRepository repo;
    private final IOrderDetailRepository orderDetailRepo;
    private final IProductRepository productRepo;
    private final IDiningTableRepository tableRepo;
    private final IClientRepository clientRepo;
    private final IAddressRepository addressRepo;
    private final IFlavorRepository flavorRepo;
    private final IPaymentRepository paymentRepo;
    private final IInvoiceRepository invoiceRepo;
    private final ICashShiftService cashShiftService;
    private final IBusinessConfigService businessConfigService;

    @Override
    protected IGenericRepository<Order, Integer> getRepo() {
        return repo;
    }

    @Override
    @Transactional
    public Order createOrder(OrderCreateRequestDTO request, User user) throws Exception {
        if (cashShiftService.getActiveShift().isEmpty()) {
            throw new IllegalStateException("No hay una caja aperturada. Debes aperturar caja antes de enviar pedidos.");
        }

        Order order = new Order();
        order.setUser(user);
        order.setSaleType(request.getSaleType() != null ? request.getSaleType() : OrderType.SALON);
        order.setStatus(OrderStatus.PENDIENTE);
        order.setNotes(request.getNotes());
        order.setDeliveryFee(request.getDeliveryFee() != null ? request.getDeliveryFee() : BigDecimal.ZERO);
        order.setDiscount(request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO);

        // Generar código de pedido del día ej. PED-20260823-0001
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        Long countToday = repo.countOrdersToday(startOfDay);
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        order.setOrderCode(String.format("PED-%s-%04d", dateStr, countToday + 1));

        // Asignar Mesa si es Salón
        if (order.getSaleType() == OrderType.SALON && request.getIdTable() != null) {
            DiningTable table = tableRepo.findById(request.getIdTable())
                    .orElseThrow(() -> new ModelNotFoundException("Mesa no encontrada: " + request.getIdTable()));
            table.setStatus(TableStatus.OCUPADA);
            tableRepo.save(table);
            order.setTable(table);
        }

        // Asignar Cliente si se especificó
        if (request.getIdClient() != null) {
            Client client = clientRepo.findById(request.getIdClient())
                    .orElseThrow(() -> new ModelNotFoundException("Cliente no encontrado: " + request.getIdClient()));
            order.setClient(client);
        }

        // Asignar Dirección si es Delivery
        if (order.getSaleType() == OrderType.DELIVERY && request.getIdAddress() != null) {
            Address address = addressRepo.findById(request.getIdAddress())
                    .orElseThrow(() -> new ModelNotFoundException("Dirección no encontrada: " + request.getIdAddress()));
            order.setDeliveryAddress(address);
            // La tarifa la fija el jiron al que se reparte. Si la venta no manda una
            // propia, se cobra la que quedo guardada junto con la direccion.
            if (request.getDeliveryFee() == null && address.getDeliveryFee() != null) {
                order.setDeliveryFee(address.getDeliveryFee());
            }
        }

        List<OrderDetail> details = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderCreateRequestDTO.OrderItemRequestDTO itemReq : request.getItems()) {
            Product product = productRepo.findById(itemReq.getIdProduct())
                    .orElseThrow(() -> new ModelNotFoundException("Producto no encontrado: " + itemReq.getIdProduct()));

            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setUser(user);
            detail.setProduct(product);
            detail.setProductName(product.getName());
            BigDecimal unitPrice = resolveUnitPrice(product, itemReq);
            detail.setQuantity(itemReq.getQuantity());
            detail.setUnitPrice(unitPrice);
            detail.setFlavorName(itemReq.getFlavorName());
            detail.setNotes(itemReq.getNotes());

            BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            detail.setSubtotal(itemSubtotal);
            // Enviar el pedido ya pone la comanda en preparacion: la estacion no tiene
            // que aceptarla, solo despacharla cuando este lista.
            detail.setItemStatus(OrderItemStatus.EN_PREPARACION);
            detail.setSentAt(LocalDateTime.now());

            details.add(detail);
            subtotal = subtotal.add(itemSubtotal);
        }

        order.setDetails(details);
        order.setSubtotal(subtotal);
        
        BigDecimal total = subtotal.add(order.getDeliveryFee()).subtract(order.getDiscount());
        order.setTotal(total.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : total);

        return repo.save(order);
    }

    @Override
    @Transactional
    public Order addItemsToOrder(Integer idOrder, List<OrderCreateRequestDTO.OrderItemRequestDTO> items, User user) throws Exception {
        Order order = repo.findById(idOrder)
                .orElseThrow(() -> new ModelNotFoundException("Pedido no encontrado: " + idOrder));

        for (OrderCreateRequestDTO.OrderItemRequestDTO itemReq : items) {
            Product product = productRepo.findById(itemReq.getIdProduct())
                    .orElseThrow(() -> new ModelNotFoundException("Producto no encontrado: " + itemReq.getIdProduct()));

            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            // Quien agrega el plato queda registrado en el item: en una mesa larga
            // pueden turnarse varios meseros y el salon debe mostrarlos a todos.
            detail.setUser(user);
            detail.setProduct(product);
            detail.setProductName(product.getName());
            BigDecimal unitPrice = resolveUnitPrice(product, itemReq);
            detail.setQuantity(itemReq.getQuantity());
            detail.setUnitPrice(unitPrice);
            detail.setFlavorName(itemReq.getFlavorName());
            detail.setNotes(itemReq.getNotes());

            BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            detail.setSubtotal(itemSubtotal);
            detail.setItemStatus(OrderItemStatus.EN_PREPARACION);
            detail.setSentAt(LocalDateTime.now());

            orderDetailRepo.save(detail);
            order.getDetails().add(detail);
        }

        recalculateOrderTotals(order);
        return repo.save(order);
    }

    /**
     * Anula una venta ya cobrada.
     *
     * No borra nada: el pedido queda CANCELADO con quien lo anulo, cuando y por
     * que; los cobros siguen en su turno pero dejan de sumar al arqueo, y el
     * comprobante queda ANULADO. Asi la caja cuadra y despues se puede explicar
     * cada anulacion, que es justo lo que un borrado haria imposible.
     *
     * El PIN lo valida el servidor. Es lo unico que separa a cualquiera de poder
     * deshacer una venta cobrada, y comprobarlo en la pantalla no protege nada.
     */
    @Override
    @Transactional
    public Order cancelOrder(Integer idOrder, String pin, String reason, User user) throws Exception {
        if (!businessConfigService.matchesAdminPin(pin)) {
            throw new IllegalArgumentException("El PIN de autorizacion no es correcto.");
        }

        Order order = repo.findByIdWithDetails(idOrder)
                .orElseThrow(() -> new ModelNotFoundException("Pedido no encontrado: " + idOrder));

        if (order.getStatus() == OrderStatus.CANCELADO) {
            throw new IllegalStateException("El pedido " + order.getOrderCode() + " ya estaba anulado.");
        }

        // Un turno cerrado ya fue arqueado y su plata contada: tocarlo cambiaria
        // un cuadre que alguien ya firmo.
        List<Payment> payments = paymentRepo.findByOrderIdOrder(idOrder);
        for (Payment payment : payments) {
            CashShift shift = payment.getCashShift();
            if (shift != null && shift.getStatus() == CashShiftStatus.CERRADA) {
                throw new IllegalStateException(
                        "El pedido " + order.getOrderCode() + " se cobro en un turno de caja ya cerrado "
                                + "y no se puede anular. Registra la devolucion como movimiento de caja.");
            }
        }

        order.setStatus(OrderStatus.CANCELADO);
        order.setCancelledBy(user);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason(reason != null && !reason.isBlank() ? reason.trim() : null);

        // La mesa vuelve a estar libre si no le queda ningun otro pedido abierto.
        if (order.getTable() != null) {
            List<Order> activeTableOrders = repo.findByTableIdTableAndStatusNotIn(
                    order.getTable().getIdTable(),
                    List.of(OrderStatus.PAGADO, OrderStatus.CANCELADO));
            if (activeTableOrders.stream().noneMatch(o -> !o.getIdOrder().equals(idOrder))) {
                DiningTable table = order.getTable();
                table.setStatus(TableStatus.LIBRE);
                tableRepo.save(table);
            }
        }

        Order saved = repo.save(order);

        // El comprobante de una venta anulada no puede seguir vigente.
        invoiceRepo.findByOrderIdOrderAndSunatStatusNot(idOrder, SunatStatus.ANULADO)
                .forEach(invoice -> {
                    invoice.setSunatStatus(SunatStatus.ANULADO);
                    invoice.setSunatDescription("Anulado junto con el pedido " + order.getOrderCode());
                    invoiceRepo.save(invoice);
                });

        // Los cobros anulados dejan de contar en el turno; el recalculo los excluye.
        for (Payment payment : payments) {
            if (payment.getCashShift() != null) {
                cashShiftService.recalculateShiftTotals(payment.getCashShift().getIdCashShift());
            }
        }

        return saved;
    }

    /**
     * Quita un plato de un pedido ya enviado y recalcula la cuenta.
     *
     * Es para el error de comanda: se mando algo que el cliente no pidio. Pide
     * PIN porque baja el monto de una cuenta abierta y porque el plato pudo
     * haberse cocinado ya; agregar, en cambio, no lo pide.
     */
    @Override
    @Transactional
    public Order removeItem(Integer idOrderDetail, String pin, User user) throws Exception {
        if (!businessConfigService.matchesAdminPin(pin)) {
            throw new IllegalArgumentException("El PIN de autorizacion no es correcto.");
        }

        OrderDetail detail = orderDetailRepo.findById(idOrderDetail)
                .orElseThrow(() -> new ModelNotFoundException("Item de pedido no encontrado: " + idOrderDetail));

        Order order = repo.findByIdWithDetails(detail.getOrder().getIdOrder())
                .orElseThrow(() -> new ModelNotFoundException("Pedido no encontrado"));

        if (order.getStatus() == OrderStatus.PAGADO || order.getStatus() == OrderStatus.CANCELADO) {
            throw new IllegalStateException(
                    "El pedido " + order.getOrderCode() + " ya esta cerrado: no se le pueden quitar platos.");
        }

        // Vaciar el pedido plato por plato dejaria una mesa ocupada por una cuenta
        // en cero. Si no queda nada que servir, lo que corresponde es anularlo.
        if (order.getDetails().size() <= 1) {
            throw new IllegalStateException(
                    "Es el unico plato del pedido. Para dejarlo sin nada, anula el pedido completo.");
        }

        order.getDetails().removeIf(d -> d.getIdOrderDetail().equals(idOrderDetail));
        orderDetailRepo.delete(detail);

        recalculateOrderTotals(order);
        return repo.save(order);
    }

    /**
     * Nota del plato (ej. "sin aji"). No pide PIN: no toca el monto y suele ser
     * justamente lo que hay que corregir a ultimo momento.
     */
    @Override
    @Transactional
    public OrderDetail updateItemNotes(Integer idOrderDetail, String notes) throws Exception {
        OrderDetail detail = orderDetailRepo.findById(idOrderDetail)
                .orElseThrow(() -> new ModelNotFoundException("Item de pedido no encontrado: " + idOrderDetail));

        detail.setNotes(notes != null && !notes.isBlank() ? notes.trim() : null);
        return orderDetailRepo.save(detail);
    }

    @Override
    public List<Order> findCancelledOrders() {
        return repo.findByStatusWithDetails(OrderStatus.CANCELADO);
    }

    @Override
    @Transactional
    public Order updateOrderStatus(Integer idOrder, OrderStatus status) throws Exception {
        Order order = repo.findById(idOrder)
                .orElseThrow(() -> new ModelNotFoundException("Pedido no encontrado: " + idOrder));

        order.setStatus(status);

        // Si el pedido se pagó o se canceló y estaba en una mesa, liberar la mesa si no hay más pedidos activos
        if ((status == OrderStatus.PAGADO || status == OrderStatus.CANCELADO) && order.getTable() != null) {
            List<Order> activeTableOrders = repo.findByTableIdTableAndStatusNotIn(
                    order.getTable().getIdTable(),
                    List.of(OrderStatus.PAGADO, OrderStatus.CANCELADO)
            );
            if (activeTableOrders.stream().noneMatch(o -> !o.getIdOrder().equals(idOrder))) {
                DiningTable table = order.getTable();
                table.setStatus(TableStatus.LIBRE);
                tableRepo.save(table);
            }
        }

        return repo.save(order);
    }

    @Override
    @Transactional
    public OrderDetail updateItemStatus(Integer idOrderDetail, OrderItemStatus status) throws Exception {
        OrderDetail detail = orderDetailRepo.findById(idOrderDetail)
                .orElseThrow(() -> new ModelNotFoundException("Item de pedido no encontrado: " + idOrderDetail));
        detail.setItemStatus(status);

        // El sello de despacho detiene el cronometro que ve el mesero en la mesa.
        // Si la estacion devuelve el item a preparacion, el cronometro vuelve a correr.
        if (status == OrderItemStatus.LISTO) {
            detail.setReadyAt(LocalDateTime.now());
        } else {
            detail.setReadyAt(null);
        }

        return orderDetailRepo.save(detail);
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return repo.findByStatusWithDetails(status);
    }

    @Override
    public List<Order> findActiveOrders() {
        return repo.findByStatusInWithDetails(List.of(
                OrderStatus.PENDIENTE,
                OrderStatus.EN_PREPARACION,
                OrderStatus.LISTO,
                OrderStatus.ENTREGADO
        ));
    }

    @Override
    public List<Order> findBySaleType(OrderType saleType) {
        return repo.findBySaleTypeWithDetails(saleType);
    }

    @Override
    public List<Order> findAll() {
        return repo.findAllWithDetails();
    }

    @Override
    public Order findById(Integer id) throws Exception {
        return repo.findByIdWithDetails(id)
                .orElseThrow(() -> new ModelNotFoundException("Pedido no encontrado: " + id));
    }

    @Override
    public List<Order> findOrdersByDateRange(LocalDateTime start, LocalDateTime end) {
        return repo.findOrdersByDateRange(start, end);
    }

    @Override
    public Order findByOrderCode(String orderCode) throws Exception {
        return repo.findByOrderCodeWithDetails(orderCode)
                .orElseThrow(() -> new ModelNotFoundException("Pedido no encontrado con código: " + orderCode));
    }

    @Override
    public Optional<Order> findActiveOrderForTable(Integer idTable) {
        return repo.findActiveOrdersForTableWithDetails(idTable, List.of(OrderStatus.PAGADO, OrderStatus.CANCELADO))
                .stream()
                .findFirst();
    }

    @Override
    public List<OrderDetail> findPendingItemsByStation(KitchenStation station) {
        return orderDetailRepo.findPendingByStation(
                List.of(OrderItemStatus.PENDIENTE, OrderItemStatus.EN_PREPARACION),
                station,
                List.of(OrderStatus.PAGADO, OrderStatus.CANCELADO)
        );
    }

    /**
     * Precio unitario del item: el del producto mas el ajuste de la opcion elegida
     * (un latte cuesta mas que un americano aunque sean el mismo producto "Cafe").
     */
    private BigDecimal resolveUnitPrice(Product product, OrderCreateRequestDTO.OrderItemRequestDTO itemReq) {
        BigDecimal base = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
        if (itemReq.getIdFlavor() == null) {
            return base;
        }
        BigDecimal delta = flavorRepo.findById(itemReq.getIdFlavor())
                .map(f -> f.getPriceDelta() != null ? f.getPriceDelta() : BigDecimal.ZERO)
                .orElse(BigDecimal.ZERO);
        BigDecimal price = base.add(delta);
        return price.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : price;
    }

    @Override
    public void recalculateOrderTotals(Order order) {
        BigDecimal subtotal = BigDecimal.ZERO;
        if (order.getDetails() != null) {
            for (OrderDetail d : order.getDetails()) {
                if (d.getSubtotal() != null) {
                    subtotal = subtotal.add(d.getSubtotal());
                }
            }
        }
        order.setSubtotal(subtotal);
        BigDecimal deliveryFee = order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.ZERO;
        BigDecimal discount = order.getDiscount() != null ? order.getDiscount() : BigDecimal.ZERO;
        BigDecimal total = subtotal.add(deliveryFee).subtract(discount);
        order.setTotal(total.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : total);
    }
}
