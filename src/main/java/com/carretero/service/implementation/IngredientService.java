package com.carretero.service.implementation;

import com.carretero.model.Ingredient;
import com.carretero.repository.IGenericRepository;
import com.carretero.repository.IIngredientRepository;
import com.carretero.service.IIngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IngredientService extends GenericService<Ingredient, Integer> implements IIngredientService {

    private final IIngredientRepository repo;

    @Override
    protected IGenericRepository<Ingredient, Integer> getRepo() {
        return repo;
    }
}
