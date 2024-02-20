package io.adabox.dextreme.dex.api;

import io.adabox.dextreme.dex.VyFinance;
import io.adabox.dextreme.dex.base.Dex;
import io.adabox.dextreme.model.Asset;
import io.adabox.dextreme.model.LiquidityPool;
import io.adabox.dextreme.model.Ohlcv;
import io.adabox.dextreme.model.Token;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.adabox.dextreme.model.AssetType.ADA;
import static io.adabox.dextreme.model.AssetType.SUNDAE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class VyFinanceApiTest {

    private final Dex vyFinance = new VyFinance();

    @Test
    public void vyFinanceGetLP() {
        Asset assetA = ADA.getAsset();
        Asset assetB = new Asset("f66d78b4a3cb3d37afa0ec36461e51ecbde00f26c8f0a68f94b69880", "69555344", 6);
        List<LiquidityPool> liquidityPoolList = vyFinance.getLiquidityPools(assetA, null);
        System.out.println(liquidityPoolList);
    }

    @Test
    public void vyFinanceGetTokens() {
        List<Token> assetList = vyFinance.getTokens(true).values().stream().toList();
        assertNotNull(assetList);
        assertFalse(assetList.isEmpty());
    }

    @Test
    public void vyFinancePriceChart() {
        Asset assetA = ADA.getAsset();
        Asset assetB = SUNDAE.getAsset();
        long currentTime = System.currentTimeMillis();
        long timeFrom = currentTime - (7 * 1000 * 60 * 60 * 24);
        List<Ohlcv> ohlcvChart = vyFinance.getPriceChart(assetA, assetB, timeFrom);
        Assertions.assertFalse(ohlcvChart.isEmpty());
    }
}
