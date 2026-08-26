package com.carretero.config;

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
    @Bean public ModelMapper clientMapper() { return new ModelMapper(); }
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
    @Bean public ModelMapper invoiceMapper() { return new ModelMapper(); }
    @Bean public ModelMapper businessConfigMapper() { return new ModelMapper(); }
    @Bean public ModelMapper ingredientMapper() { return new ModelMapper(); }
    @Bean public ModelMapper menuMapper() { return new ModelMapper(); }
}
