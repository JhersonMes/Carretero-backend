package com.carretero;

import com.carretero.config.MapperConfig;
import com.carretero.dto.ClientDTO;
import com.carretero.dto.InvoiceDTO;
import com.carretero.model.Client;
import com.carretero.model.Invoice;
import com.carretero.model.Order;
import org.hibernate.collection.spi.PersistentBag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MapperConfigTest {

    private final MapperConfig config = new MapperConfig();

    @Test
    void clientMapperSkipsAddresses() {
        Client client = new Client();
        client.setIdClient(7);
        client.setName("Ana");
        client.setPhone("987654321");

        ClientDTO dto = config.clientMapper().map(client, ClientDTO.class);

        assertEquals(7, dto.getIdClient());
        assertEquals("Ana", dto.getName());
        assertNull(dto.getAddresses());
    }

    /**
     * Reproduce lo que llega de verdad al controlador: las direcciones son una
     * coleccion LAZY sin inicializar y sin sesion detras. Si el mapper la toca,
     * revienta con el mismo error que se veia al cobrar un delivery.
     */
    @Test
    void clientMapperDoesNotTouchAnUninitializedCollection() {
        Client client = new Client();
        client.setIdClient(7);
        client.setName("Ana");
        client.setAddresses(new PersistentBag<>(null));

        ClientDTO dto = config.clientMapper().map(client, ClientDTO.class);

        assertEquals("Ana", dto.getName());
        assertNull(dto.getAddresses());
    }

    @Test
    void invoiceMapperSkipsClientAndKeepsTheRest() {
        Order order = new Order();
        order.setIdOrder(3);
        order.setOrderCode("PED-20260831-0001");

        Client client = new Client();
        client.setIdClient(7);
        client.setName("Ana");

        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setClient(client);
        invoice.setSeries("B001");
        invoice.setCorrelativeNumber(1);
        invoice.setFullNumber("B001-00000001");
        invoice.setTaxableAmount(new BigDecimal("84.75"));
        invoice.setIgvAmount(new BigDecimal("15.25"));
        invoice.setTotalAmount(new BigDecimal("100.00"));

        InvoiceDTO dto = config.invoiceMapper().map(invoice, InvoiceDTO.class);

        assertNull(dto.getClient());
        assertEquals("B001-00000001", dto.getFullNumber());
        assertEquals(new BigDecimal("15.25"), dto.getIgvAmount());
        assertEquals(new BigDecimal("100.00"), dto.getTotalAmount());
    }
}
