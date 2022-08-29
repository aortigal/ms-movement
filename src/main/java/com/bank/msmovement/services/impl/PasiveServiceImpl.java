package com.bank.msmovement.services.impl;

import com.bank.msmovement.models.utils.Amount;
import com.bank.msmovement.models.utils.ResponseAmount;
import com.bank.msmovement.models.utils.ResponseParameter;
import com.bank.msmovement.services.PasiveService;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


@Service
public class PasiveServiceImpl implements PasiveService {

    private final WebClient webClient;

    public PasiveServiceImpl(WebClient.Builder webClientBuilder){
        this.webClient = webClientBuilder.baseUrl("http://localhost:8082").build();
    }

    @Override
    public Mono<ResponseParameter> getTypeParams(String idPasive)
    {
        return webClient.get()
                .uri("/api/pasive/type/"+ idPasive)
                .retrieve()
                .bodyToMono(ResponseParameter.class);
    }

    @Override
    public Mono<ResponseAmount> getAmount(String idPasive) {
        return webClient.get()
                .uri("/api/pasive/amount/"+ idPasive)
                .retrieve()
                .bodyToMono(ResponseAmount.class);
    }

    @Override
    public Mono<ResponseAmount> setAmount(String idPasive, Amount amount) {
        return webClient.post()
                .uri("/api/pasive/amount/"+ idPasive)
                .body(Mono.just(amount), Amount.class)
                .retrieve()
                .bodyToMono(ResponseAmount.class);
    }
}