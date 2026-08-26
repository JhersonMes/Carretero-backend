package com.carretero.service.implementation;

import com.carretero.model.Category;
import com.carretero.repository.ICategoryRepository;
import com.carretero.repository.IGenericRepository;
import com.carretero.service.ICategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService extends GenericService<Category, Integer> implements ICategoryService {

    private final ICategoryRepository repo;

    @Override
    protected IGenericRepository<Category, Integer> getRepo() {
        return repo;
    }

    @Override
    public List<Category> findActiveOrdered() {
        return repo.findByActiveTrueOrderByOrderIndexAsc();
    }
}
