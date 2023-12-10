package io.adabox.dextreme.dex.api;

import io.adabox.dextreme.dex.Sundaeswap;
import io.adabox.dextreme.dex.base.Dex;
import io.adabox.dextreme.model.LiquidityPool;
import io.adabox.dextreme.model.Token;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class SundaeswapApiTest {

    private final Dex sundaeswap = new Sundaeswap();

    @Test
    public void sundaeswapGetLP() {
        Map<String, LiquidityPool> liquidityPoolList = sundaeswap.getLiquidityPoolMap();
        System.out.println(liquidityPoolList);
    }

    @Test
    public void sundaeswapGetTokens() {
        List<Token> assetList = sundaeswap.getTokens(true).values().stream().toList();
        System.out.println(assetList);
    }
}
