package com.carretero.service.implementation;

import com.carretero.dto.InvoiceEmitRequestDTO;
import com.carretero.exception.ModelNotFoundException;
import com.carretero.model.*;
import com.carretero.model.enums.DocumentType;
import com.carretero.model.enums.InvoiceType;
import com.carretero.repository.*;
import com.carretero.service.IInvoiceService;
import com.carretero.service.ISunatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InvoiceService extends GenericService<Invoice, Integer> implements IInvoiceService {

    private final IInvoiceRepository repo;
    private final IOrderRepository orderRepo;
    private final IClientRepository clientRepo;
    private final IBusinessConfigRepository configRepo;
    private final ISunatService sunatService;

    @Override
    protected IGenericRepository<Invoice, Integer> getRepo() {
        return repo;
    }

    @Override
    @Transactional
    public Invoice emitInvoice(InvoiceEmitRequestDTO request, User user) throws Exception {
        Order order = orderRepo.findById(request.getIdOrder())
                .orElseThrow(() -> new ModelNotFoundException("Pedido no encontrado: " + request.getIdOrder()));

        Optional<Invoice> existing = repo.findByOrderIdOrder(order.getIdOrder());
        if (existing.isPresent()) {
            throw new IllegalStateException("El pedido ya cuenta con un comprobante emitido: " + existing.get().getFullNumber());
        }

        BusinessConfig config = configRepo.findFirstByActiveTrue().orElseGet(BusinessConfig::new);

        // Determinar Serie según tipo de comprobante
        String series;
        if (request.getInvoiceType() == InvoiceType.FACTURA) {
            series = config.getFacturaSeries() != null ? config.getFacturaSeries() : "F001";
            if (request.getDocNumber() == null || request.getDocNumber().trim().length() != 11) {
                throw new IllegalArgumentException("Para emitir Factura se requiere un RUC válido de 11 dígitos.");
            }
        } else if (request.getInvoiceType() == InvoiceType.BOLETA) {
            series = config.getBoletaSeries() != null ? config.getBoletaSeries() : "B001";
        } else {
            series = config.getNotaVentaSeries() != null ? config.getNotaVentaSeries() : "NV01";
        }

        // Obtener siguiente correlativo
        Integer maxCorrelative = repo.findMaxCorrelativeBySeries(series);
        int nextCorrelative = (maxCorrelative != null) ? maxCorrelative + 1 : 1;

        // Gestionar cliente del comprobante
        Client client = null;
        if (request.getIdClient() != null) {
            client = clientRepo.findById(request.getIdClient()).orElse(null);
        }
        if (client == null && request.getDocNumber() != null && !request.getDocNumber().trim().isEmpty()) {
            String doc = request.getDocNumber().trim();
            client = clientRepo.findByDocNumber(doc).orElseGet(() -> {
                Client newC = new Client();
                newC.setDocType(request.getDocType() != null ? request.getDocType() : (doc.length() == 11 ? DocumentType.RUC : DocumentType.DNI));
                newC.setDocNumber(doc);
                newC.setName(request.getClientName() != null ? request.getClientName() : "CLIENTE " + doc);
                newC.setEmail(request.getClientEmail());
                return clientRepo.save(newC);
            });
        }

        // Cálculo tributario (18% IGV)
        BigDecimal total = order.getTotal();
        BigDecimal taxable = total.divide(BigDecimal.valueOf(1.18), 2, RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(taxable);

        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setClient(client);
        invoice.setInvoiceType(request.getInvoiceType());
        invoice.setSeries(series);
        invoice.setCorrelativeNumber(nextCorrelative);
        invoice.setFullNumber(String.format("%s-%08d", series, nextCorrelative));
        invoice.setIssueDate(LocalDateTime.now());
        invoice.setTaxableAmount(taxable);
        invoice.setIgvAmount(igv);
        invoice.setTotalAmount(total);

        // Envío o despacho electrónico (SUNAT / OSE)
        invoice = sunatService.dispatchToSunat(invoice);

        return repo.save(invoice);
    }

    @Override
    public Optional<Invoice> findByOrderId(Integer idOrder) {
        return repo.findByOrderIdOrder(idOrder);
    }

    @Override
    public List<Invoice> findByInvoiceType(InvoiceType invoiceType) {
        return repo.findByInvoiceType(invoiceType);
    }

    @Override
    public String getNextCorrelativeNumber(InvoiceType invoiceType) {
        BusinessConfig config = configRepo.findFirstByActiveTrue().orElseGet(BusinessConfig::new);
        String series;
        if (invoiceType == InvoiceType.FACTURA) series = config.getFacturaSeries();
        else if (invoiceType == InvoiceType.BOLETA) series = config.getBoletaSeries();
        else series = config.getNotaVentaSeries();

        Integer max = repo.findMaxCorrelativeBySeries(series);
        int next = (max != null) ? max + 1 : 1;
        return String.format("%s-%08d", series, next);
    }
}
