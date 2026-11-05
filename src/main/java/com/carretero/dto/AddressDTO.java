package com.carretero.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressDTO {

    private Integer idAddress;

    private Integer idClient;

    @NotBlank(message = "La calle o jirón es obligatorio")
    @Size(max = 150, message = "La calle no debe superar 150 caracteres")
    private String street;

    @Size(max = 20, message = "El número no debe superar 20 caracteres")
    private String number;

    @Size(max = 200, message = "La referencia no debe superar 200 caracteres")
    private String reference;

    @Size(max = 100, message = "El distrito/sector no debe superar 100 caracteres")
    private String district;

    private boolean favorite = false;

    @DecimalMin(value = "0.00", message = "El costo de delivery no puede ser negativo")
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    private LocalDateTime createdAt;
}
