package com.carretero.service;

import com.carretero.model.DiningTable;
import com.carretero.model.enums.TableStatus;

import java.util.List;

public interface IDiningTableService extends IGenericService<DiningTable, Integer> {
    List<DiningTable> findActive();
    List<DiningTable> findByStatus(TableStatus status);
    DiningTable updateStatus(Integer idTable, TableStatus status) throws Exception;

    /** Todas las mesas, activas e inactivas, en el orden del plano. */
    List<DiningTable> findAllForManagement();

    /** Registra una mesa nueva y la coloca al final del plano. */
    DiningTable createTable(String name, Integer capacity) throws Exception;

    /**
     * Cambia nombre y capacidad conservando el estado de servicio y la posicion
     * en el plano, que no se editan desde el formulario.
     */
    DiningTable renameTable(Integer idTable, String name, Integer capacity) throws Exception;

    /** Baja logica: la mesa sale del salon pero conserva su historial de ventas. */
    DiningTable deactivate(Integer idTable) throws Exception;

    /** Vuelve a poner en servicio una mesa dada de baja. */
    DiningTable activate(Integer idTable) throws Exception;
}
