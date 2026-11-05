package com.carretero.repository;

import com.carretero.model.Invoice;
import com.carretero.model.enums.InvoiceType;
import com.carretero.model.enums.SunatStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IInvoiceRepository extends IGenericRepository<Invoice, Integer> {
    /**
     * Comprobantes vigentes del pedido (los que no estan anulados).
     *
     * Devuelve lista y no Optional porque un pedido puede acumular varios: al
     * reemitir, el anterior queda ANULADO y nace uno nuevo. Solo puede haber uno
     * vigente a la vez.
     */
    List<Invoice> findByOrderIdOrderAndSunatStatusNot(Integer idOrder, SunatStatus sunatStatus);

    /** Todos los comprobantes del pedido, anulados incluidos, del mas nuevo al mas viejo. */
    List<Invoice> findByOrderIdOrderOrderByIdInvoiceDesc(Integer idOrder);

    Optional<Invoice> findBySeriesAndCorrelativeNumber(String series, Integer correlativeNumber);
    List<Invoice> findByInvoiceType(InvoiceType invoiceType);

    @Query("SELECT MAX(i.correlativeNumber) FROM Invoice i WHERE i.series = :series")
    Integer findMaxCorrelativeBySeries(@Param("series") String series);
}
