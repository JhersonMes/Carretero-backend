package com.carretero.service;

import com.carretero.model.Client;

import java.util.List;
import java.util.Optional;

public interface IClientService extends IGenericService<Client, Integer> {
    Optional<Client> findByDocNumber(String docNumber);
    Optional<Client> findByPhone(String phone);
    List<Client> search(String query);
}
