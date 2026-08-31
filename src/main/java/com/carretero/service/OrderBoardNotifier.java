package com.carretero.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Avisa por WebSocket que algo cambio en los pedidos abiertos.
 *
 * Lo escuchan tanto las pantallas de cocina como el tablero de Salon: un solo
 * evento evita que cada pantalla tenga que sondear al servidor. Esta centralizado
 * aqui para que el nombre del topic no quede repetido en cada punto que lo emite.
 */
@Component
@RequiredArgsConstructor
public class OrderBoardNotifier {

    public static final String TOPIC = "/topic/kitchen";

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyBoards() {
        messagingTemplate.convertAndSend(TOPIC, Map.of("event", "KITCHEN_UPDATED"));
    }
}
