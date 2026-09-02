package com.carretero.service.implementation;

import com.carretero.model.BusinessConfig;
import com.carretero.repository.IBusinessConfigRepository;
import com.carretero.repository.IGenericRepository;
import com.carretero.service.IBusinessConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessConfigService extends GenericService<BusinessConfig, Integer> implements IBusinessConfigService {

    private final IBusinessConfigRepository repo;
    private final PasswordEncoder passwordEncoder;

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
        // El PIN no viaja en el DTO, asi que llega vacio en cada guardado. Sin
        // arrastrar el que ya estaba, editar los datos del negocio lo borraria y
        // nadie podria anular una venta.
        if (config.getAdminPin() == null || config.getAdminPin().isBlank()) {
            config.setAdminPin(existing.getAdminPin());
        }
        return repo.save(config);
    }

    /**
     * Comprueba el PIN que autoriza anular una venta.
     *
     * Se valida aqui y no en el navegador: el PIN es lo unico que separa a un
     * mesero de poder borrar una venta cobrada, y una comprobacion en el cliente
     * se saltea con las herramientas del navegador.
     */
    @Override
    public boolean matchesAdminPin(String pin) {
        if (pin == null || pin.isBlank()) return false;
        String stored = getConfig().getAdminPin();
        if (stored == null || stored.isBlank()) return false;
        return passwordEncoder.matches(pin.trim(), stored);
    }

    @Override
    @Transactional
    public void changeAdminPin(String newPin) throws Exception {
        // Exactamente 4: es el largo que acepta el teclado de la pantalla, y un
        // PIN mas largo quedaria guardado sin poder teclearse.
        if (newPin == null || !newPin.trim().matches("\\d{4}")) {
            throw new IllegalArgumentException("El PIN debe tener 4 digitos.");
        }
        BusinessConfig config = getConfig();
        config.setAdminPin(passwordEncoder.encode(newPin.trim()));
        repo.save(config);
    }
}
