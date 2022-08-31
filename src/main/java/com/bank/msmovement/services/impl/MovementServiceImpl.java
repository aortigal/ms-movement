package com.bank.msmovement.services.impl;

import com.bank.msmovement.handler.ResponseHandler;
import com.bank.msmovement.models.dao.MovementDao;
import com.bank.msmovement.models.documents.Movement;
import com.bank.msmovement.models.documents.Parameter;
import com.bank.msmovement.models.emus.TypeMovement;
import com.bank.msmovement.models.emus.TypePasiveMovement;
import com.bank.msmovement.models.utils.Amount;
import com.bank.msmovement.services.MovementService;
import com.bank.msmovement.services.PasiveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class MovementServiceImpl implements MovementService {

    @Autowired
    private MovementDao dao;

    @Autowired
    private PasiveService pasiveService;

    private static final Logger log = LoggerFactory.getLogger(MovementServiceImpl.class);

    @Override
    public Mono<ResponseHandler> findAll() {
        log.info("[INI] findAll Movement");
        return dao.findAll()
                .doOnNext(movement -> log.info(movement.toString()))
                .collectList()
                .map(movements -> new ResponseHandler("Done", HttpStatus.OK, movements))
                .onErrorResume(error -> Mono.just(new ResponseHandler(error.getMessage(), HttpStatus.BAD_REQUEST, null)))
                .switchIfEmpty(Mono.just(new ResponseHandler("Empty", HttpStatus.NO_CONTENT, null)))
                .doFinally(fin -> log.info("[END] findAll Movement"));
    }

    @Override
    public Mono<ResponseHandler> findByIdClient(String idClient) {
        log.info("[INI] findByIdClient Movement");
        return dao.findAll()
                .filter(movement ->
                        movement.getClientId().equals(idClient)
                )
                .collectList()
                .map(movements -> new ResponseHandler("Done", HttpStatus.OK, movements))
                .onErrorResume(error -> Mono.just(new ResponseHandler(error.getMessage(), HttpStatus.BAD_REQUEST, null)))
                .switchIfEmpty(Mono.just(new ResponseHandler("Empty", HttpStatus.NO_CONTENT, null)))
                .doFinally(fin -> log.info("[END] findByIdClient Movement"));
    }

    @Override
    public Mono<ResponseHandler> find(String id) {
        log.info("[INI] find Movement");
        return dao.findById(id)
                .doOnNext(movement -> log.info(movement.toString()))
                .map(movement -> new ResponseHandler("Done", HttpStatus.OK, movement))
                .onErrorResume(error -> Mono.just(new ResponseHandler(error.getMessage(), HttpStatus.BAD_REQUEST, null)))
                .switchIfEmpty(Mono.just(new ResponseHandler("Empty", HttpStatus.NO_CONTENT, null)))
                .doFinally(fin -> log.info("[END] find Movement"));
    }

    @Override
    public Mono<ResponseHandler> create(Movement mov) {
        log.info("[INI] create Movement");
        mov.setCreated(LocalDateTime.now());

        AtomicReference<Float> currentAmount = new AtomicReference<>(0f);
        AtomicReference<Float> addAmount = new AtomicReference<>(0f);

        return pasiveService.getAmount(mov.getPasiveId())
                .flatMap(responseAmount ->
                {
                    if(responseAmount.getData() != null)
                    {
                        currentAmount.set(responseAmount.getData().getAmount());

                        return pasiveService.getTypeParams(mov.getPasiveId())
                                .flatMap(parameters -> {
                                    log.info(parameters.toString());

                                    List<Parameter> parametersData = parameters.getData();

                                    AtomicReference<Integer> currentMovement = new AtomicReference<>(0);
                                    AtomicReference<Boolean> differentDates = new AtomicReference<>(false);
                                    AtomicReference<Integer> maxMovement = new AtomicReference<>(0);

                                    mov.setTypePasiveMovement(TypePasiveMovement.fromInteger(parametersData.get(0).getCode()));

                                    for (Parameter parameter: parametersData)
                                    {
                                        if(parameter.getValue().equals("1") && !parameter.getArgument().equals("0"))
                                        {
                                            float percentage = Float.parseFloat(parameter.getArgument());
                                            mov.setComissionAmount(mov.getAmount()*percentage);
                                        }
                                        else if (parameter.getValue().equals("2") )
                                        {
                                            if(!parameter.getArgument().equals("false"))
                                            {
                                                int day = Integer.parseInt(parameter.getArgument());
                                                differentDates.set(LocalDateTime.now().getDayOfMonth() == day);
                                            }
                                            else
                                                differentDates.set(true);
                                        }
                                        else if (parameter.getValue().equals("3") )
                                        {
                                            if(!parameter.getArgument().equals("false"))
                                                maxMovement.set(Integer.parseInt(parameter.getArgument()));
                                            else
                                                maxMovement.set(99999999);
                                        }
                                    }

                                    if(mov.getTypeMovement().equals(TypeMovement.DEPOSITS))
                                    {
                                        addAmount.set((addAmount.get() - mov.getComissionAmount()) + mov.getAmount());
                                    }
                                    else if(mov.getTypeMovement().equals(TypeMovement.WITHDRAWALS))
                                    {
                                        addAmount.set((addAmount.get() - mov.getComissionAmount())  - mov.getAmount());
                                    }

                                    float newAmount = currentAmount.get() - addAmount.get();

                                    if(newAmount > 0)
                                    {
                                        Amount amount = new Amount();
                                        amount.setAmount(addAmount.get());
                                        amount.setIdPasive(mov.getPasiveId());

                                        return pasiveService.setAmount(mov.getPasiveId(),amount)
                                                .flatMap(responseAmount1 -> {
                                                    if(responseAmount1.getStatus().equalsIgnoreCase("Ok"))
                                                        return dao.findAll()
                                                                .doOnNext(movement -> {
                                                                    String dateCreated = movement.getCreated().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                                                                    String dateNow = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                                                                    if(dateCreated.equals(dateNow) && movement.getTypePasiveMovement() == mov.getTypePasiveMovement())
                                                                    {
                                                                        currentMovement.getAndSet(currentMovement.get() + 1);
                                                                    }
                                                                }).collectList().flatMap(movements -> {
                                                                    log.info(differentDates.get().toString());
                                                                    log.info(currentMovement.get().toString());
                                                                    log.info(maxMovement.get().toString());
                                                                    if(differentDates.get() && currentMovement.get() < maxMovement.get())
                                                                    {

                                                                        return dao.save(mov)
                                                                                .doOnNext(movement -> log.info(movement.toString()))
                                                                                .map(movement -> new ResponseHandler("Done", HttpStatus.OK, movement)                )
                                                                                //.onErrorResume(error -> Mono.just(new ResponseHandler(error.getMessage(), HttpStatus.BAD_REQUEST, null)))
                                                                                .doFinally(fin -> log.info("[END] create Movement"));
                                                                    }
                                                                    else
                                                                    {
                                                                        return Mono.just(new ResponseHandler("You can't generate more movement today", HttpStatus.BAD_REQUEST, null));
                                                                    }
                                                                });
                                                    else
                                                        return Mono.just(new ResponseHandler("Error", HttpStatus.BAD_REQUEST, null));
                                                });
                                    }
                                    else
                                    {
                                        return Mono.just(new ResponseHandler("You don't have enough credit", HttpStatus.BAD_REQUEST, null));
                                    }

                                })
                                .switchIfEmpty(Mono.just(new ResponseHandler("Empty", HttpStatus.NO_CONTENT, null)));
                    }
                    else
                        return Mono.just(new ResponseHandler("Not found", HttpStatus.BAD_REQUEST, null));

                })
                .switchIfEmpty(Mono.just(new ResponseHandler("Empty", HttpStatus.NO_CONTENT, null)))
                .doFinally(fin -> log.info("[END] create Movement"));

    }

    @Override
    public Mono<ResponseHandler> update(String id,Movement mov) {
        log.info("[INI] update Movement");
        return dao.existsById(id).flatMap(check -> {
            if (check){
                mov.setDateUpdate(LocalDateTime.now());
                return dao.save(mov)
                        .doOnNext(movement -> log.info(movement.toString()))
                        .map(movement -> new ResponseHandler("Done", HttpStatus.OK, movement)                )
                        .onErrorResume(error -> Mono.just(new ResponseHandler(error.getMessage(), HttpStatus.BAD_REQUEST, null)));
            }
            else
                return Mono.just(new ResponseHandler("Not found", HttpStatus.NOT_FOUND, null));

        }).doFinally(fin -> log.info("[END] update Movement"));
    }

    @Override
    public Mono<ResponseHandler> delete(String id) {
        log.info("[INI] delete Movement");
        log.info(id);

        return dao.existsById(id).flatMap(check -> {
            if (check)
                return dao.deleteById(id).then(Mono.just(new ResponseHandler("Done", HttpStatus.OK, null)));
            else
                return Mono.just(new ResponseHandler("Not found", HttpStatus.NOT_FOUND, null));
        }).doFinally(fin -> log.info("[END] delete Movement"));
    }

    @Override
    public Mono<ResponseHandler> getBalance(String id) {
        log.info("[INI] getBalance Movement");
        log.info(id);

        AtomicReference<Float> balance = new AtomicReference<>((float) 0);

        return dao.findAll()
                .doOnNext(movement -> {
                    if(movement.getClientId().equals(id))
                        if(movement.getTypeMovement().equals(TypeMovement.DEPOSITS))
                        {
                            balance.set((balance.get() - movement.getComissionAmount()) + movement.getAmount());
                        }
                        else if(movement.getTypeMovement().equals(TypeMovement.WITHDRAWALS))
                        {
                            balance.set((balance.get() - movement.getComissionAmount())  - movement.getAmount());
                        }
                })
                .collectList()
                .map(movements -> new ResponseHandler("Done", HttpStatus.OK, balance.get()))
                .onErrorResume(error -> Mono.just(new ResponseHandler(error.getMessage(), HttpStatus.BAD_REQUEST, null)))
                .switchIfEmpty(Mono.just(new ResponseHandler("Empty", HttpStatus.NO_CONTENT, null)))
                .doFinally(fin -> log.info("[END] getBalance Movement"));
    }
}
