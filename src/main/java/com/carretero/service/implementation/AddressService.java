package com.carretero.service.implementation;

import com.carretero.model.Address;
import com.carretero.repository.IAddressRepository;
import com.carretero.repository.IGenericRepository;
import com.carretero.service.IAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService extends GenericService<Address, Integer> implements IAddressService {

    private final IAddressRepository repo;

    @Override
    protected IGenericRepository<Address, Integer> getRepo() {
        return repo;
    }

    @Override
    public List<Address> findByClientId(Integer idClient) {
        return repo.findByClientIdClient(idClient);
    }
}
