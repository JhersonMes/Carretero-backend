package com.carretero;

import com.carretero.model.*;
import com.carretero.model.enums.KitchenStation;
import com.carretero.model.enums.OrderType;
import com.carretero.repository.IBusinessConfigRepository;
import com.carretero.repository.IDiningTableRepository;
import com.carretero.repository.IInvoiceRepository;
import com.carretero.repository.IOrderRepository;
import com.carretero.service.implementation.DiningTableService;
import com.carretero.service.implementation.EscPosPrinterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de la impresion en ticketera (HU-12) y de la reactivacion de mesas (HU-17).
 * La ticketera se simula con un ServerSocket local, igual que una impresora ESC/POS de red.
 */
@ExtendWith(MockitoExtension.class)
public class ImpresionYMesasTest {

    @Mock private IOrderRepository orderRepo;
    @Mock private IInvoiceRepository invoiceRepo;
    @Mock private IBusinessConfigRepository configRepo;
    @Mock private IDiningTableRepository tableRepo;

    @InjectMocks private EscPosPrinterService printerService;

    private Order pedidoMesa3() {
        Category alitas = new Category();
        alitas.setName("ALITAS DE POLLO");
        alitas.setStation(KitchenStation.COCINA);
        Product producto = new Product();
        producto.setName("Alitas De Pollo X 6");
        producto.setCategory(alitas);

        OrderDetail d = new OrderDetail();
        d.setProduct(producto);
        d.setProductName("Alitas De Pollo X 6");
        d.setQuantity(2);
        d.setFlavorName("BBQ");
        d.setNotes("sin aji");
        d.setStation(KitchenStation.COCINA);
        d.setSubtotal(new BigDecimal("49.00"));

        DiningTable mesa = new DiningTable();
        mesa.setName("Mesa 3");
        Order o = new Order();
        o.setIdOrder(1);
        o.setOrderCode("PED-20261030-0001");
        o.setSaleType(OrderType.SALON);
        o.setTable(mesa);
        o.setCreatedAt(java.time.LocalDateTime.now());
        o.setDetails(List.of(d));
        o.setSubtotal(new BigDecimal("49.00"));
        o.setTotal(new BigDecimal("49.00"));
        return o;
    }

    // CASO DE PRUEBA CP-12-1-1: la comanda impresa lleva producto, cantidad, sabor y nota
    @Test
    void imprimirComanda_llevaProductoCantidadSaborYNota() throws Exception {
        try (ServerSocket ticketera = new ServerSocket(0)) {
            // preparacion
            BusinessConfig config = new BusinessConfig();
            config.setKitchenPrinterIp("127.0.0.1");
            config.setKitchenPrinterPort(ticketera.getLocalPort());
            config.setKitchenPrinterEnabled(true);
            when(configRepo.findFirstByActiveTrue()).thenReturn(Optional.of(config));
            when(orderRepo.findById(1)).thenReturn(Optional.of(pedidoMesa3()));
            CompletableFuture<String> recibido = CompletableFuture.supplyAsync(() -> leer(ticketera));

            // ejecucion
            boolean ok = printerService.printKitchenComanda(1);

            // afirmacion
            String ticket = recibido.get(5, TimeUnit.SECONDS);
            assertTrue(ok);
            assertTrue(ticket.contains("[ 2 ] Alitas De Pollo X 6"));
            assertTrue(ticket.contains("Opcion: BBQ"));
            assertTrue(ticket.contains("NOTA: sin aji"));
            assertTrue(ticket.contains("Mesa 3"));
        }
    }

    // CASO DE PRUEBA CP-12-1-1: con la ticketera desactivada no se informa un falso exito
    @Test
    void imprimirComanda_ticketeraDesactivada_devuelveFalse() {
        BusinessConfig config = new BusinessConfig();
        config.setKitchenPrinterEnabled(false);
        when(configRepo.findFirstByActiveTrue()).thenReturn(Optional.of(config));
        when(orderRepo.findById(1)).thenReturn(Optional.of(pedidoMesa3()));

        assertFalse(printerService.printKitchenComanda(1));
    }

    // CASO DE PRUEBA CP-12-2-1: la pre-cuenta lleva los platos, el subtotal y el total
    @Test
    void imprimirPrecuenta_llevaPlatosSubtotalYTotal() throws Exception {
        try (ServerSocket ticketera = new ServerSocket(0)) {
            BusinessConfig config = new BusinessConfig();
            config.setCashierPrinterIp("127.0.0.1");
            config.setCashierPrinterPort(ticketera.getLocalPort());
            config.setCashierPrinterEnabled(true);
            when(configRepo.findFirstByActiveTrue()).thenReturn(Optional.of(config));
            when(orderRepo.findById(1)).thenReturn(Optional.of(pedidoMesa3()));
            CompletableFuture<String> recibido = CompletableFuture.supplyAsync(() -> leer(ticketera));

            assertTrue(printerService.printPreAccount(1));

            String ticket = recibido.get(5, TimeUnit.SECONDS);
            assertTrue(ticket.contains("PRE-CUENTA"));
            assertTrue(ticket.contains("2 x Alitas De Pollo X 6"));
            assertTrue(ticket.contains("SUBTOTAL"));
            assertTrue(ticket.contains("49.00"));
        }
    }

    // CASO DE PRUEBA CP-17-3-2: la mesa reactivada pasa al final del plano
    @Test
    void reactivarMesa_quedaAlFinalDelPlano() throws Exception {
        // preparacion: 9 mesas, Barra 2 estaba en la posicion 7
        DiningTableService tableService = new DiningTableService(tableRepo, orderRepo);
        DiningTable barra2 = new DiningTable();
        barra2.setIdTable(8);
        barra2.setName("Barra 2");
        barra2.setOrderIndex(7);
        barra2.setActive(false);
        DiningTable mesa7 = new DiningTable();
        mesa7.setIdTable(9);
        mesa7.setOrderIndex(8);
        when(tableRepo.findById(8)).thenReturn(Optional.of(barra2));
        when(tableRepo.findAllOrdered()).thenReturn(List.of(barra2, mesa7));
        when(tableRepo.save(any(DiningTable.class))).thenAnswer(i -> i.getArgument(0));

        // ejecucion
        DiningTable reactivada = tableService.activate(8);

        // afirmacion
        assertTrue(reactivada.isActive());
        assertEquals(9, reactivada.getOrderIndex());
    }

    private static String leer(ServerSocket server) {
        try (Socket s = server.accept(); InputStream in = s.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            in.transferTo(out);
            return out.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
