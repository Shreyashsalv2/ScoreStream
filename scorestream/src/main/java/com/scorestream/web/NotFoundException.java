package com.scorestream.web;

/** Thrown when a requested record doesn't exist. Mapped to 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String entity, Long id) {
        super(entity + " " + id + " not found");
    }
}
