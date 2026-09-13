package com.carretero.service.implementation;

import com.carretero.exception.ModelNotFoundException;
import com.carretero.model.Category;
import com.carretero.model.Product;
import com.carretero.model.enums.KitchenStation;
import com.carretero.repository.ICategoryRepository;
import com.carretero.repository.IGenericRepository;
import com.carretero.repository.IProductRepository;
import com.carretero.service.ICategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService extends GenericService<Category, Integer> implements ICategoryService {

    private final ICategoryRepository repo;
    private final IProductRepository productRepo;

    @Override
    protected IGenericRepository<Category, Integer> getRepo() {
        return repo;
    }

    @Override
    public List<Category> findActiveOrdered() {
        return repo.findByActiveTrueOrderByOrderIndexAsc();
    }

    /**
     * Corrige el area de una categoria mal clasificada.
     *
     * Arrastra a sus productos porque la comanda se enruta por el area del
     * producto y no por la de su categoria: cambiar solo la categoria dejaria
     * los pedidos saliendo a la estacion equivocada, que es justo lo que se
     * queria arreglar. Los productos que alguien haya movido a mano a otra area
     * se respetan: solo se mueven los que seguian en el area vieja.
     */
    @Override
    @Transactional
    public Category changeStation(Integer idCategory, KitchenStation station, boolean moveProducts) throws Exception {
        Category category = repo.findById(idCategory)
                .orElseThrow(() -> new ModelNotFoundException("Categoria no encontrada: " + idCategory));

        KitchenStation previous = category.getStation();
        category.setStation(station);
        Category saved = repo.save(category);

        if (moveProducts) {
            List<Product> toMove = productRepo.findByCategoryIdCategory(idCategory).stream()
                    .filter(p -> p.getStation() == null || p.getStation() == previous)
                    .peek(p -> p.setStation(station))
                    .toList();
            if (!toMove.isEmpty()) {
                productRepo.saveAll(toMove);
            }
        }

        return saved;
    }
}
