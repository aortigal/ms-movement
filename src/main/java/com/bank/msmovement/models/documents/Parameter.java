package com.bank.msmovement.models.documents;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor
public class Parameter {

    private String id;

    private Integer code;

    private String value;

    private String name;

    private String argument;

}
