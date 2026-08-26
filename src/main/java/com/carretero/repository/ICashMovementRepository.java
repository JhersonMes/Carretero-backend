package com.carretero.repository;

import com.carretero.model.CashMovement;

import java.util.List;

public interface ICashMovementRepository extends IGenericRepository<CashMovement, Integer> {
    List<CashMovement> findByCashShiftIdCashShift(Integer idCashShift);
}
