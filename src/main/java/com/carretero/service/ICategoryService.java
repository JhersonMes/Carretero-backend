package com.carretero.service;

import com.carretero.model.Category;
import com.carretero.model.enums.KitchenStation;

import java.util.List;

public interface ICategoryService extends IGenericService<Category, Integer> {
    List<Category> findActiveOrdered();

    /**
     * Cambia el area de la categoria.
     *
     * @param moveProducts arrastra tambien a sus productos. Hace falta casi
     *        siempre: la comanda se enruta por el area del producto, no por la
     *        de su categoria, asi que mover solo la categoria no cambia a donde
     *        salen los pedidos que ya existen.
     */
    Category changeStation(Integer idCategory, KitchenStation station, boolean moveProducts) throws Exception;
}
