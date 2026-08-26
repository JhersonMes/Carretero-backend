package com.carretero.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "addresses")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer idAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_client", nullable = false,
            foreignKey = @ForeignKey(name = "FK_ADDRESS_CLIENT"))
    private Client client;

    @Column(nullable = false, length = 150)
    private String street;

    @Column(length = 20)
    private String number;

    @Column(length = 200)
    private String reference;

    @Column(length = 100)
    private String district;

    @Column(nullable = false)
    private boolean favorite = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
