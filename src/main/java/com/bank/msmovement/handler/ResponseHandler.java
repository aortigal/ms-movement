package com.bank.msmovement.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

public class ResponseHandler
{
    public static ResponseEntity<Object> response(String message, HttpStatus status, Object data)
    {
        Map<String, Object> map = new HashMap<>();
        map.put("message", message);
        map.put("status", status);
        map.put("data", data);

        return new ResponseEntity<>(map,status);
    }
}
