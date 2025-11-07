package org.zuzukov.bank_rest.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@AllArgsConstructor
public class ApiError {
    private Instant timestamp;
    private int status;
    private ErrorCode code;
    private String message;
    private Map<String, Object> details;
}




