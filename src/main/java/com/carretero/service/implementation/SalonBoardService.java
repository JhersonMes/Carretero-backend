package com.carretero.service.implementation;

import com.carretero.dto.TableBoardDTO;
import com.carretero.exception.ModelNotFoundException;
import com.carretero.model.DiningTable;
import com.carretero.model.Order;
import com.carretero.model.OrderDetail;
import com.carretero.model.User;
import com.carretero.model.enums.OrderItemStatus;
import com.carretero.model.enums.OrderStatus;
import com.carretero.repository.IDiningTableRepository;
import com.carretero.repository.IOrderRepository;
import com.carretero.service.ISalonBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Arma la vista del salon que ve el mesero.
 *
 * Vive aparte de DiningTableService porque cruza dos agregados (mesas y pedidos):
 * meterlo en el servicio de mesas obligaria a que este conozca el ciclo de vida de
 * los pedidos, y meterlo en el de pedidos lo haria depender del plano del salon.
 */
@Service
@RequiredArgsConstructor
public class SalonBoardService implements ISalonBoardService {

    /** Estados que ya no ocupan la mesa: al llegar aqui el cronometro desaparece. */
    private static final List<OrderStatus> CLOSED_STATUSES =
            List.of(OrderStatus.PAGADO, OrderStatus.CANCELADO);

    private final IDiningTableRepository tableRepo;
    private final IOrderRepository orderRepo;

    @Override
    @Transactional(readOnly = true)
    public List<TableBoardDTO> getBoard() {
        List<DiningTable> tables = tableRepo.findActiveOrdered();

        // Un solo viaje a la base por todos los pedidos abiertos del salon.
        Map<Integer, List<Order>> ordersByTable = orderRepo
                .findActiveTableOrdersForBoard(CLOSED_STATUSES).stream()
                .filter(o -> o.getTable() != null)
                .collect(Collectors.groupingBy(o -> o.getTable().getIdTable()));

        List<TableBoardDTO> board = new ArrayList<>();
        for (DiningTable table : tables) {
            List<Order> tableOrders = ordersByTable.getOrDefault(table.getIdTable(), List.of());
            board.add(toBoardEntry(table, tableOrders));
        }
        return board;
    }

    /**
     * Una mesa puede acumular mas de un pedido abierto (se pidio, se cobro a medias
     * y se volvio a pedir), asi que el resumen consolida los items de todos ellos y
     * expone como referencia el pedido mas reciente.
     */
    private TableBoardDTO toBoardEntry(DiningTable table, List<Order> orders) {
        TableBoardDTO dto = new TableBoardDTO();
        dto.setIdTable(table.getIdTable());
        dto.setName(table.getName());
        dto.setCapacity(table.getCapacity());
        dto.setStatus(table.getStatus());
        dto.setOrderIndex(table.getOrderIndex());
        dto.setWaiters(List.of());
        dto.setTotalItems(0);
        dto.setDispatchedItems(0);
        dto.setPendingItems(0);
        dto.setAllDispatched(false);

        if (orders.isEmpty()) {
            return dto;
        }

        Order latest = orders.stream()
                .max(Comparator.comparing(Order::getCreatedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(orders.get(0));

        dto.setIdOrder(latest.getIdOrder());
        dto.setOrderCode(latest.getOrderCode());
        dto.setOrderCreatedAt(latest.getCreatedAt());
        dto.setOrderTotal(orders.stream()
                .map(o -> o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        List<OrderDetail> items = orders.stream()
                .flatMap(o -> o.getDetails().stream())
                .toList();

        dto.setWaiters(collectWaiters(orders, items));

        int total = 0;
        int dispatched = 0;
        LocalDateTime firstSentAt = null;
        LocalDateTime oldestPendingSentAt = null;
        LocalDateTime lastReadyAt = null;

        for (Order order : orders) {
            for (OrderDetail item : order.getDetails()) {
                total++;

                // Los items registrados antes de existir sent_at caen al createdAt del
                // pedido, igual que hace la pantalla de comandas. Sin este respaldo el
                // cronometro de una mesa con historial antiguo se quedaria en cero.
                LocalDateTime sentAt = item.getSentAt() != null
                        ? item.getSentAt()
                        : order.getCreatedAt();

                if (sentAt != null && (firstSentAt == null || sentAt.isBefore(firstSentAt))) {
                    firstSentAt = sentAt;
                }

                if (item.getItemStatus() == OrderItemStatus.LISTO) {
                    dispatched++;
                    LocalDateTime readyAt = item.getReadyAt();
                    if (readyAt != null && (lastReadyAt == null || readyAt.isAfter(lastReadyAt))) {
                        lastReadyAt = readyAt;
                    }
                } else if (sentAt != null
                        && (oldestPendingSentAt == null || sentAt.isBefore(oldestPendingSentAt))) {
                    oldestPendingSentAt = sentAt;
                }
            }
        }

        dto.setTotalItems(total);
        dto.setDispatchedItems(dispatched);
        dto.setPendingItems(total - dispatched);
        dto.setFirstSentAt(firstSentAt != null ? firstSentAt : latest.getCreatedAt());
        dto.setOldestPendingSentAt(oldestPendingSentAt);
        dto.setLastReadyAt(lastReadyAt);
        dto.setAllDispatched(total > 0 && dispatched == total);

        return dto;
    }

    /**
     * Meseros que intervinieron, sin repetir. Se empieza por quien abrio cada pedido
     * y se suman los que agregaron items despues; los items antiguos no tienen mesero
     * registrado y simplemente no aportan nombre.
     */
    private List<String> collectWaiters(List<Order> orders, List<OrderDetail> items) {
        Set<String> names = new LinkedHashSet<>();
        orders.stream()
                .sorted(Comparator.comparing(Order::getCreatedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(Order::getUser)
                .map(this::displayName)
                .filter(Objects::nonNull)
                .forEach(names::add);

        items.stream()
                .sorted(Comparator.comparing(OrderDetail::getSentAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(OrderDetail::getUser)
                .map(this::displayName)
                .filter(Objects::nonNull)
                .forEach(names::add);

        return new ArrayList<>(names);
    }

    private String displayName(User user) {
        if (user == null) return null;
        String fullName = user.getFullName();
        return (fullName != null && !fullName.isBlank()) ? fullName : user.getUsername();
    }

    @Override
    @Transactional
    public void reorderTables(List<Integer> orderedTableIds) throws Exception {
        if (orderedTableIds == null || orderedTableIds.isEmpty()) {
            throw new IllegalStateException("Debe enviar al menos una mesa para reordenar el salon.");
        }

        Map<Integer, DiningTable> byId = tableRepo.findAllOrdered().stream()
                .collect(Collectors.toMap(DiningTable::getIdTable, Function.identity()));

        List<DiningTable> toSave = new ArrayList<>();
        int position = 0;
        for (Integer idTable : orderedTableIds) {
            DiningTable table = byId.get(idTable);
            if (table == null) {
                throw new ModelNotFoundException("Mesa no encontrada: " + idTable);
            }
            table.setOrderIndex(position++);
            toSave.add(table);
        }

        // Las mesas que no viajaron en la peticion (por ejemplo, inactivas) se
        // reacomodan detras para que no queden compitiendo por las mismas posiciones.
        for (DiningTable table : byId.values()) {
            if (!orderedTableIds.contains(table.getIdTable())) {
                table.setOrderIndex(position++);
                toSave.add(table);
            }
        }

        tableRepo.saveAll(toSave);
    }
}
