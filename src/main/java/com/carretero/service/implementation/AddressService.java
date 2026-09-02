package com.carretero.service.implementation;

import com.carretero.exception.ModelNotFoundException;
import com.carretero.model.Address;
import com.carretero.repository.IAddressRepository;
import com.carretero.repository.IGenericRepository;
import com.carretero.service.IAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService extends GenericService<Address, Integer> implements IAddressService {

    private final IAddressRepository repo;

    @Override
    protected IGenericRepository<Address, Integer> getRepo() {
        return repo;
    }

    @Override
    public List<Address> findByClientId(Integer idClient) {
        return repo.findByClientIdClient(idClient);
    }

    @Override
    @Transactional
    public Address save(Address address) throws Exception {
        Address saved = repo.save(address);
        clearOtherFavorites(saved);
        return saved;
    }

    /**
     * Edita la direccion sobre la fila ya guardada.
     *
     * La direccion que llega del controlador no trae a su cliente (el DTO no lo
     * lleva), y guardarla tal cual dejaria la fila sin id_client, que es una
     * columna obligatoria. Por eso solo se copian los campos editables.
     */
    @Override
    @Transactional
    public Address update(Address address, Integer id) throws Exception {
        Address existing = repo.findById(id)
                .orElseThrow(() -> new ModelNotFoundException("Direccion no encontrada: " + id));

        existing.setStreet(address.getStreet());
        existing.setNumber(address.getNumber());
        existing.setReference(address.getReference());
        existing.setDistrict(address.getDistrict());
        existing.setDeliveryFee(address.getDeliveryFee());
        existing.setFavorite(address.isFavorite());

        Address saved = repo.save(existing);
        clearOtherFavorites(saved);
        return saved;
    }

    /**
     * Un cliente puede tener varias direcciones, pero solo una es la habitual:
     * es la que la caja preselecciona al atenderlo. Marcar una nueva como
     * predeterminada desmarca la anterior.
     */
    private void clearOtherFavorites(Address favorite) {
        if (!favorite.isFavorite() || favorite.getClient() == null) return;

        List<Address> others = repo.findByClientIdClient(favorite.getClient().getIdClient()).stream()
                .filter(a -> a.isFavorite() && !a.getIdAddress().equals(favorite.getIdAddress()))
                .peek(a -> a.setFavorite(false))
                .toList();

        if (!others.isEmpty()) {
            repo.saveAll(others);
        }
    }
}
