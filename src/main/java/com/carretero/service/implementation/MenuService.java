package com.carretero.service.implementation;

import com.carretero.model.Menu;
import com.carretero.repository.IGenericRepository;
import com.carretero.repository.IMenuRepository;
import com.carretero.service.IMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MenuService extends GenericService<Menu, Integer> implements IMenuService {

    private final IMenuRepository repo;

    @Override
    protected IGenericRepository<Menu, Integer> getRepo() {
        return repo;
    }
}
