package com.carretero.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlavorDTO {

    private Integer idFlavor;
    private String name;
    /** Se llena cuando la opcion es de toda una categoria. */
    private Integer idCategory;
    /** Se llena cuando la opcion es propia de un solo producto. */
    private Integer idProduct;
    private BigDecimal priceDelta;
    private Integer orderIndex;
    private boolean active;
}
