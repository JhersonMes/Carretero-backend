package com.carretero.repository;

import com.carretero.model.Address;

import java.util.List;

public interface IAddressRepository extends IGenericRepository<Address, Integer> {
    List<Address> findByClientIdClient(Integer idClient);
}
