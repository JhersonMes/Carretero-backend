package com.carretero.service.billing;

import com.carretero.model.Invoice;
import com.carretero.service.IElectronicBillingProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * El proveedor que no envia nada.
 *
 * Es el modo de desarrollo, y existe como clase propia en vez de como un "if"
 * dentro del proveedor real para que no haya forma de que un camino sin envio
 * termine devolviendo el estado de uno con envio. Todo lo que sale de aqui es
 * SIMULADO, sin CDR y sin codigo de respuesta.
 */
@Component
public class SimulatedBillingProvider implements IElectronicBillingProvider {

    @Override
    public BillingOutcome send(Invoice invoice) {
        return BillingOutcome.simulated();
    }

    @Override
    public BillingOutcome sendDailySummary(List<Invoice> invoices) {
        return BillingOutcome.simulated();
    }

    @Override
    public BillingOutcome checkTicket(String ticket) {
        return BillingOutcome.simulated();
    }

    @Override
    public String name() {
        return "Modo local";
    }
}
