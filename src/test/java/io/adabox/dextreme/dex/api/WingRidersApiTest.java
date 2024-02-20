package io.adabox.dextreme.dex.api;

import io.adabox.dextreme.dex.WingRiders;
import io.adabox.dextreme.dex.base.Dex;
import io.adabox.dextreme.model.Asset;
import io.adabox.dextreme.model.LiquidityPool;
import io.adabox.dextreme.model.Ohlcv;
import io.adabox.dextreme.model.Token;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.adabox.dextreme.model.AssetType.ADA;
import static io.adabox.dextreme.model.AssetType.GERO;

public class WingRidersApiTest {

    private final Dex wingRiders = new WingRiders();

    @Test
    public void wingRidersGetLP() {
        Asset assetA = ADA.getAsset();
        Asset assetB = new Asset("f66d78b4a3cb3d37afa0ec36461e51ecbde00f26c8f0a68f94b69880", "69555344", 6);
        List<LiquidityPool> liquidityPoolList = wingRiders.getLiquidityPools(assetA, null);
        System.out.println(liquidityPoolList);
    }

    @Test
    public void wingRidersGetTokens() {
        List<Token> assetList = wingRiders.getTokens(true).values().stream().toList();
        System.out.println(assetList);
    }

    @Test
    public void wingRidersPriceChart() {
        Asset assetA = ADA.getAsset();
        Asset assetB = GERO.getAsset();
        long currentTime = System.currentTimeMillis();
        long timeFrom = currentTime - (7 * 1000 * 60 * 60 * 24);
        List<Ohlcv> ohlcvChart = wingRiders.getPriceChart(assetA, assetB, timeFrom);
        Assertions.assertFalse(ohlcvChart.isEmpty());
    }
}
