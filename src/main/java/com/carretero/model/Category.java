package com.carretero.model;

import com.carretero.model.enums.KitchenStation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "categories")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer idCategory;

    @Column(nullable = false, length = 80, unique = true)
    private String name;

    @Column(length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private KitchenStation station = KitchenStation.COCINA;

    @Column(name = "order_index")
    private Integer orderIndex = 0;

    @Column(nullable = false)
    private boolean active = true;
}
