package com.carretero.repository;

import com.carretero.model.Flavor;

import java.util.List;

public interface IFlavorRepository extends IGenericRepository<Flavor, Integer> {
    List<Flavor> findByCategoryIdCategoryAndActiveTrueOrderByOrderIndexAsc(Integer idCategory);
    List<Flavor> findByProductIdProductAndActiveTrueOrderByOrderIndexAsc(Integer idProduct);

    /** Solo las de la categoria (sin producto), para la pantalla de administracion. */
    List<Flavor> findByCategoryIdCategoryAndProductIsNullOrderByOrderIndexAsc(Integer idCategory);
    List<Flavor> findByProductIdProductOrderByOrderIndexAsc(Integer idProduct);
}
