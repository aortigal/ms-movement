package com.bank.msmovement.models.documents;

import com.bank.msmovement.models.emus.TypeMovement;
import com.bank.msmovement.models.emus.TypePasiveMovement;
import com.bank.msmovement.models.utils.Audit;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Document(collection = "movements")
public class Movement extends Audit
{
    @Id
    private String id;
    @NotNull(message = "pasiveId must not be null")
    private String pasiveId;
    @NotNull(message = "clientId must not be null")
    private String clientId;
    @NotNull(message = "typeMovement must not be null")
    private TypeMovement typeMovement;
    private TypePasiveMovement typePasiveMovement;
    private float mont;
    private float comissionMont;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy",timezone = "GMT-05:00")
    private LocalDateTime created;
}
