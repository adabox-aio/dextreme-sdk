package io.adabox.dextreme.model;

import lombok.*;

import java.math.BigInteger;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Balance {

    private Asset asset;
    private BigInteger quantity;
}
