package com.carretero.service.implementation;

import com.carretero.exception.ModelNotFoundException;
import com.carretero.model.DiningTable;
import com.carretero.model.enums.OrderStatus;
import com.carretero.model.enums.TableStatus;
import com.carretero.repository.IDiningTableRepository;
import com.carretero.repository.IGenericRepository;
import com.carretero.repository.IOrderRepository;
import com.carretero.service.IDiningTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DiningTableService extends GenericService<DiningTable, Integer> implements IDiningTableService {

    /** Estados que ya no ocupan la mesa. */
    private static final List<OrderStatus> CLOSED_STATUSES =
            List.of(OrderStatus.PAGADO, OrderStatus.CANCELADO);

    private final IDiningTableRepository repo;
    private final IOrderRepository orderRepo;

    @Override
    protected IGenericRepository<DiningTable, Integer> getRepo() {
        return repo;
    }

    @Override
    public List<DiningTable> findActive() {
        return repo.findActiveOrdered();
    }

    @Override
    public List<DiningTable> findAllForManagement() {
        return repo.findAllOrdered();
    }

    @Override
    public List<DiningTable> findByStatus(TableStatus status) {
        return repo.findByStatusAndActiveTrue(status);
    }

    @Override
    @Transactional
    public DiningTable updateStatus(Integer idTable, TableStatus status) throws Exception {
        DiningTable table = find(idTable);
        table.setStatus(status);
        return repo.save(table);
    }

    @Override
    @Transactional
    public DiningTable createTable(String name, Integer capacity) throws Exception {
        String cleanName = requireName(name, null);

        DiningTable table = new DiningTable();
        table.setName(cleanName);
        table.setCapacity(requireCapacity(capacity));
        table.setStatus(TableStatus.LIBRE);
        table.setActive(true);
        // La mesa nueva entra al final del plano; desde ahi el administrador la
        // arrastra a su sitio real en el local.
        table.setOrderIndex(nextOrderIndex());

        return repo.save(table);
    }

    @Override
    @Transactional
    public DiningTable renameTable(Integer idTable, String name, Integer capacity) throws Exception {
        DiningTable table = find(idTable);

        // Solo se tocan nombre y capacidad: el estado de servicio y la posicion en
        // el plano se manejan desde el salon y desde la pantalla de distribucion.
        table.setName(requireName(name, idTable));
        table.setCapacity(requireCapacity(capacity));

        return repo.save(table);
    }

    @Override
    @Transactional
    public DiningTable deactivate(Integer idTable) throws Exception {
        DiningTable table = find(idTable);

        if (hasOpenOrders(idTable)) {
            throw new IllegalStateException(
                    "La mesa " + table.getName() + " tiene un pedido abierto. Cierra la cuenta antes de darla de baja.");
        }

        // Baja logica y no borrado: la mesa aparece en pedidos y comprobantes ya
        // emitidos, y eliminarla dejaria ese historial sin referencia.
        table.setActive(false);
        table.setStatus(TableStatus.LIBRE);
        return repo.save(table);
    }

    @Override
    @Transactional
    public DiningTable activate(Integer idTable) throws Exception {
        DiningTable table = find(idTable);
        table.setActive(true);
        if (table.getOrderIndex() == null) {
            table.setOrderIndex(nextOrderIndex());
        }
        return repo.save(table);
    }

    // ---------------------------------------------------------------------------

    private DiningTable find(Integer idTable) {
        return repo.findById(idTable)
                .orElseThrow(() -> new ModelNotFoundException("Mesa no encontrada: " + idTable));
    }

    private boolean hasOpenOrders(Integer idTable) {
        return !orderRepo.findByTableIdTableAndStatusNotIn(idTable, CLOSED_STATUSES).isEmpty();
    }

    /**
     * El nombre es unico en la base. Se valida aqui para responder un 400 con un
     * mensaje entendible en vez del error de integridad crudo del driver.
     */
    private String requireName(String name, Integer idTableBeingEdited) {
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("El nombre de la mesa es obligatorio.");
        }
        String clean = name.trim();

        Optional<DiningTable> existing = repo.findByName(clean);
        if (existing.isPresent() && !Objects.equals(existing.get().getIdTable(), idTableBeingEdited)) {
            throw new IllegalStateException("Ya existe una mesa llamada \"" + clean + "\".");
        }
        return clean;
    }

    private Integer requireCapacity(Integer capacity) {
        if (capacity == null || capacity < 1) {
            throw new IllegalStateException("La capacidad de la mesa debe ser al menos 1 persona.");
        }
        return capacity;
    }

    private int nextOrderIndex() {
        return repo.findAllOrdered().stream()
                .map(DiningTable::getOrderIndex)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .map(max -> max + 1)
                .orElse(0);
    }
}
