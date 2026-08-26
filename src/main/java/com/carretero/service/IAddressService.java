package com.carretero.service;

import com.carretero.model.Address;

import java.util.List;

public interface IAddressService extends IGenericService<Address, Integer> {
    List<Address> findByClientId(Integer idClient);
}
