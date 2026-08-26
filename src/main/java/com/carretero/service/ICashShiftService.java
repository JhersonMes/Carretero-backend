package com.carretero.service;

import com.carretero.dto.CashMovementDTO;
import com.carretero.model.CashMovement;
import com.carretero.model.CashShift;
import com.carretero.model.Payment;
import com.carretero.model.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ICashShiftService extends IGenericService<CashShift, Integer> {
    CashShift openShift(BigDecimal initialAmount, User user, String notes) throws Exception;
    Optional<CashShift> getActiveShift();
    CashShift closeShift(Integer idCashShift, BigDecimal actualCash, String notes) throws Exception;
    CashMovement registerMovement(CashMovementDTO dto, User user) throws Exception;
    List<CashMovement> getMovementsByShift(Integer idCashShift);
    void registerPaymentInShift(CashShift shift, Payment payment);
    CashShift recalculateShiftTotals(Integer idCashShift) throws Exception;
}
