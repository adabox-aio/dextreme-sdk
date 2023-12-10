package io.adabox.dextreme.dex.api.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.adabox.dextreme.dex.base.DexType;
import io.adabox.dextreme.model.Asset;
import io.adabox.dextreme.model.LiquidityPool;
import lombok.Getter;

import java.net.http.HttpClient;
import java.util.List;


@Getter
public abstract class Api {

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DexType identifier;

    public Api(DexType identifier) {
        this.identifier = identifier;
    }

    public List<LiquidityPool> liquidityPools() {
        return this.liquidityPools(null, null);
    }

    /**
     * Fetch all liquidity pools matching assetA & assetB.
     */
    public abstract List<LiquidityPool> liquidityPools(Asset assetA, Asset assetB);
}
