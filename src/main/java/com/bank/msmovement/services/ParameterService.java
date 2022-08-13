package com.bank.msmovement.services;

import com.bank.msmovement.models.utils.Mont;
import com.bank.msmovement.models.utils.ResponseMont;
import com.bank.msmovement.models.utils.ResponseParameter;
import reactor.core.publisher.Mono;

public interface ParameterService {

    Mono<ResponseParameter> getTypeParams(String idPasive);
    Mono<ResponseMont> getMont(String idPasive);
    Mono<ResponseMont> setMont(String idPasive, Mont mont);
}
