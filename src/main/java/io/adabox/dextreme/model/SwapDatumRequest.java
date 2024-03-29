package io.adabox.dextreme.model;

import io.adabox.dextreme.dex.base.DexType;
import lombok.*;

import java.math.BigInteger;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SwapDatumRequest {

    private String walletAddr;
    private String buyTokenPolicyID;
    private String buyTokenName;
    private String sellTokenPolicyID;
    private String sellTokenName;
    private BigInteger buyAmount;
    private BigInteger sellAmount;
    private String poolId;
    private DexType dexType;
}
