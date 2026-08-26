package com.carretero.service;

import com.carretero.dto.PaymentCreateRequestDTO;
import com.carretero.model.Payment;
import com.carretero.model.User;

import java.util.List;

public interface IPaymentService extends IGenericService<Payment, Integer> {
    List<Payment> registerPayments(PaymentCreateRequestDTO request, User user) throws Exception;
    List<Payment> findByOrderId(Integer idOrder);
    List<Payment> findByCashShiftId(Integer idCashShift);
}
