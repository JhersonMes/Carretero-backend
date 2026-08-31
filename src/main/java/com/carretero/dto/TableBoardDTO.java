package com.carretero.dto;

import com.carretero.model.enums.TableStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Vista consolidada de una mesa para la pantalla de Salon.
 *
 * Reune en una sola llamada lo que el mesero necesita ver sin abrir la mesa:
 * quien la esta atendiendo, cuanto lleva esperando la comanda y si la estacion
 * ya la despacho. Se arma en el backend y no en el frontend porque implica
 * recorrer los items de cada pedido activo, y hacerlo en el cliente obligaria a
 * traer todos los pedidos completos en cada refresco.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableBoardDTO {

    private Integer idTable;
    private String name;
    private Integer capacity;
    private TableStatus status;
    private Integer orderIndex;

    // ---- Pedido activo (null si la mesa esta libre) ----
    private Integer idOrder;
    private String orderCode;
    private LocalDateTime orderCreatedAt;
    private BigDecimal orderTotal;

    /** Meseros que intervinieron en la mesa, sin repetir y en orden de aparicion. */
    private List<String> waiters;

    private Integer totalItems;
    private Integer dispatchedItems;
    private Integer pendingItems;

    /** Envio de la comanda mas antigua: origen del cronometro de la mesa. */
    private LocalDateTime firstSentAt;

    /** Envio del item pendiente mas antiguo. Null cuando ya se despacho todo. */
    private LocalDateTime oldestPendingSentAt;

    /** Momento del ultimo despacho. Congela el cronometro cuando ya no hay pendientes. */
    private LocalDateTime lastReadyAt;

    /** true cuando la estacion despacho todos los items del pedido. */
    private boolean allDispatched;
}
