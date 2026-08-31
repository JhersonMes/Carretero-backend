package com.carretero.service;

import com.carretero.dto.TableBoardDTO;

import java.util.List;

public interface ISalonBoardService {

    /** Tablero del salon: mesas en el orden del plano, con su pedido activo resumido. */
    List<TableBoardDTO> getBoard();

    /**
     * Reordena el plano del salon segun la secuencia de ids recibida.
     * El indice de cada mesa pasa a ser su posicion en la lista.
     */
    void reorderTables(List<Integer> orderedTableIds) throws Exception;
}
