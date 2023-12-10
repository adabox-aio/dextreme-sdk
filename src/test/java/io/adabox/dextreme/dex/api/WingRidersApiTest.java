package io.adabox.dextreme.dex.api;

import io.adabox.dextreme.dex.WingRiders;
import io.adabox.dextreme.dex.base.Dex;
import io.adabox.dextreme.model.Asset;
import io.adabox.dextreme.model.LiquidityPool;
import io.adabox.dextreme.model.Token;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.adabox.dextreme.model.AssetType.ADA;

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
}
