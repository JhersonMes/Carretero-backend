package com.carretero.repository;

import com.carretero.model.PaymentMethod;

public interface IPaymentMethodRepository extends IGenericRepository<PaymentMethod, Integer> {
    PaymentMethod findOneByName(String name);
}
