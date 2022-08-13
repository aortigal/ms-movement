package com.bank.msmovement.models.dao;

import com.bank.msmovement.models.documents.Movement;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface MovementDao extends ReactiveMongoRepository<Movement, String>
{
}
