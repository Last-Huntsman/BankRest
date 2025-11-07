package org.zuzukov.t1task4.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.zuzukov.bank_rest.exception.BadRequestException;
import org.zuzukov.bank_rest.exception.ApiError;
import org.zuzukov.bank_rest.exception.GlobalExceptionHandler;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalExceptionHandlerTest {
    @Test
    void handleBadRequest_mapsTo400() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<ApiError> resp = handler.handleBadRequest(new BadRequestException("bad"));
        assertEquals(400, resp.getStatusCode().value());
        assertEquals("bad", resp.getBody().getMessage());
    }
}


