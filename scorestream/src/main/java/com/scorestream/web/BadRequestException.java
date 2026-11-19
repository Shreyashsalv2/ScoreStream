package com.scorestream.web;

/** Thrown when input is well-formed but makes no sense (e.g. a team playing itself). Mapped to 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
