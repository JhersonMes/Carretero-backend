package com.carretero.controller;

import com.carretero.service.IEscPosPrinterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/printers")
@RequiredArgsConstructor
public class PrinterController {

    private final IEscPosPrinterService printerService;

    @PostMapping("/order/{idOrder}/kitchen")
    public ResponseEntity<Map<String, Object>> printKitchen(@PathVariable("idOrder") Integer idOrder) {
        boolean success = printerService.printKitchenComanda(idOrder);
        return ResponseEntity.ok(Map.of("success", success, "message", success ? "Comanda enviada a cocina" : "No se pudo conectar con la ticketera de cocina"));
    }

    @PostMapping("/order/{idOrder}/bar")
    public ResponseEntity<Map<String, Object>> printBar(@PathVariable("idOrder") Integer idOrder) {
        boolean success = printerService.printBarComanda(idOrder);
        return ResponseEntity.ok(Map.of("success", success, "message", success ? "Comanda enviada a barra" : "No se pudo conectar con la ticketera de barra"));
    }

    @PostMapping("/order/{idOrder}/pre-account")
    public ResponseEntity<Map<String, Object>> printPreAccount(@PathVariable("idOrder") Integer idOrder) {
        boolean success = printerService.printPreAccount(idOrder);
        return ResponseEntity.ok(Map.of("success", success, "message", success ? "Pre-cuenta enviada a caja" : "No se pudo conectar con la ticketera de caja"));
    }

    @PostMapping("/invoice/{idInvoice}")
    public ResponseEntity<Map<String, Object>> printInvoice(@PathVariable("idInvoice") Integer idInvoice) {
        boolean success = printerService.printInvoice(idInvoice);
        return ResponseEntity.ok(Map.of("success", success, "message", success ? "Comprobante impreso en caja" : "No se pudo conectar con la ticketera de caja"));
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testPrinter(@RequestBody Map<String, Object> body) {
        String ip = (String) body.get("ip");
        int port = body.containsKey("port") ? Integer.parseInt(body.get("port").toString()) : 9100;

        boolean reachable = printerService.testPrinterConnection(ip, port);
        return ResponseEntity.ok(Map.of(
                "reachable", reachable,
                "ip", ip,
                "port", port,
                "message", reachable ? "Conexión exitosa con la impresora térmica" : "No se pudo alcanzar la impresora en la dirección IP especificada"
        ));
    }
}
