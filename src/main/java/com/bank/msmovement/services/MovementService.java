package com.bank.msmovement.services;

import com.bank.msmovement.handler.ResponseHandler;
import com.bank.msmovement.models.documents.Movement;
import reactor.core.publisher.Mono;

public interface MovementService {
    Mono<ResponseHandler> findAll();

    Mono<ResponseHandler> findByIdClient(String idClient);

    Mono<ResponseHandler> find(String id);

    Mono<ResponseHandler> create(Movement mov);

    Mono<ResponseHandler> update(String id,Movement mov);

    Mono<ResponseHandler> delete(String id);

    Mono<ResponseHandler> getBalance(String id);
}
