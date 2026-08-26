package com.carretero.service;

import com.carretero.model.Category;

import java.util.List;

public interface ICategoryService extends IGenericService<Category, Integer> {
    List<Category> findActiveOrdered();
}
