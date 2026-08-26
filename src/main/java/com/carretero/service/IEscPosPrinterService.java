package com.carretero.service;

public interface IEscPosPrinterService {
    boolean printKitchenComanda(Integer idOrder);
    boolean printBarComanda(Integer idOrder);
    boolean printPreAccount(Integer idOrder);
    boolean printInvoice(Integer idInvoice);
    boolean testPrinterConnection(String ip, int port);
}
