package com.kadi_aon.mon_salon.stock.exception;

public class StockInsuffisantException extends IllegalStateException {
    public StockInsuffisantException(String message) {
        super(message);
    }
}
