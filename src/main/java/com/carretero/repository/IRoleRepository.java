package com.carretero.repository;

import com.carretero.model.Role;

public interface IRoleRepository extends IGenericRepository<Role, Integer> {
    Role findOneByName(String name);
}
