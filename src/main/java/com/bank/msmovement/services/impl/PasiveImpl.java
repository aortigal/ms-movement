package com.bank.msmovement.services.impl;

import com.bank.msmovement.models.utils.Mont;
import com.bank.msmovement.models.utils.ResponseMont;
import com.bank.msmovement.models.utils.ResponseParameter;
import com.bank.msmovement.services.PasiveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


@Service
public class PasiveImpl implements PasiveService {

    @Autowired
    WebClient webClient;


    @Override
    public Mono<ResponseParameter> getTypeParams(String idPasive)
    {
        return webClient.get()
                .uri("/api/pasive/type/"+ idPasive)
                .retrieve()
                .bodyToMono(ResponseParameter.class);
    }

    @Override
    public Mono<ResponseMont> getMont(String idPasive) {
        return webClient.get()
                .uri("/api/pasive/mont/"+ idPasive)
                .retrieve()
                .bodyToMono(ResponseMont.class);
    }

    @Override
    public Mono<ResponseMont> setMont(String idPasive, Mont mont) {
        return webClient.post()
                .uri("/api/pasive/mont/"+ idPasive)
                .body(Mono.just(mont), Mont.class)
                .retrieve()
                .bodyToMono(ResponseMont.class);
    }
}