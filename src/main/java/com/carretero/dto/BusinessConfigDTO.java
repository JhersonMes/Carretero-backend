package com.carretero.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessConfigDTO {

    private Integer idConfig;

    @NotBlank(message = "El nombre del negocio es obligatorio")
    private String businessName;

    private String commercialName;
    private String ruc;
    private String address;
    private String phone;
    private String email;

    private String boletaSeries = "B001";
    private String facturaSeries = "F001";
    private String notaVentaSeries = "NV01";

    private String sunatApiUrl;
    private String sunatApiToken;
    private String dniRucApiUrl;
    private String dniRucApiToken;

    private String kitchenPrinterIp = "192.168.1.200";
    private Integer kitchenPrinterPort = 9100;
    private boolean kitchenPrinterEnabled = false;

    private String barPrinterIp = "192.168.1.201";
    private Integer barPrinterPort = 9100;
    private boolean barPrinterEnabled = false;

    private String cashierPrinterIp = "192.168.1.202";
    private Integer cashierPrinterPort = 9100;
    private boolean cashierPrinterEnabled = false;

    private boolean active = true;
}
