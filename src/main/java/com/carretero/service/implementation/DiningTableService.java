package com.carretero.service.implementation;

import com.carretero.exception.ModelNotFoundException;
import com.carretero.model.DiningTable;
import com.carretero.model.enums.TableStatus;
import com.carretero.repository.IDiningTableRepository;
import com.carretero.repository.IGenericRepository;
import com.carretero.service.IDiningTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiningTableService extends GenericService<DiningTable, Integer> implements IDiningTableService {

    private final IDiningTableRepository repo;

    @Override
    protected IGenericRepository<DiningTable, Integer> getRepo() {
        return repo;
    }

    @Override
    public List<DiningTable> findActive() {
        return repo.findByActiveTrue();
    }

    @Override
    public List<DiningTable> findByStatus(TableStatus status) {
        return repo.findByStatusAndActiveTrue(status);
    }

    @Override
    @Transactional
    public DiningTable updateStatus(Integer idTable, TableStatus status) throws Exception {
        DiningTable table = repo.findById(idTable).orElseThrow(() -> new ModelNotFoundException("Mesa no encontrada: " + idTable));
        table.setStatus(status);
        return repo.save(table);
    }
}
