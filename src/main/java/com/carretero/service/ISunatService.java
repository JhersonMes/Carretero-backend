package com.carretero.service;

import com.carretero.dto.DniRucQueryResponseDTO;
import com.carretero.model.Invoice;

public interface ISunatService {
    DniRucQueryResponseDTO queryDocument(String docNumber);
    Invoice dispatchToSunat(Invoice invoice);
}
