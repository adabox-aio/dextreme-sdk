package io.adabox.dextreme.dex.api;

import io.adabox.dextreme.dex.Sundaeswap;
import io.adabox.dextreme.dex.base.Dex;
import io.adabox.dextreme.model.Asset;
import io.adabox.dextreme.model.LiquidityPool;
import io.adabox.dextreme.model.Ohlcv;
import io.adabox.dextreme.model.Token;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.adabox.dextreme.model.AssetType.*;

public class SundaeswapApiTest {

    private final Dex sundaeswap = new Sundaeswap();

    @Test
    public void sundaeSwapGetLP() {
        Map<String, LiquidityPool> liquidityPoolList = sundaeswap.getLiquidityPoolMap();
        System.out.println(liquidityPoolList);
    }

    @Test
    public void sundaeSwapGetTokens() {
        List<Token> assetList = sundaeswap.getTokens(true).values().stream().toList();
        System.out.println(assetList);
    }

    @Test
    public void sundaeSwapPriceChart() {
        Asset assetA = ADA.getAsset();
        Asset assetB = SUNDAE.getAsset();
        long currentTime = System.currentTimeMillis();
        long timeFrom = currentTime - (7 * 1000 * 60 * 60 * 24);
        List<Ohlcv> ohlcvChart = sundaeswap.getPriceChart(assetA, assetB, timeFrom);
        Assertions.assertFalse(ohlcvChart.isEmpty());
    }
}
