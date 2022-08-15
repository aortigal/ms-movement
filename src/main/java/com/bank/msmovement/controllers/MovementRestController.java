package com.bank.msmovement.controllers;

import com.bank.msmovement.handler.ResponseHandler;
import com.bank.msmovement.models.dao.MovementDao;
import com.bank.msmovement.models.documents.Movement;
import com.bank.msmovement.models.documents.Parameter;
import com.bank.msmovement.models.emus.TypeMovement;
import com.bank.msmovement.models.emus.TypePasiveMovement;
import com.bank.msmovement.models.utils.Mont;
import com.bank.msmovement.services.PasiveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/movement")
public class MovementRestController
{
    @Autowired
    private MovementDao dao;

    @Autowired
    private PasiveService pasiveService;
    private static final Logger log = LoggerFactory.getLogger(MovementRestController.class);

    @GetMapping
    public Mono<ResponseEntity<Object>> findAll()
    {
        log.info("[INI] findAll Movement");
        return dao.findAll()
                .doOnNext(movement -> log.info(movement.toString()))
                .collectList()
                .map(movements -> ResponseHandler.response("Done", HttpStatus.OK, movements))
                .onErrorResume(error -> Mono.just(ResponseHandler.response(error.getMessage(), HttpStatus.BAD_REQUEST, null)))
                .switchIfEmpty(Mono.just(ResponseHandler.response("Empty", HttpStatus.NO_CONTENT, null)))
                .doFinally(fin -> log.info("[END] findAll Movement"));
    }

