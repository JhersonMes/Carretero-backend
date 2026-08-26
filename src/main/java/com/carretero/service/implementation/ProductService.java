package com.carretero.service.implementation;

import com.carretero.exception.ModelNotFoundException;
import com.carretero.model.PriceHistory;
import com.carretero.model.Product;
import com.carretero.model.User;
import com.carretero.model.enums.KitchenStation;
import com.carretero.repository.IGenericRepository;
import com.carretero.repository.IPriceHistoryRepository;
import com.carretero.repository.IProductRepository;
import com.carretero.service.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService extends GenericService<Product, Integer> implements IProductService {

    private final IProductRepository repo;
    private final IPriceHistoryRepository priceHistoryRepo;

    @Override
    protected IGenericRepository<Product, Integer> getRepo() {
        return repo;
    }

    @Override
    public List<Product> findByCategoryId(Integer idCategory) {
        return repo.findByCategoryIdCategoryAndActiveTrue(idCategory);
    }

    @Override
    public List<Product> findActive() {
        return repo.findByActiveTrue();
    }

    @Override
    public List<Product> findByStation(KitchenStation station) {
        return repo.findByStation(station);
    }

    @Override
    @Transactional
    public Product update(Product product, Integer id) throws Exception {
        Product current = repo.findById(id).orElseThrow(() -> new ModelNotFoundException("Producto no encontrado: " + id));
        
        // Si el precio cambió, guardar en el histórico
        if (current.getPrice() != null && product.getPrice() != null && current.getPrice().compareTo(product.getPrice()) != 0) {
            PriceHistory history = new PriceHistory();
            history.setProduct(current);
            history.setPreviousPrice(current.getPrice());
            history.setNewPrice(product.getPrice());
            priceHistoryRepo.save(history);
        }

        product.setIdProduct(id);
        return repo.save(product);
    }

    @Override
    @Transactional
    public Product updatePrice(Integer idProduct, BigDecimal newPrice, User user) throws Exception {
        Product product = repo.findById(idProduct).orElseThrow(() -> new ModelNotFoundException("Producto no encontrado: " + idProduct));
        
        PriceHistory history = new PriceHistory();
        history.setProduct(product);
        history.setPreviousPrice(product.getPrice());
        history.setNewPrice(newPrice);
        history.setUser(user);
        priceHistoryRepo.save(history);

        product.setPrice(newPrice);
        return repo.save(product);
    }

    @Override
    public List<PriceHistory> getPriceHistories(Integer idProduct) {
        return priceHistoryRepo.findByProductIdProductOrderByChangedAtDesc(idProduct);
    }
}
