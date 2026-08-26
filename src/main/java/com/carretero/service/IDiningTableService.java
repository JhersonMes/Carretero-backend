package com.carretero.service;

import com.carretero.model.DiningTable;
import com.carretero.model.enums.TableStatus;

import java.util.List;

public interface IDiningTableService extends IGenericService<DiningTable, Integer> {
    List<DiningTable> findActive();
    List<DiningTable> findByStatus(TableStatus status);
    DiningTable updateStatus(Integer idTable, TableStatus status) throws Exception;
}
