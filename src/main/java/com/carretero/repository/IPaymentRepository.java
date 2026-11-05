package com.carretero.repository;

import com.carretero.model.Payment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IPaymentRepository extends IGenericRepository<Payment, Integer> {
    List<Payment> findByOrderIdOrder(Integer idOrder);
    List<Payment> findByCashShiftIdCashShift(Integer idCashShift);

    /**
     * Cobros de un turno de caja, del mas antiguo al mas reciente.
     *
     * Trae el pedido en la misma consulta porque el detalle del turno muestra el
     * codigo del pedido y de que tipo de venta vino; el pedido es LAZY y, con la
     * sesion ya cerrada, leerlo despues fallaria.
     */
    @Query("SELECT p FROM Payment p JOIN FETCH p.order WHERE p.cashShift.idCashShift = :idCashShift ORDER BY p.paymentDate ASC")
    List<Payment> findByShiftWithOrder(@Param("idCashShift") Integer idCashShift);

    /**
     * Cobros que hizo una persona en un turno, del mas reciente al mas antiguo.
     *
     * El filtro por usuario va en la consulta y no en la pantalla: las ventas de
     * un mesero no son asunto de otro, asi que las de los demas no deben siquiera
     * viajar al navegador de quien no las hizo.
     */
    @Query("SELECT p FROM Payment p JOIN FETCH p.order WHERE p.cashShift.idCashShift = :idCashShift AND p.user.idUser = :idUser ORDER BY p.paymentDate DESC")
    List<Payment> findByShiftAndUserWithOrder(@Param("idCashShift") Integer idCashShift,
                                              @Param("idUser") Integer idUser);

    @Query("SELECT p FROM Payment p JOIN FETCH p.order o WHERE o.idOrder = :idOrder ORDER BY p.paymentDate ASC")
    List<Payment> findByOrderWithOrder(@Param("idOrder") Integer idOrder);

    @Query("SELECT p FROM Payment p JOIN FETCH p.order ORDER BY p.paymentDate DESC")
    List<Payment> findAllWithOrder();
}
