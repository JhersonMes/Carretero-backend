package com.carretero.repository;

import com.carretero.model.Payment;

import java.util.List;

public interface IPaymentRepository extends IGenericRepository<Payment, Integer> {
    List<Payment> findByOrderIdOrder(Integer idOrder);
    List<Payment> findByCashShiftIdCashShift(Integer idCashShift);
}
