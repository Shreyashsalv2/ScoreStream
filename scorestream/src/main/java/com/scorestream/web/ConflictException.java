package com.scorestream.web;

/** Thrown when a request clashes with existing data or a business rule. Mapped to 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
