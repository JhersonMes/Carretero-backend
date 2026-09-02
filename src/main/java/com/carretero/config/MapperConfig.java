package com.carretero.config;

import com.carretero.dto.ClientDTO;
import com.carretero.dto.InvoiceDTO;
import com.carretero.model.Client;
import com.carretero.model.Invoice;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MapperConfig {

    @Bean
    @Primary
    public ModelMapper defaultMapper() {
        return new ModelMapper();
    }

    @Bean public ModelMapper roleMapper() { return new ModelMapper(); }
    @Bean public ModelMapper userMapper() { return new ModelMapper(); }

    /**
     * Las direcciones del cliente son LAZY y, con open-in-view apagado, llegan al
     * controlador sin inicializar: si el mapper intenta recorrer esa coleccion
     * revienta ("failed to convert PersistentBag to List"). Por eso se salta en
     * ambos sentidos y las direcciones se cargan aparte, con su propia consulta.
     *
     * Saltarla tambien protege al guardar: un ClientDTO trae direcciones sueltas
     * sin su cliente, y volcarlas sobre la entidad dispararia el orphanRemoval y
     * borraria las que el cliente ya tenia guardadas.
     */
    @Bean
    public ModelMapper clientMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.typeMap(Client.class, ClientDTO.class)
                .addMappings(m -> m.skip(ClientDTO::setAddresses));
        mapper.typeMap(ClientDTO.class, Client.class)
                .addMappings(m -> m.skip(Client::setAddresses));
        return mapper;
    }

    @Bean public ModelMapper addressMapper() { return new ModelMapper(); }
    @Bean public ModelMapper categoryMapper() { return new ModelMapper(); }
    @Bean public ModelMapper productMapper() { return new ModelMapper(); }
    @Bean public ModelMapper diningTableMapper() { return new ModelMapper(); }
    @Bean public ModelMapper orderMapper() { return new ModelMapper(); }
    @Bean public ModelMapper orderDetailMapper() { return new ModelMapper(); }
    @Bean public ModelMapper cashShiftMapper() { return new ModelMapper(); }
    @Bean public ModelMapper cashMovementMapper() { return new ModelMapper(); }
    @Bean public ModelMapper paymentMethodMapper() { return new ModelMapper(); }
    @Bean public ModelMapper paymentMapper() { return new ModelMapper(); }
    /**
     * El comprobante lleva a su cliente, y mapearlo de corrido arrastraria las
     * direcciones LAZY de ese cliente con el mismo problema de arriba. El
     * controlador arma el cliente del comprobante aparte, con el clientMapper.
     *
     * Se parte de un type map vacio y las reglas implicitas se agregan al final:
     * ModelMapper no deja saltar una propiedad cuyos campos anidados ya mapeo
     * (client.name, client.docNumber...), y typeMap() los mapea de entrada.
     */
    @Bean
    public ModelMapper invoiceMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.emptyTypeMap(Invoice.class, InvoiceDTO.class)
                .addMappings(m -> m.skip(InvoiceDTO::setClient))
                .implicitMappings();
        return mapper;
    }

    @Bean public ModelMapper businessConfigMapper() { return new ModelMapper(); }
    @Bean public ModelMapper ingredientMapper() { return new ModelMapper(); }
    @Bean public ModelMapper menuMapper() { return new ModelMapper(); }
}
