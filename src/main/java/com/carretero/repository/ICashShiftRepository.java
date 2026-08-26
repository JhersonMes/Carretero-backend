package com.carretero.repository;

import com.carretero.model.CashShift;
import com.carretero.model.enums.CashShiftStatus;

import java.util.List;
import java.util.Optional;

public interface ICashShiftRepository extends IGenericRepository<CashShift, Integer> {
    Optional<CashShift> findFirstByStatusOrderByOpenTimeDesc(CashShiftStatus status);
    Optional<CashShift> findFirstByUserIdUserAndStatusOrderByOpenTimeDesc(Integer idUser, CashShiftStatus status);
    List<CashShift> findByOrderByOpenTimeDesc();
}
