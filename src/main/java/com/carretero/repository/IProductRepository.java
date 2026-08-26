package com.carretero.repository;

import com.carretero.model.Product;
import com.carretero.model.enums.KitchenStation;

import java.util.List;
import java.util.Optional;

public interface IProductRepository extends IGenericRepository<Product, Integer> {
    List<Product> findByCategoryIdCategoryAndActiveTrue(Integer idCategory);
    List<Product> findByCategoryIdCategory(Integer idCategory);
    List<Product> findByActiveTrue();
    List<Product> findByStation(KitchenStation station);
    Optional<Product> findByName(String name);
}
