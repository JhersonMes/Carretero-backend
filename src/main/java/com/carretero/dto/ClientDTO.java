package com.carretero.dto;

import com.carretero.model.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientDTO {

    private Integer idClient;

    @NotNull(message = "El tipo de documento es obligatorio")
    private DocumentType docType = DocumentType.DNI;

    @Size(max = 20, message = "El número de documento no debe superar 20 caracteres")
    private String docNumber;

    @NotBlank(message = "El nombre o razón social es obligatorio")
    @Size(max = 150, message = "El nombre no debe superar 150 caracteres")
    private String name;

    @Size(max = 20, message = "El teléfono no debe superar 20 caracteres")
    private String phone;

    private String email;

    private boolean active = true;

    private List<AddressDTO> addresses;

    private LocalDateTime createdAt;
}
