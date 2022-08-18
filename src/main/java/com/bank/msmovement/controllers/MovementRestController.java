package com.bank.msmovement.controllers;

import com.bank.msmovement.handler.ResponseHandler;
import com.bank.msmovement.models.documents.Movement;
import com.bank.msmovement.services.MovementService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/movement")
public class MovementRestController
{
    @Autowired
    private MovementService movementService;

    @GetMapping
    public Mono<ResponseHandler> findAll()
    {
        return movementService.findAll();
    }

    @GetMapping("/clientMovements/{idClient}")
    public Mono<ResponseHandler> findByIdClient(@PathVariable String idClient)
    {
        return movementService.findByIdClient(idClient);
    }

    @GetMapping("/{id}")
    public Mono<ResponseHandler> find(@PathVariable String id)
    {
        return movementService.find(id);
    }

    @PostMapping
    @CircuitBreaker(name="pasive", fallbackMethod = "fallBackPasive")
    public Mono<ResponseHandler> create(@Valid  @RequestBody Movement mov)
    {
        return movementService.create(mov);
    }

    @PutMapping("/{id}")
    public Mono<ResponseHandler> update(@PathVariable("id") String id,@Valid @RequestBody Movement mov)
    {
        return movementService.update(id,mov);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseHandler> delete(@PathVariable("id") String id)
    {
        return movementService.delete(id);
    }

    @GetMapping("/balance/{id}")
    public Mono<ResponseHandler> getBalance(@PathVariable("id") String id)
    {
        return movementService.getBalance(id);
    }

    public Mono<ResponseHandler> fallBackPasive(RuntimeException runtimeException){
        return Mono.just(new ResponseHandler("Microservicio externo no responde",HttpStatus.BAD_REQUEST,runtimeException.getMessage()));
    }
}
