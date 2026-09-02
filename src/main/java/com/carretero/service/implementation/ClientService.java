package com.carretero.service.implementation;

import com.carretero.exception.ModelNotFoundException;
import com.carretero.model.Client;
import com.carretero.repository.IClientRepository;
import com.carretero.repository.IGenericRepository;
import com.carretero.service.IClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientService extends GenericService<Client, Integer> implements IClientService {

    private final IClientRepository repo;

    @Override
    protected IGenericRepository<Client, Integer> getRepo() {
        return repo;
    }

    @Override
    @Transactional
    public Client save(Client client) throws Exception {
        ensureDocNumberIsFree(client.getDocNumber(), null);
        return repo.save(client);
    }

    /**
     * Edita los datos del cliente sobre la fila ya guardada.
     *
     * No se puede guardar el objeto que llega del controlador: viene sin sus
     * direcciones y, como la relacion tiene orphanRemoval, guardarlo asi le
     * borraria al cliente todas las direcciones que tenia. Las direcciones se
     * administran por su propio endpoint.
     */
    @Override
    @Transactional
    public Client update(Client client, Integer id) throws Exception {
        Client existing = repo.findById(id)
                .orElseThrow(() -> new ModelNotFoundException("Cliente no encontrado: " + id));

        ensureDocNumberIsFree(client.getDocNumber(), id);

        existing.setDocType(client.getDocType());
        existing.setDocNumber(client.getDocNumber());
        existing.setName(client.getName());
        existing.setPhone(client.getPhone());
        existing.setEmail(client.getEmail());
        existing.setActive(client.isActive());

        return repo.save(existing);
    }

    /**
     * Un documento identifica a una sola persona. Si ya hay una ficha con ese DNI
     * o RUC se rechaza el alta: dos fichas para el mismo cliente parten su
     * historial en dos y hacen que la caja no lo encuentre al buscarlo.
     *
     * La restriccion tambien esta en la base, que es la que aguanta dos altas
     * simultaneas; esta comprobacion existe para poder decir de quien es el
     * documento en vez de devolver un error de integridad.
     */
    private void ensureDocNumberIsFree(String docNumber, Integer idToIgnore) {
        if (docNumber == null || docNumber.isBlank()) return;

        repo.findAllByDocNumber(docNumber.trim()).stream()
                .filter(other -> idToIgnore == null || !idToIgnore.equals(other.getIdClient()))
                .findFirst()
                .ifPresent(other -> {
                    throw new IllegalStateException(
                            "Ya hay un cliente registrado con el documento " + docNumber.trim()
                                    + ": " + other.getName());
                });
    }

    @Override
    public Optional<Client> findByDocNumber(String docNumber) {
        return repo.findAllByDocNumber(docNumber).stream().findFirst();
    }

    @Override
    public Optional<Client> findByPhone(String phone) {
        return repo.findByPhone(phone);
    }

    @Override
    public List<Client> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return repo.findAll();
        }
        return repo.searchClients(query.trim());
    }
}
