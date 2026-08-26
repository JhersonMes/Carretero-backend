package com.carretero.service.implementation;

import com.carretero.dto.PaymentCreateRequestDTO;
import com.carretero.exception.ModelNotFoundException;
import com.carretero.model.*;
import com.carretero.model.enums.OrderStatus;
import com.carretero.repository.IGenericRepository;
import com.carretero.repository.IOrderRepository;
import com.carretero.repository.IPaymentMethodRepository;
import com.carretero.repository.IPaymentRepository;
import com.carretero.service.ICashShiftService;
import com.carretero.service.IOrderService;
import com.carretero.service.IPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService extends GenericService<Payment, Integer> implements IPaymentService {

    private final IPaymentRepository repo;
    private final IOrderRepository orderRepo;
    private final IPaymentMethodRepository paymentMethodRepo;
    private final ICashShiftService cashShiftService;
    private final IOrderService orderService;

    @Override
    protected IGenericRepository<Payment, Integer> getRepo() {
        return repo;
    }

    @Override
    @Transactional
    public List<Payment> registerPayments(PaymentCreateRequestDTO request, User user) throws Exception {
        Order order = orderRepo.findById(request.getIdOrder())
                .orElseThrow(() -> new ModelNotFoundException("Pedido no encontrado: " + request.getIdOrder()));

        BigDecimal alreadyPaid = repo.findByOrderIdOrder(order.getIdOrder()).stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal incomingTotal = request.getPayments().stream()
                .map(PaymentCreateRequestDTO.PaymentItemDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remaining = order.getTotal().subtract(alreadyPaid);
        if (incomingTotal.compareTo(remaining) > 0) {
            throw new IllegalStateException(
                    "El monto ingresado (S/ " + incomingTotal + ") supera el saldo pendiente (S/ " + remaining + ").");
        }

        Optional<CashShift> activeShiftOpt = cashShiftService.getActiveShift();
        CashShift activeShift = activeShiftOpt.orElse(null);

        List<Payment> createdPayments = new ArrayList<>();
        BigDecimal totalPaidNow = BigDecimal.ZERO;

        for (PaymentCreateRequestDTO.PaymentItemDTO item : request.getPayments()) {
            PaymentMethod method = paymentMethodRepo.findById(item.getIdPaymentMethod())
                    .orElseThrow(() -> new ModelNotFoundException("Método de pago no encontrado: " + item.getIdPaymentMethod()));

            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setCashShift(activeShift);
            payment.setPaymentMethod(method);
            payment.setUser(user);
            payment.setAmount(item.getAmount());
            payment.setAmountTendered(item.getAmountTendered());
            payment.setChangeGiven(item.getChangeGiven());
            payment.setReferenceNumber(item.getReferenceNumber());
            payment.setPaymentDate(LocalDateTime.now());

            Payment saved = repo.save(payment);
            createdPayments.add(saved);

            if (activeShift != null) {
                cashShiftService.registerPaymentInShift(activeShift, saved);
            }

            totalPaidNow = totalPaidNow.add(item.getAmount());
        }

        // Verificar si la orden ya está totalmente pagada
        List<Payment> allOrderPayments = repo.findByOrderIdOrder(order.getIdOrder());
        BigDecimal sumAllPayments = allOrderPayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sumAllPayments.compareTo(order.getTotal()) >= 0) {
            orderService.updateOrderStatus(order.getIdOrder(), OrderStatus.PAGADO);
        }

        return createdPayments;
    }

    @Override
    public List<Payment> findByOrderId(Integer idOrder) {
        return repo.findByOrderIdOrder(idOrder);
    }

    @Override
    public List<Payment> findByCashShiftId(Integer idCashShift) {
        return repo.findByCashShiftIdCashShift(idCashShift);
    }
}
