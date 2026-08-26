package com.carretero.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Integer idUser;

    @NotBlank(message = "El nombre de usuario es requerido")
    @Size(min = 2, max = 50, message = "El nombre de usuario debe tener entre 2 y 50 caracteres")
    private String username;

    @NotBlank(message = "El email es requerido")
    @Email(message = "El email debe ser valido")
    @Size(min = 2, max = 100, message = "El email debe tener entre 2 y 100 caracteres")
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String fullName;

    private String phone;

    private Boolean enabled = true;

    private RoleDTO role;
}
