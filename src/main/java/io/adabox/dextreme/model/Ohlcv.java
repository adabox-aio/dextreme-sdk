package io.adabox.dextreme.model;

import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Ohlcv {

    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Double volume;
    private Long time;
}
