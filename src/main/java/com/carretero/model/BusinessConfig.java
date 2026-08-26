package com.carretero.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "business_config")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BusinessConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer idConfig;

    @Column(nullable = false, length = 150)
    private String businessName = "EL CARRETERO";

    @Column(length = 150)
    private String commercialName = "EL CARRETERO BURGERS & WINGS";

    @Column(length = 20)
    private String ruc = "20000000001";

    @Column(length = 255)
    private String address = "Av. Principal 123";

    @Column(length = 30)
    private String phone = "999888777";

    @Column(length = 100)
    private String email;

    // Series de facturación
    @Column(length = 10)
    private String boletaSeries = "B001";

    @Column(length = 10)
    private String facturaSeries = "F001";

    @Column(length = 10)
    private String notaVentaSeries = "NV01";

    // Configuración API DNI / RUC y SUNAT
    @Column(length = 255)
    private String sunatApiUrl;

    @Column(length = 255)
    private String sunatApiToken;

    @Column(length = 255)
    private String dniRucApiUrl = "https://apiperu.dev/api";

    @Column(length = 255)
    private String dniRucApiToken;

    // Impresoras Térmicas de Red (Ethernet / WiFi)
    @Column(length = 50)
    private String kitchenPrinterIp = "192.168.1.200";

    @Column
    private Integer kitchenPrinterPort = 9100;

    @Column
    private boolean kitchenPrinterEnabled = false;

    @Column(length = 50)
    private String barPrinterIp = "192.168.1.201";

    @Column
    private Integer barPrinterPort = 9100;

    @Column
    private boolean barPrinterEnabled = false;

    @Column(length = 50)
    private String cashierPrinterIp = "192.168.1.202";

    @Column
    private Integer cashierPrinterPort = 9100;

    @Column
    private boolean cashierPrinterEnabled = false;

    @Column(nullable = false)
    private boolean active = true;
}
