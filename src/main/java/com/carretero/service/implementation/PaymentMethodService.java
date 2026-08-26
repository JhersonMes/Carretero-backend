package com.carretero.service.implementation;

import com.carretero.model.PaymentMethod;
import com.carretero.repository.IGenericRepository;
import com.carretero.repository.IPaymentMethodRepository;
import com.carretero.service.IPaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentMethodService extends GenericService<PaymentMethod, Integer> implements IPaymentMethodService {

    private final IPaymentMethodRepository repo;

    @Override
    protected IGenericRepository<PaymentMethod, Integer> getRepo() {
        return repo;
    }
}
