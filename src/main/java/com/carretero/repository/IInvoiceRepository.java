package com.carretero.repository;

import com.carretero.model.Invoice;
import com.carretero.model.enums.InvoiceType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IInvoiceRepository extends IGenericRepository<Invoice, Integer> {
    Optional<Invoice> findByOrderIdOrder(Integer idOrder);
    Optional<Invoice> findBySeriesAndCorrelativeNumber(String series, Integer correlativeNumber);
    List<Invoice> findByInvoiceType(InvoiceType invoiceType);

    @Query("SELECT MAX(i.correlativeNumber) FROM Invoice i WHERE i.series = :series")
    Integer findMaxCorrelativeBySeries(@Param("series") String series);
}
