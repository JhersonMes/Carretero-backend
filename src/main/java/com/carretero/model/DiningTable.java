package com.carretero.model;

import com.carretero.model.enums.TableStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dining_tables")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DiningTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer idTable;

    @Column(nullable = false, length = 50, unique = true)
    private String name; // ej. "Mesa 1", "Mesa 2", "Barra 1"

    @Column(nullable = false)
    private Integer capacity = 4;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TableStatus status = TableStatus.LIBRE;

    /**
     * Posicion de la mesa en el plano del salon, definida por el administrador
     * arrastrando las mesas. Es nullable porque las mesas creadas antes de existir
     * esta columna quedan en null; DataInitializer les asigna un orden al arrancar.
     */
    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(nullable = false)
    private boolean active = true;
}
