package com.carretero.service;

import com.carretero.dto.InvoiceEmitRequestDTO;
import com.carretero.model.Invoice;
import com.carretero.model.User;
import com.carretero.model.enums.InvoiceType;

import java.util.List;
import java.util.Optional;

public interface IInvoiceService extends IGenericService<Invoice, Integer> {
    Invoice emitInvoice(InvoiceEmitRequestDTO request, User user) throws Exception;
    Optional<Invoice> findByOrderId(Integer idOrder);

    /** Anula el comprobante vigente del pedido y emite uno nuevo con los datos corregidos. */
    Invoice reissueInvoice(InvoiceEmitRequestDTO request, String pin, User user) throws Exception;

    /** Todos los comprobantes del pedido, anulados incluidos. */
    List<Invoice> findAllByOrderId(Integer idOrder);
    List<Invoice> findByInvoiceType(InvoiceType invoiceType);
    String getNextCorrelativeNumber(InvoiceType invoiceType);
}
