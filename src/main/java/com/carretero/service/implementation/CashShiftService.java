package com.carretero.service.implementation;

import com.carretero.dto.CashMovementDTO;
import com.carretero.exception.ModelNotFoundException;
import com.carretero.model.CashMovement;
import com.carretero.model.CashShift;
import com.carretero.model.Payment;
import com.carretero.model.User;
import com.carretero.model.enums.CashMovementType;
import com.carretero.model.enums.CashShiftStatus;
import com.carretero.repository.ICashMovementRepository;
import com.carretero.repository.ICashShiftRepository;
import com.carretero.repository.IGenericRepository;
import com.carretero.repository.IPaymentRepository;
import com.carretero.service.ICashShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CashShiftService extends GenericService<CashShift, Integer> implements ICashShiftService {

    private final ICashShiftRepository repo;
    private final ICashMovementRepository movementRepo;
    private final IPaymentRepository paymentRepo;

    @Override
    protected IGenericRepository<CashShift, Integer> getRepo() {
        return repo;
    }

    @Override
    @Transactional
    public CashShift openShift(BigDecimal initialAmount, User user, String notes) throws Exception {
        Optional<CashShift> active = repo.findFirstByStatusOrderByOpenTimeDesc(CashShiftStatus.ABIERTA);
        if (active.isPresent()) {
            throw new IllegalStateException("Ya existe un turno de caja abierto. Ciérrelo antes de abrir uno nuevo.");
        }

        CashShift shift = new CashShift();
        shift.setUser(user);
        shift.setOpenTime(LocalDateTime.now());
        shift.setInitialAmount(initialAmount != null ? initialAmount : BigDecimal.ZERO);
        shift.setExpectedCash(shift.getInitialAmount());
        shift.setStatus(CashShiftStatus.ABIERTA);
        shift.setNotes(notes);

        return repo.save(shift);
    }

    @Override
    public Optional<CashShift> getActiveShift() {
        return repo.findFirstByStatusOrderByOpenTimeDesc(CashShiftStatus.ABIERTA);
    }

    @Override
    @Transactional
    public CashShift closeShift(Integer idCashShift, BigDecimal actualCash, String notes) throws Exception {
        CashShift shift = repo.findById(idCashShift)
                .orElseThrow(() -> new ModelNotFoundException("Turno de caja no encontrado: " + idCashShift));

        if (shift.getStatus() == CashShiftStatus.CERRADA) {
            throw new IllegalStateException("El turno de caja ya se encuentra cerrado.");
        }

        // Recalcular montos consolidados antes del cierre
        recalculateShiftTotals(idCashShift);

        shift.setCloseTime(LocalDateTime.now());
        shift.setActualCash(actualCash != null ? actualCash : BigDecimal.ZERO);
        shift.setDifference(shift.getActualCash().subtract(shift.getExpectedCash()));
        shift.setStatus(CashShiftStatus.CERRADA);
        if (notes != null && !notes.trim().isEmpty()) {
            shift.setNotes((shift.getNotes() != null ? shift.getNotes() + " | " : "") + notes);
        }

        return repo.save(shift);
    }

    @Override
    @Transactional
    public CashMovement registerMovement(CashMovementDTO dto, User user) throws Exception {
        CashShift shift = repo.findById(dto.getIdCashShift())
                .orElseThrow(() -> new ModelNotFoundException("Turno de caja no encontrado: " + dto.getIdCashShift()));

        if (shift.getStatus() == CashShiftStatus.CERRADA) {
            throw new IllegalStateException("No se pueden registrar movimientos en un turno de caja cerrado.");
        }

        CashMovement movement = new CashMovement();
        movement.setCashShift(shift);
        movement.setUser(user);
        movement.setType(dto.getType());
        movement.setAmount(dto.getAmount());
        movement.setReason(dto.getReason());
        movementRepo.save(movement);

        if (dto.getType() == CashMovementType.INGRESO) {
            shift.setCashIn(shift.getCashIn().add(dto.getAmount()));
        } else {
            shift.setCashOut(shift.getCashOut().add(dto.getAmount()));
        }

        BigDecimal expected = shift.getInitialAmount()
                .add(shift.getCashSales())
                .add(shift.getCashIn())
                .subtract(shift.getCashOut());
        shift.setExpectedCash(expected);
        repo.save(shift);

        return movement;
    }

    @Override
    public List<CashMovement> getMovementsByShift(Integer idCashShift) {
        return movementRepo.findByCashShiftIdCashShift(idCashShift);
    }

    @Override
    @Transactional
    public void registerPaymentInShift(CashShift shift, Payment payment) {
        String methodName = payment.getPaymentMethod() != null ? payment.getPaymentMethod().getName().toUpperCase() : "";

        if (methodName.contains("EFECTIVO")) {
            shift.setCashSales(shift.getCashSales().add(payment.getAmount()));
        } else if (methodName.contains("TARJETA")) {
            shift.setCardSales(shift.getCardSales().add(payment.getAmount()));
        } else if (methodName.contains("YAPE")) {
            shift.setYapeSales(shift.getYapeSales().add(payment.getAmount()));
        } else if (methodName.contains("PLIN")) {
            shift.setPlinSales(shift.getPlinSales().add(payment.getAmount()));
        } else {
            shift.setTransferSales(shift.getTransferSales().add(payment.getAmount()));
        }

        BigDecimal expected = shift.getInitialAmount()
                .add(shift.getCashSales())
                .add(shift.getCashIn())
                .subtract(shift.getCashOut());
        shift.setExpectedCash(expected);
        repo.save(shift);
    }

    @Override
    @Transactional
    public CashShift recalculateShiftTotals(Integer idCashShift) throws Exception {
        CashShift shift = repo.findById(idCashShift)
                .orElseThrow(() -> new ModelNotFoundException("Turno de caja no encontrado: " + idCashShift));

        List<Payment> payments = paymentRepo.findByCashShiftIdCashShift(idCashShift);
        List<CashMovement> movements = movementRepo.findByCashShiftIdCashShift(idCashShift);

        BigDecimal cashSales = BigDecimal.ZERO;
        BigDecimal cardSales = BigDecimal.ZERO;
        BigDecimal yapeSales = BigDecimal.ZERO;
        BigDecimal plinSales = BigDecimal.ZERO;
        BigDecimal transferSales = BigDecimal.ZERO;

        for (Payment p : payments) {
            String m = p.getPaymentMethod() != null ? p.getPaymentMethod().getName().toUpperCase() : "";
            if (m.contains("EFECTIVO")) cashSales = cashSales.add(p.getAmount());
            else if (m.contains("TARJETA")) cardSales = cardSales.add(p.getAmount());
            else if (m.contains("YAPE")) yapeSales = yapeSales.add(p.getAmount());
            else if (m.contains("PLIN")) plinSales = plinSales.add(p.getAmount());
            else transferSales = transferSales.add(p.getAmount());
        }

        BigDecimal cashIn = BigDecimal.ZERO;
        BigDecimal cashOut = BigDecimal.ZERO;
        for (CashMovement mov : movements) {
            if (mov.getType() == CashMovementType.INGRESO) cashIn = cashIn.add(mov.getAmount());
            else cashOut = cashOut.add(mov.getAmount());
        }

        shift.setCashSales(cashSales);
        shift.setCardSales(cardSales);
        shift.setYapeSales(yapeSales);
        shift.setPlinSales(plinSales);
        shift.setTransferSales(transferSales);
        shift.setCashIn(cashIn);
        shift.setCashOut(cashOut);

        BigDecimal expected = shift.getInitialAmount().add(cashSales).add(cashIn).subtract(cashOut);
        shift.setExpectedCash(expected);

        return repo.save(shift);
    }
}
