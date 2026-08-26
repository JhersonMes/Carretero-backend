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
@Table(name = "payment_methods")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer idPaymentMethod;

    @Column(nullable = false, length = 50, unique = true)
    private String name; // EFECTIVO, YAPE, PLIN, TARJETA, TRANSFERENCIA

    @Column(length = 20)
    private String code;

    @Column(length = 150)
    private String description;

    @Column(nullable = false)
    private boolean active = true;
}
