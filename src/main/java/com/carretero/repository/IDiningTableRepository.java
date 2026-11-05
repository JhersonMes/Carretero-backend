package com.carretero.repository;

import com.carretero.model.DiningTable;
import com.carretero.model.enums.TableStatus;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface IDiningTableRepository extends IGenericRepository<DiningTable, Integer> {
    Optional<DiningTable> findByName(String name);
    List<DiningTable> findByActiveTrue();
    List<DiningTable> findByStatusAndActiveTrue(TableStatus status);

    /**
     * Mesas activas en el orden del plano del salon. Las que aun no tienen posicion
     * asignada (orderIndex null) se mandan al final en lugar de romper el orden.
     */
    @Query("SELECT t FROM DiningTable t WHERE t.active = true " +
            "ORDER BY COALESCE(t.orderIndex, 999999) ASC, t.idTable ASC")
    List<DiningTable> findActiveOrdered();

    @Query("SELECT t FROM DiningTable t ORDER BY COALESCE(t.orderIndex, 999999) ASC, t.idTable ASC")
    List<DiningTable> findAllOrdered();
}
