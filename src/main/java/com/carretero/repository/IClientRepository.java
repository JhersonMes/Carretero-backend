package com.carretero.repository;

import com.carretero.model.Client;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IClientRepository extends IGenericRepository<Client, Integer> {
    Optional<Client> findByDocNumber(String docNumber);
    Optional<Client> findByPhone(String phone);
    
    @Query("SELECT c FROM Client c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR c.docNumber LIKE CONCAT('%', :query, '%') OR c.phone LIKE CONCAT('%', :query, '%')")
    List<Client> searchClients(@Param("query") String query);
}
