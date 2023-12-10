package io.adabox.dextreme.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigInteger;

@Setter
@Getter
@ToString
@NoArgsConstructor
public class LiquidityPool {

    private String dex;
    private Asset assetA;
    private Asset assetB;
    private BigInteger reserveA;
    private BigInteger reserveB;
    private String address;
    private String marketOrderAddress;
    private String limitOrderAddress;

    @JsonIgnore
    private Asset lpToken;
    private String identifier = "";
    private double poolFeePercent = 0.0;

    @JsonIgnore
    private UTxO uTxO;

    public LiquidityPool(String dex, Asset assetA, Asset assetB, BigInteger reserveA, BigInteger reserveB,
                         String address, String marketOrderAddress, String limitOrderAddress) {
        this.dex = dex;
        this.assetA = assetA;
        this.assetB = assetB;
        this.reserveA = reserveA;
        this.reserveB = reserveB;
        this.address = address;
        this.marketOrderAddress = marketOrderAddress;
        this.limitOrderAddress = limitOrderAddress;
    }

    @JsonProperty("uuid")
    public String getUuid() {
        return dex + "." + getPair() + "." + identifier;
    }

    public String getPair() {
        return assetA.getAssetName() + "/" + assetB.getAssetName();
    }

    public double getPrice() {
        double adjustedReserveA = reserveA.doubleValue() / Math.pow(10, assetA.getDecimals());
        double adjustedReserveB = reserveB.doubleValue() / Math.pow(10, assetB.getDecimals());

        return adjustedReserveA / adjustedReserveB;
    }

    @JsonProperty("TVL")
    public double getTotalValueLocked() {
        double assetADividedByDecimals = reserveA.doubleValue() / Math.pow(10, assetA.getDecimals());
        double assetBDividedByDecimals = reserveB.doubleValue() / Math.pow(10, assetB.getDecimals());
        if (assetA.isLovelace()) {
            return assetADividedByDecimals + assetBDividedByDecimals * getPrice();
        }
        return (assetADividedByDecimals) * getPrice() * assetBDividedByDecimals * (1.0 / getPrice());
    }
}