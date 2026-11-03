package com.carretero.service;

import com.carretero.dto.UserDTO;
import com.carretero.model.User;

public interface IUserService extends IGenericService<User, Integer> {

    /** Alta de una cuenta del personal desde la pantalla Usuarios (HU-16). */
    User createUser(UserDTO dto) throws Exception;

    /** Edicion: conserva lo que no viene y solo cambia la contrasena si se escribe una nueva. */
    User updateUser(Integer id, UserDTO dto) throws Exception;

    /** Baja o reactivacion logica de la cuenta (RN-04). */
    User setEnabled(Integer id, boolean enabled, String actorUsername) throws Exception;

    /** El propio colaborador cambia su contrasena (CP-16-4-1). */
    void changeOwnPassword(String username, String currentPassword, String newPassword) throws Exception;

    /** Regla minima de contrasena del sistema. */
    void validateNewPassword(String newPassword);
}
