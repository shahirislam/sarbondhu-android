package com.sarbondhu.app.data.repository;

/** A business rule refused a change. The UI maps the reason to a Bangla message. */
public class DataException extends Exception {

    public enum Reason { NOT_FOUND, USERNAME_TAKEN, NEGATIVE_STOCK, INVALID_AMOUNT,
        INSUFFICIENT_STOCK, EMPTY_CART, DEALER_INACTIVE, WRONG_PASSWORD }

    private final Reason reason;

    public DataException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
