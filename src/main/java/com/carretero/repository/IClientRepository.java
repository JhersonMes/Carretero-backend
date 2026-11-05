package com.carretero.repository;

import com.carretero.model.Client;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IClientRepository extends IGenericRepository<Client, Integer> {
    /**
     * Todas las fichas con ese documento. Deberia devolver una sola, pero las bases
     * que vienen de antes de la restriccion de unicidad pueden traer repetidos, y
     * una consulta que espere un unico resultado revienta al toparse con ellos.
     */
    List<Client> findAllByDocNumber(String docNumber);

    Optional<Client> findByPhone(String phone);
    
    @Query("SELECT c FROM Client c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR c.docNumber LIKE CONCAT('%', :query, '%') OR c.phone LIKE CONCAT('%', :query, '%')")
    List<Client> searchClients(@Param("query") String query);
}
