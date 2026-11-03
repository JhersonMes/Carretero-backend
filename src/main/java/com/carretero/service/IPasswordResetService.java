package com.carretero.service;

public interface IPasswordResetService {

    /**
     * Si el correo pertenece a una cuenta activa, genera un enlace de un solo uso y lo
     * envia. Si no, no hace nada: la respuesta es la misma para no revelar que correos
     * estan registrados.
     */
    void requestReset(String email);

    /** Cambia la contrasena con el enlace recibido por correo. */
    void resetPassword(String token, String newPassword) throws Exception;
}
