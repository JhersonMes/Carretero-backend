package com.carretero.repository;

import com.carretero.model.PriceHistory;

import java.util.List;

public interface IPriceHistoryRepository extends IGenericRepository<PriceHistory, Integer> {
    List<PriceHistory> findByProductIdProductOrderByChangedAtDesc(Integer idProduct);
}
