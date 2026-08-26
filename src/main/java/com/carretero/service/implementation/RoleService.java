package com.carretero.service.implementation;

import com.carretero.model.Role;
import com.carretero.repository.IGenericRepository;
import com.carretero.repository.IRoleRepository;
import com.carretero.service.IRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleService extends GenericService<Role, Integer> implements IRoleService {

    private final IRoleRepository repo;

    @Override
    protected IGenericRepository<Role, Integer> getRepo() {
        return repo;
    }
}
