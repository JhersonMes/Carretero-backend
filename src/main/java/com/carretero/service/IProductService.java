package com.carretero.service;

import com.carretero.model.PriceHistory;
import com.carretero.model.Product;
import com.carretero.model.User;
import com.carretero.model.enums.KitchenStation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface IProductService extends IGenericService<Product, Integer> {
    List<Product> findByCategoryId(Integer idCategory);
    List<Product> findActive();
    List<Product> findByStation(KitchenStation station);
    Product updatePrice(Integer idProduct, BigDecimal newPrice, User user) throws Exception;
    List<PriceHistory> getPriceHistories(Integer idProduct);

    /** Unidades vendidas por producto: {idProduct -> unidades}. */
    Map<Integer, Long> getSoldUnitsByProduct();
}
