package io.adabox.dextreme.dex.api;

import io.adabox.dextreme.dex.Muesliswap;
import io.adabox.dextreme.dex.base.Dex;
import io.adabox.dextreme.model.Asset;
import io.adabox.dextreme.model.LiquidityPool;
import io.adabox.dextreme.model.Token;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.adabox.dextreme.model.AssetType.ADA;

public class MuesliSwapApiTest {

    private final Dex muesliswap = new Muesliswap();

    @Test
    public void muesliswapGetLP() {
        Asset assetA = ADA.getAsset();
        Asset assetB = new Asset("f66d78b4a3cb3d37afa0ec36461e51ecbde00f26c8f0a68f94b69880", "69555344", 6);
        List<LiquidityPool> liquidityPoolList = muesliswap.getLiquidityPools(assetA, assetB);
        System.out.println(liquidityPoolList);
    }

    @Test
    public void muesliswapGetTokens() {
        List<Token> assetList = muesliswap.getTokens(true).values().stream().toList();
        System.out.println(assetList);
    }
}
