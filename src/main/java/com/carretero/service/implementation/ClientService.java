package com.carretero.service.implementation;

import com.carretero.model.Client;
import com.carretero.repository.IClientRepository;
import com.carretero.repository.IGenericRepository;
import com.carretero.service.IClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientService extends GenericService<Client, Integer> implements IClientService {

    private final IClientRepository repo;

    @Override
    protected IGenericRepository<Client, Integer> getRepo() {
        return repo;
    }

    @Override
    public Optional<Client> findByDocNumber(String docNumber) {
        return repo.findByDocNumber(docNumber);
    }

    @Override
    public Optional<Client> findByPhone(String phone) {
        return repo.findByPhone(phone);
    }

    @Override
    public List<Client> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return repo.findAll();
        }
        return repo.searchClients(query.trim());
    }
}
