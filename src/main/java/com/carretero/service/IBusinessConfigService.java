package com.carretero.service;

import com.carretero.model.BusinessConfig;

public interface IBusinessConfigService extends IGenericService<BusinessConfig, Integer> {
    BusinessConfig getConfig();
    BusinessConfig updateConfig(BusinessConfig config) throws Exception;

    /** true si el PIN recibido es el que autoriza anular ventas. */
    boolean matchesAdminPin(String pin);

    void changeAdminPin(String newPin) throws Exception;
}
