package com.carretero.repository;

import com.carretero.model.User;

public interface IUserRepository extends IGenericRepository<User, Integer> {
    User findOneByUsername(String username);
    User findOneByEmail(String email);
    User findOneByUsernameOrEmail(String username, String email);
}
