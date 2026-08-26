package com.carretero.dto;

import com.carretero.model.enums.DocumentType;
import com.carretero.model.enums.InvoiceType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceEmitRequestDTO {

    @NotNull(message = "El id de pedido es obligatorio")
    private Integer idOrder;

    @NotNull(message = "El tipo de comprobante es obligatorio (BOLETA, FACTURA, NOTA_VENTA)")
    private InvoiceType invoiceType;

    // Datos del cliente para la emisión
    private Integer idClient;
    private DocumentType docType;
    private String docNumber;
    private String clientName;
    private String clientAddress;
    private String clientEmail;
}
