package com.bank.msmovement.services;

import com.bank.msmovement.models.utils.Amount;
import com.bank.msmovement.models.utils.ResponseAmount;
import com.bank.msmovement.models.utils.ResponseParameter;
import reactor.core.publisher.Mono;

public interface PasiveService {

    Mono<ResponseParameter> getTypeParams(String idPasive);
    Mono<ResponseAmount> getAmount(String idPasive);
    Mono<ResponseAmount> setAmount(String idPasive, Amount amount);
}
