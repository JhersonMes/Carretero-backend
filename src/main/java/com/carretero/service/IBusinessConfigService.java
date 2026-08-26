package com.carretero.service;

import com.carretero.model.BusinessConfig;

public interface IBusinessConfigService extends IGenericService<BusinessConfig, Integer> {
    BusinessConfig getConfig();
    BusinessConfig updateConfig(BusinessConfig config) throws Exception;
}
