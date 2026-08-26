package com.carretero.repository;

import com.carretero.model.BusinessConfig;

import java.util.Optional;

public interface IBusinessConfigRepository extends IGenericRepository<BusinessConfig, Integer> {
    Optional<BusinessConfig> findFirstByActiveTrue();
}
