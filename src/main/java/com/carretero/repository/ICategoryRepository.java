package com.carretero.repository;

import com.carretero.model.Category;

import java.util.List;
import java.util.Optional;

public interface ICategoryRepository extends IGenericRepository<Category, Integer> {
    Optional<Category> findByName(String name);
    List<Category> findByActiveTrueOrderByOrderIndexAsc();
}
