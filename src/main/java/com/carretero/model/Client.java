package com.carretero.model;

import com.carretero.model.enums.DocumentType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "clients", indexes = {
        @Index(name = "idx_client_doc", columnList = "docNumber"),
        @Index(name = "idx_client_phone", columnList = "phone")
})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer idClient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentType docType = DocumentType.DNI;

    @Column(length = 20)
    private String docNumber;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(nullable = false)
    private boolean active = true;

    @JsonIgnore
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Address> addresses = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
