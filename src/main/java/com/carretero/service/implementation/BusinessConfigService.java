package com.carretero.service.implementation;

import com.carretero.model.BusinessConfig;
import com.carretero.repository.IBusinessConfigRepository;
import com.carretero.repository.IGenericRepository;
import com.carretero.service.IBusinessConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessConfigService extends GenericService<BusinessConfig, Integer> implements IBusinessConfigService {

    private final IBusinessConfigRepository repo;

    @Override
    protected IGenericRepository<BusinessConfig, Integer> getRepo() {
        return repo;
    }

    @Override
    public BusinessConfig getConfig() {
        return repo.findFirstByActiveTrue().orElseGet(() -> {
            BusinessConfig newConfig = new BusinessConfig();
            return repo.save(newConfig);
        });
    }

    @Override
    @Transactional
    public BusinessConfig updateConfig(BusinessConfig config) throws Exception {
        BusinessConfig existing = getConfig();
        config.setIdConfig(existing.getIdConfig());
        return repo.save(config);
    }
}
