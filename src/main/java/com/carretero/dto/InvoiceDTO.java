package com.carretero.dto;

import com.carretero.model.enums.InvoiceType;
import com.carretero.model.enums.SunatStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDTO {

    private Integer idInvoice;
    private Integer idOrder;
    private String orderCode;
    private ClientDTO client;
    private InvoiceType invoiceType;
    private String series;
    private Integer correlativeNumber;
    private String fullNumber;
    private LocalDateTime issueDate;
    private BigDecimal taxableAmount;
    private BigDecimal igvAmount;
    private BigDecimal totalAmount;
    private SunatStatus sunatStatus;
    private String sunatResponseCode;
    private String sunatDescription;
    private String cdrHash;
    private String pdfUrl;
    private String xmlUrl;
    private String qrData;
}
