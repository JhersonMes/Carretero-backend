package com.carretero.repository;

import com.carretero.model.DiningTable;
import com.carretero.model.enums.TableStatus;

import java.util.List;
import java.util.Optional;

public interface IDiningTableRepository extends IGenericRepository<DiningTable, Integer> {
    Optional<DiningTable> findByName(String name);
    List<DiningTable> findByActiveTrue();
    List<DiningTable> findByStatusAndActiveTrue(TableStatus status);
}
