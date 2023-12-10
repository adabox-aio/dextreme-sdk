package io.adabox.dextreme.dex.api.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.adabox.dextreme.dex.base.DexType;
import io.adabox.dextreme.model.Asset;
import io.adabox.dextreme.model.LiquidityPool;
import lombok.Getter;

import java.net.http.HttpClient;
import java.util.List;


/**
 * API Base Class
 */
@Getter
public abstract class Api {

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DexType dexType;

    /**
     * Api Constructor
     * @param dexType DEX Type Enum
     */
    public Api(DexType dexType) {
        this.dexType = dexType;
    }

    /**
     * Fetch all liquidity pools
     */
    public List<LiquidityPool> liquidityPools() {
        return this.liquidityPools(null, null);
    }

    /**
     * Fetch all liquidity pools matching assetA &amp; assetB.
     */
    public abstract List<LiquidityPool> liquidityPools(Asset assetA, Asset assetB);
}
