package com.carretero.model;

import com.carretero.model.enums.InvoiceType;
import com.carretero.model.enums.SunatStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invoices", indexes = {
        @Index(name = "idx_invoice_series_correlative", columnList = "series, correlative_number", unique = true),
        @Index(name = "idx_invoice_type", columnList = "invoice_type"),
        @Index(name = "idx_invoice_sunat_status", columnList = "sunat_status"),
        @Index(name = "idx_invoice_issue_date", columnList = "issue_date")
})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer idInvoice;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_order", nullable = false,
            foreignKey = @ForeignKey(name = "FK_INVOICE_ORDER"))
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_client",
            foreignKey = @ForeignKey(name = "FK_INVOICE_CLIENT"))
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false, length = 20)
    private InvoiceType invoiceType = InvoiceType.BOLETA;

    @Column(nullable = false, length = 10)
    private String series; // ej. B001, F001, NV01

    @Column(name = "correlative_number", nullable = false)
    private Integer correlativeNumber; // ej. 1, 2, 3...

    @Column(name = "full_number", length = 30)
    private String fullNumber; // ej. "B001-00000001"

    @Column(name = "issue_date", nullable = false)
    private LocalDateTime issueDate;

    @Column(name = "taxable_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxableAmount; // Op. Gravadas

    @Column(name = "igv_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal igvAmount; // 18% IGV

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "sunat_status", nullable = false, length = 30)
    private SunatStatus sunatStatus = SunatStatus.NO_ENVIADO;

    @Column(name = "sunat_response_code", length = 20)
    private String sunatResponseCode;

    @Column(name = "sunat_description", length = 500)
    private String sunatDescription;

    @Column(name = "cdr_hash", length = 100)
    private String cdrHash;

    @Column(name = "pdf_url", length = 300)
    private String pdfUrl;

    @Column(name = "xml_url", length = 300)
    private String xmlUrl;

    @Column(name = "qr_data", length = 500)
    private String qrData;

    @PrePersist
    public void prePersist() {
        if (this.issueDate == null) {
            this.issueDate = LocalDateTime.now();
        }
        if (this.fullNumber == null && this.series != null && this.correlativeNumber != null) {
            this.fullNumber = String.format("%s-%08d", this.series, this.correlativeNumber);
        }
    }
}
