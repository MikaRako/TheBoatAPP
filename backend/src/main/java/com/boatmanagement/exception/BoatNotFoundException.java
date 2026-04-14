package com.boatmanagement.exception;

import org.springframework.lang.NonNull;

public class BoatNotFoundException extends RuntimeException {

    public BoatNotFoundException(Long id) {
        super("Boat not found with id: " + id);
    }

    /**
     * Overridden with @NonNull because this exception is always constructed
     * with a non-null message. Eliminates nullable-to-@NonNull warnings at
     * call sites (e.g. GlobalExceptionHandler) without any suppression.
     */
    @Override
    @NonNull
    public String getMessage() {
        // super.getMessage() is guaranteed non-null: we always pass a literal to super().
        String msg = super.getMessage();
        return msg != null ? msg : "Boat not found";
    }
}
