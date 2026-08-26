package com.carretero.dto;

import com.carretero.model.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DniRucQueryResponseDTO {

    private boolean success;
    private DocumentType docType;
    private String docNumber;
    private String name; // Nombre completo (persona natural) o Razón Social (empresa)
    private String address; // Dirección fiscal (si es RUC)
    private String status; // Estado contribuyente (ej. HABIDO / ACTIVO)
    private String message;
    private boolean fromCache; // Indica si vino de la BD local
}
