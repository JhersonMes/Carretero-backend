package com.carretero.repository;

import com.carretero.model.PriceHistory;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IPriceHistoryRepository extends IGenericRepository<PriceHistory, Integer> {
    List<PriceHistory> findByProductIdProductOrderByChangedAtDesc(Integer idProduct);

    /**
     * Historico con su producto y su usuario ya cargados. Sin el JOIN FETCH la respuesta
     * leia relaciones LAZY fuera de la sesion y fallaba con error 500 (defecto CP-15-3-1).
     */
    @Query("SELECT h FROM PriceHistory h JOIN FETCH h.product LEFT JOIN FETCH h.user " +
            "WHERE h.product.idProduct = :idProduct ORDER BY h.changedAt DESC, h.idPriceHistory DESC")
    List<PriceHistory> findHistoryWithDetails(@Param("idProduct") Integer idProduct);
}