    @GetMapping("/clientMovements/{idClient}")
    public Mono<ResponseEntity<Object>> findByIdClient(@PathVariable String idClient)
    {
        log.info("[INI] findByIdClient Movement");
        return dao.findAll()
                .filter(movement ->
                    movement.getClientId().equals(idClient)
                )
                .collectList()
                .map(movements -> ResponseHandler.response("Done", HttpStatus.OK, movements))
                .onErrorResume(error -> Mono.just(ResponseHandler.response(error.getMessage(), HttpStatus.BAD_REQUEST, null)))
                .switchIfEmpty(Mono.just(ResponseHandler.response("Empty", HttpStatus.NO_CONTENT, null)))
                .doFinally(fin -> log.info("[END] findByIdClient Movement"));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Object>> find(@PathVariable String id)
    {
        log.info("[INI] find Movement");
        return dao.findById(id)
                .doOnNext(movement -> log.info(movement.toString()))
                .map(movement -> ResponseHandler.response("Done", HttpStatus.OK, movement))
                .onErrorResume(error -> Mono.just(ResponseHandler.response(error.getMessage(), HttpStatus.BAD_REQUEST, null)))
                .switchIfEmpty(Mono.just(ResponseHandler.response("Empty", HttpStatus.NO_CONTENT, null)))
                .doFinally(fin -> log.info("[END] find Movement"));
    }

    @PostMapping
    public Mono<ResponseEntity<Object>> create(@Valid  @RequestBody Movement mov)
    {
        log.info("[INI] create Movement");
        mov.setCreated(LocalDateTime.now());

        AtomicReference<Float> currentMont = new AtomicReference<>(0f);
        AtomicReference<Float> addMont = new AtomicReference<>(0f);

        return pasiveService.getMont(mov.getPasiveId())
                .flatMap(responseMont ->
                {
                    if(responseMont.getData() != null)
                    {
                        currentMont.set(responseMont.getData().getMont());

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
                                            mov.setComissionMont(mov.getMont()*percentage);
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
                                        addMont.set((addMont.get() - mov.getComissionMont()) + mov.getMont());
                                    }
                                    else if(mov.getTypeMovement().equals(TypeMovement.WITHDRAWALS))
                                    {
                                        addMont.set((addMont.get() - mov.getComissionMont())  - mov.getMont());
                                    }

                                    float newMont = currentMont.get() - addMont.get();

                                    if(newMont > 0)
                                    {
                                        Mont mont = new Mont();
                                        mont.setMont(addMont.get());
                                        mont.setIdPasive(mov.getPasiveId());

                                        return pasiveService.setMont(mov.getPasiveId(),mont)
                                                .flatMap(responseMont1 -> {
                                                    if(responseMont1.getStatus().equalsIgnoreCase("Ok"))
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
                                                                                .map(movement -> ResponseHandler.response("Done", HttpStatus.OK, movement)                )
                                                                                .onErrorResume(error -> Mono.just(ResponseHandler.response(error.getMessage(), HttpStatus.BAD_REQUEST, null)))
                                                                                .doFinally(fin -> log.info("[END] create Movement"));
                                                                    }
                                                                    else
                                                                    {
                                                                        return Mono.just(ResponseHandler.response("You can't generate more movement today", HttpStatus.BAD_REQUEST, null));
                                                                    }
                                                                });
                                                    else
                                                        return Mono.just(ResponseHandler.response("Error", HttpStatus.BAD_REQUEST, null));
                                                });
                                    }
                                    else
                                    {
                                        return Mono.just(ResponseHandler.response("You don't have enough credit", HttpStatus.BAD_REQUEST, null));
                                    }

                                })
                                .switchIfEmpty(Mono.just(ResponseHandler.response("Empty", HttpStatus.NO_CONTENT, null)));
                    }
                    else
                        return Mono.just(ResponseHandler.response("Not found", HttpStatus.BAD_REQUEST, null));

                })
                .switchIfEmpty(Mono.just(ResponseHandler.response("Empty", HttpStatus.NO_CONTENT, null)));


    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Object>> update(@PathVariable("id") String id,@Valid @RequestBody Movement mov)
    {
        log.info("[INI] update Movement");
        return dao.existsById(id).flatMap(check -> {
            if (check){
                mov.setDateUpdate(LocalDateTime.now());
                return dao.save(mov)
                        .doOnNext(movement -> log.info(movement.toString()))
                        .map(movement -> ResponseHandler.response("Done", HttpStatus.OK, movement)                )
                        .onErrorResume(error -> Mono.just(ResponseHandler.response(error.getMessage(), HttpStatus.BAD_REQUEST, null)));
            }
            else
                return Mono.just(ResponseHandler.response("Not found", HttpStatus.NOT_FOUND, null));

        }).doFinally(fin -> log.info("[END] update Movement"));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Object>> delete(@PathVariable("id") String id)
    {
        log.info("[INI] delete Movement");
        log.info(id);

        return dao.existsById(id).flatMap(check -> {
            if (check)
                return dao.deleteById(id).then(Mono.just(ResponseHandler.response("Done", HttpStatus.OK, null)));
            else
                return Mono.just(ResponseHandler.response("Not found", HttpStatus.NOT_FOUND, null));
        }).doFinally(fin -> log.info("[END] delete Movement"));
    }

    @GetMapping("/balance/{id}")
    public Mono<ResponseEntity<Object>> getBalance(@PathVariable("id") String id)
    {
        log.info("[INI] getBalance Movement");
        log.info(id);

        AtomicReference<Float> balance = new AtomicReference<>((float) 0);

        return dao.findAll()
                .doOnNext(movement -> {
                    if(movement.getClientId().equals(id))
                        if(movement.getTypeMovement().equals(TypeMovement.DEPOSITS))
                        {
                            balance.set((balance.get() - movement.getComissionMont()) + movement.getMont());
                        }
                        else if(movement.getTypeMovement().equals(TypeMovement.WITHDRAWALS))
                        {
                            balance.set((balance.get() - movement.getComissionMont())  - movement.getMont());
                        }
                })
                .collectList()
                .map(movements -> ResponseHandler.response("Done", HttpStatus.OK, balance.get()))
                .onErrorResume(error -> Mono.just(ResponseHandler.response(error.getMessage(), HttpStatus.BAD_REQUEST, null)))
                .switchIfEmpty(Mono.just(ResponseHandler.response("Empty", HttpStatus.NO_CONTENT, null)))
                .doFinally(fin -> log.info("[END] getBalance Movement"));
    }

}
