package com.carretero.service.implementation;

import com.carretero.model.*;
import com.carretero.model.enums.KitchenStation;
import com.carretero.repository.IBusinessConfigRepository;
import com.carretero.repository.IInvoiceRepository;
import com.carretero.repository.IOrderRepository;
import com.carretero.service.IEscPosPrinterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EscPosPrinterService implements IEscPosPrinterService {

    private final IOrderRepository orderRepo;
    private final IInvoiceRepository invoiceRepo;
    private final IBusinessConfigRepository configRepo;

    // Constantes de comandos binarios ESC/POS estándar
    private static final byte[] ESC_INIT = new byte[]{0x1B, 0x40}; // ESC @
    private static final byte[] ALIGN_CENTER = new byte[]{0x1B, 0x61, 0x01}; // ESC a 1
    private static final byte[] ALIGN_LEFT = new byte[]{0x1B, 0x61, 0x00}; // ESC a 0
    private static final byte[] TEXT_BOLD_ON = new byte[]{0x1B, 0x45, 0x01}; // ESC E 1
    private static final byte[] TEXT_BOLD_OFF = new byte[]{0x1B, 0x45, 0x00}; // ESC E 0
    private static final byte[] TEXT_DOUBLE_SIZE = new byte[]{0x1D, 0x21, 0x11}; // GS ! 0x11
    private static final byte[] TEXT_NORMAL_SIZE = new byte[]{0x1D, 0x21, 0x00}; // GS ! 0x00
    private static final byte[] PAPER_CUT = new byte[]{0x1D, 0x56, 0x42, 0x00}; // GS V 66 0
    private static final byte[] LINE_FEED = new byte[]{0x0A};

    @Override
    public boolean printKitchenComanda(Integer idOrder) {
        Optional<Order> orderOpt = orderRepo.findById(idOrder);
        if (orderOpt.isEmpty()) return false;
        Order order = orderOpt.get();

        List<OrderDetail> kitchenItems = order.getDetails().stream()
                .filter(d -> d.getProduct() != null &&
                        (d.getProduct().getCategory() == null ||
                         d.getProduct().getCategory().getStation() == KitchenStation.COCINA ||
                         d.getProduct().getCategory().getStation() == KitchenStation.PARRILLA))
                .toList();

        if (kitchenItems.isEmpty()) return true;

        BusinessConfig config = configRepo.findFirstByActiveTrue().orElseGet(BusinessConfig::new);
        if (!config.isKitchenPrinterEnabled()) {
            log.info("Impresora de cocina deshabilitada en la configuración.");
            return true;
        }

        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            buffer.write(ESC_INIT);
            buffer.write(ALIGN_CENTER);
            buffer.write(TEXT_DOUBLE_SIZE);
            buffer.write(TEXT_BOLD_ON);
            buffer.write("*** COCINA / PARRILLA ***\n".getBytes(StandardCharsets.UTF_8));
            buffer.write(TEXT_NORMAL_SIZE);
            buffer.write(TEXT_BOLD_OFF);

            String mesaText = (order.getTable() != null) ? order.getTable().getName() : order.getSaleType().name();
            buffer.write(("MESA/TIPO: " + mesaText + "\n").getBytes(StandardCharsets.UTF_8));
            buffer.write(("PEDIDO: " + order.getOrderCode() + "\n").getBytes(StandardCharsets.UTF_8));
            buffer.write(("HORA: " + order.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "\n").getBytes(StandardCharsets.UTF_8));
            buffer.write("------------------------------------------\n".getBytes(StandardCharsets.UTF_8));

            buffer.write(ALIGN_LEFT);
            for (OrderDetail d : kitchenItems) {
                buffer.write(TEXT_BOLD_ON);
                buffer.write(String.format("[ %d ] %s\n", d.getQuantity(), d.getProductName()).getBytes(StandardCharsets.UTF_8));
                buffer.write(TEXT_BOLD_OFF);
                if (d.getNotes() != null && !d.getNotes().trim().isEmpty()) {
                    buffer.write(("   >> NOTA: " + d.getNotes() + "\n").getBytes(StandardCharsets.UTF_8));
                }
            }

            if (order.getNotes() != null && !order.getNotes().trim().isEmpty()) {
                buffer.write("------------------------------------------\n".getBytes(StandardCharsets.UTF_8));
                buffer.write(("OBS. GENERAL: " + order.getNotes() + "\n").getBytes(StandardCharsets.UTF_8));
            }

            buffer.write(LINE_FEED);
            buffer.write(LINE_FEED);
            buffer.write(PAPER_CUT);

            return sendToPrinter(config.getKitchenPrinterIp(), config.getKitchenPrinterPort(), buffer.toByteArray());
        } catch (Exception e) {
            log.error("Error al preparar comanda de cocina para pedido {}: {}", order.getOrderCode(), e.getMessage());
            return false;
        }
    }

    @Override
    public boolean printBarComanda(Integer idOrder) {
        Optional<Order> orderOpt = orderRepo.findById(idOrder);
        if (orderOpt.isEmpty()) return false;
        Order order = orderOpt.get();

        List<OrderDetail> barItems = order.getDetails().stream()
                .filter(d -> d.getProduct() != null &&
                        d.getProduct().getCategory() != null &&
                        (d.getProduct().getCategory().getStation() == KitchenStation.BEBIDAS ||
                         d.getProduct().getCategory().getStation() == KitchenStation.BARRA))
                .toList();

        if (barItems.isEmpty()) return true;

        BusinessConfig config = configRepo.findFirstByActiveTrue().orElseGet(BusinessConfig::new);
        if (!config.isBarPrinterEnabled()) {
            log.info("Impresora de bar/bebidas deshabilitada en la configuración.");
            return true;
        }

        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            buffer.write(ESC_INIT);
            buffer.write(ALIGN_CENTER);
            buffer.write(TEXT_DOUBLE_SIZE);
            buffer.write(TEXT_BOLD_ON);
            buffer.write("*** BARRA / BEBIDAS ***\n".getBytes(StandardCharsets.UTF_8));
            buffer.write(TEXT_NORMAL_SIZE);
            buffer.write(TEXT_BOLD_OFF);

            String mesaText = (order.getTable() != null) ? order.getTable().getName() : order.getSaleType().name();
            buffer.write(("MESA/TIPO: " + mesaText + "\n").getBytes(StandardCharsets.UTF_8));
            buffer.write(("PEDIDO: " + order.getOrderCode() + "\n").getBytes(StandardCharsets.UTF_8));
            buffer.write("------------------------------------------\n".getBytes(StandardCharsets.UTF_8));

            buffer.write(ALIGN_LEFT);
            for (OrderDetail d : barItems) {
                buffer.write(TEXT_BOLD_ON);
                buffer.write(String.format("[ %d ] %s\n", d.getQuantity(), d.getProductName()).getBytes(StandardCharsets.UTF_8));
                buffer.write(TEXT_BOLD_OFF);
            }

            buffer.write(LINE_FEED);
            buffer.write(LINE_FEED);
            buffer.write(PAPER_CUT);

            return sendToPrinter(config.getBarPrinterIp(), config.getBarPrinterPort(), buffer.toByteArray());
        } catch (Exception e) {
            log.error("Error al preparar comanda de bar: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean printPreAccount(Integer idOrder) {
        Optional<Order> orderOpt = orderRepo.findById(idOrder);
        if (orderOpt.isEmpty()) return false;
        Order order = orderOpt.get();

        BusinessConfig config = configRepo.findFirstByActiveTrue().orElseGet(BusinessConfig::new);
        if (!config.isCashierPrinterEnabled()) return true;

        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            buffer.write(ESC_INIT);
            buffer.write(ALIGN_CENTER);
            buffer.write(TEXT_BOLD_ON);
            buffer.write((config.getBusinessName() + "\n").getBytes(StandardCharsets.UTF_8));
            buffer.write("--- PRE-CUENTA ---\n".getBytes(StandardCharsets.UTF_8));
            buffer.write(TEXT_BOLD_OFF);

            String mesaText = (order.getTable() != null) ? order.getTable().getName() : order.getSaleType().name();
            buffer.write(("Mesa: " + mesaText + "\n").getBytes(StandardCharsets.UTF_8));
            buffer.write(("Pedido: " + order.getOrderCode() + "\n").getBytes(StandardCharsets.UTF_8));
            buffer.write("------------------------------------------\n".getBytes(StandardCharsets.UTF_8));

            buffer.write(ALIGN_LEFT);
            for (OrderDetail d : order.getDetails()) {
                buffer.write(String.format("%d x %-24s S/ %6.2f\n",
                        d.getQuantity(),
                        d.getProductName().length() > 24 ? d.getProductName().substring(0, 24) : d.getProductName(),
                        d.getSubtotal()).getBytes(StandardCharsets.UTF_8));
            }

            buffer.write("------------------------------------------\n".getBytes(StandardCharsets.UTF_8));
            buffer.write(ALIGN_LEFT);
            buffer.write(String.format("SUBTOTAL:                     S/ %7.2f\n", order.getSubtotal()).getBytes(StandardCharsets.UTF_8));
            if (order.getDeliveryFee().compareTo(java.math.BigDecimal.ZERO) > 0) {
                buffer.write(String.format("DELIVERY:                     S/ %7.2f\n", order.getDeliveryFee()).getBytes(StandardCharsets.UTF_8));
            }
            if (order.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                buffer.write(String.format("DESCUENTO:                   -S/ %7.2f\n", order.getDiscount()).getBytes(StandardCharsets.UTF_8));
            }
            buffer.write(TEXT_BOLD_ON);
            buffer.write(TEXT_DOUBLE_SIZE);
            buffer.write(String.format("TOTAL: S/ %7.2f\n", order.getTotal()).getBytes(StandardCharsets.UTF_8));
            buffer.write(TEXT_NORMAL_SIZE);
            buffer.write(TEXT_BOLD_OFF);

            buffer.write(LINE_FEED);
            buffer.write(LINE_FEED);
            buffer.write(PAPER_CUT);

            return sendToPrinter(config.getCashierPrinterIp(), config.getCashierPrinterPort(), buffer.toByteArray());
        } catch (Exception e) {
            log.error("Error al preparar pre-cuenta: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean printInvoice(Integer idInvoice) {
        Optional<Invoice> invOpt = invoiceRepo.findById(idInvoice);
        if (invOpt.isEmpty()) return false;
        Invoice invoice = invOpt.get();

        BusinessConfig config = configRepo.findFirstByActiveTrue().orElseGet(BusinessConfig::new);
        if (!config.isCashierPrinterEnabled()) return true;

        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            buffer.write(ESC_INIT);
            buffer.write(ALIGN_CENTER);
            buffer.write(TEXT_BOLD_ON);
            buffer.write((config.getBusinessName() + "\n").getBytes(StandardCharsets.UTF_8));
            buffer.write((config.getCommercialName() + "\n").getBytes(StandardCharsets.UTF_8));
            buffer.write(("RUC: " + config.getRuc() + "\n").getBytes(StandardCharsets.UTF_8));
            buffer.write((config.getAddress() + "\n").getBytes(StandardCharsets.UTF_8));
            buffer.write(TEXT_BOLD_OFF);
            buffer.write("------------------------------------------\n".getBytes(StandardCharsets.UTF_8));

            buffer.write(TEXT_BOLD_ON);
            buffer.write((invoice.getInvoiceType().name() + " ELECTRONICA\n").getBytes(StandardCharsets.UTF_8));
            buffer.write((invoice.getFullNumber() + "\n").getBytes(StandardCharsets.UTF_8));
            buffer.write(TEXT_BOLD_OFF);
            buffer.write(("Fecha: " + invoice.getIssueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "\n").getBytes(StandardCharsets.UTF_8));

            if (invoice.getClient() != null) {
                buffer.write(("Cliente: " + invoice.getClient().getName() + "\n").getBytes(StandardCharsets.UTF_8));
                if (invoice.getClient().getDocNumber() != null) {
                    buffer.write((invoice.getClient().getDocType() + ": " + invoice.getClient().getDocNumber() + "\n").getBytes(StandardCharsets.UTF_8));
                }
            } else {
                buffer.write("Cliente: CLIENTES VARIOS\n".getBytes(StandardCharsets.UTF_8));
            }

            buffer.write("------------------------------------------\n".getBytes(StandardCharsets.UTF_8));
            buffer.write(ALIGN_LEFT);

            Order order = invoice.getOrder();
            if (order != null && order.getDetails() != null) {
                for (OrderDetail d : order.getDetails()) {
                    buffer.write(String.format("%d x %-24s S/ %6.2f\n",
                            d.getQuantity(),
                            d.getProductName().length() > 24 ? d.getProductName().substring(0, 24) : d.getProductName(),
                            d.getSubtotal()).getBytes(StandardCharsets.UTF_8));
                }
            }

            buffer.write("------------------------------------------\n".getBytes(StandardCharsets.UTF_8));
            buffer.write(String.format("OP. GRAVADA:                  S/ %7.2f\n", invoice.getTaxableAmount()).getBytes(StandardCharsets.UTF_8));
            buffer.write(String.format("I.G.V. (18%%):                 S/ %7.2f\n", invoice.getIgvAmount()).getBytes(StandardCharsets.UTF_8));
            buffer.write(TEXT_BOLD_ON);
            buffer.write(String.format("IMPORTE TOTAL:                S/ %7.2f\n", invoice.getTotalAmount()).getBytes(StandardCharsets.UTF_8));
            buffer.write(TEXT_BOLD_OFF);
            buffer.write("------------------------------------------\n".getBytes(StandardCharsets.UTF_8));

            buffer.write(ALIGN_CENTER);
            buffer.write("Gracias por su preferencia!\n".getBytes(StandardCharsets.UTF_8));
            buffer.write("www.elcarretero.pe\n".getBytes(StandardCharsets.UTF_8));

            buffer.write(LINE_FEED);
            buffer.write(LINE_FEED);
            buffer.write(PAPER_CUT);

            return sendToPrinter(config.getCashierPrinterIp(), config.getCashierPrinterPort(), buffer.toByteArray());
        } catch (Exception e) {
            log.error("Error al imprimir comprobante {}: {}", invoice.getFullNumber(), e.getMessage());
            return false;
        }
    }

    @Override
    public boolean testPrinterConnection(String ip, int port) {
        if (ip == null || ip.trim().isEmpty() || port <= 0) return false;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip.trim(), port), 1500); // 1.5s timeout
            return socket.isConnected();
        } catch (Exception e) {
            log.warn("Ticketera en {}:{} no alcanzable: {}", ip, port, e.getMessage());
            return false;
        }
    }

    private boolean sendToPrinter(String ip, Integer port, byte[] data) {
        if (ip == null || ip.trim().isEmpty()) return false;
        int targetPort = (port != null && port > 0) ? port : 9100;

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip.trim(), targetPort), 2000);
            try (OutputStream out = socket.getOutputStream()) {
                out.write(data);
                out.flush();
            }
            log.info("Datos enviados exitosamente a ticketera en {}:{}", ip, targetPort);
            return true;
        } catch (Exception e) {
            log.warn("No se pudo conectar a la ticketera {}:{}. Error: {}", ip, targetPort, e.getMessage());
            return false;
        }
    }
}
