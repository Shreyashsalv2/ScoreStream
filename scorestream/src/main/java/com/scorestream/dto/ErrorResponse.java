package com.scorestream.dto;

import java.time.Instant;
import java.util.Map;

/** The JSON body of every error response, so clients can handle errors the same way everywhere. */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
}
