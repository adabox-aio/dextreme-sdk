package io.adabox.dextreme.dex.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import io.adabox.dextreme.dex.api.base.Api;
import io.adabox.dextreme.dex.base.DexType;
import io.adabox.dextreme.model.Asset;
import io.adabox.dextreme.model.LiquidityPool;
import io.adabox.dextreme.model.Ohlcv;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;

/**
 * Muesliswap API Class
 */
@Slf4j
@Getter
public class MuesliSwapApi extends Api {

    private final String marketOrderAddress = "addr1zyq0kyrml023kwjk8zr86d5gaxrt5w8lxnah8r6m6s4jp4g3r6dxnzml343sx8jweqn4vn3fz2kj8kgu9czghx0jrsyqqktyhv";
    private static final String BASE_URL = "https://api.muesliswap.com";
    private final String[] providers = {"muesliswap", "muesliswap_v2", "muesliswap_clp"};

    /**
     * {@link MuesliSwapApi}
     * Default Constructor
     */
    public MuesliSwapApi() {
        super(DexType.Muesliswap);
    }

    @Override
    public List<LiquidityPool> liquidityPools(Asset assetA, Asset assetB) {
        try {
            String tokenA = (assetA != null && !assetA.isLovelace()) ? assetA.getIdentifier(".") : "";
            String tokenB = (assetB != null && !assetB.isLovelace()) ? assetB.getIdentifier(".") : "";
            String url = BASE_URL + "/liquidity/pools?providers=" + String.join(",", this.providers) + "&token-a=" + tokenA + "&token-b=" + tokenB;
            HttpRequest request = HttpRequest
                    .newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> httpResponse = getClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() == 200) {
                return extractLiquidityPoolsByAsset(httpResponse.body());
            }
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    @Override
    public List<Ohlcv> priceChart(Asset assetA, Asset assetB, long timeFrom) {
        try {
            String assetAPolicyId = (assetA != null && !assetA.isLovelace()) ? assetA.getPolicyId() : "";
            String assetATokenName = (assetA != null && !assetA.isLovelace()) ? assetA.getNameHex() : "";
            String assetBPolicyId = (assetB != null && !assetB.isLovelace()) ? assetB.getPolicyId() : "";
            String assetBTokenName = (assetB != null && !assetB.isLovelace()) ? assetB.getNameHex() : "";
            String url = BASE_URL + "/charts/price?base-policy-id="+assetAPolicyId+"&base-tokenname="+assetATokenName+"&quote-policy-id="+assetBPolicyId+"&quote-tokenname="+assetBTokenName+"&interval=1h";
            HttpRequest request = HttpRequest
                    .newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> httpResponse = getClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() == 200) {
                return extractPriceChart(httpResponse.body()).stream().sorted(Comparator.comparing(Ohlcv::getTime)).filter(price -> price.getTime() >= timeFrom).toList();
            } else {
                log.error("Response Code {} for URL: {}", httpResponse.statusCode(), url);
            }
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    private List<Ohlcv> extractPriceChart(String responseBody) {
        List<Ohlcv> ohlcvChart = new ArrayList<>();
        try {
            JsonNode jsonNode = getObjectMapper().readTree(responseBody);
            for (JsonNode priceNode : jsonNode.path("data")) {
                ohlcvChart.add(new Ohlcv(priceNode.path("open").asDouble(), priceNode.path("high").asDouble(), priceNode.path("low").asDouble(), priceNode.path("close").asDouble(), priceNode.path("volume").asDouble(),  priceNode.path("time").asLong()*1000));
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return ohlcvChart;
    }

    private List<LiquidityPool> extractLiquidityPoolsByAsset(String responseBody) {
        List<LiquidityPool> liquidityPools = new ArrayList<>();
        try {
            JsonNode jsonNode = getObjectMapper().readTree(responseBody);
            if (jsonNode.isArray()) {
                for (JsonNode jsonNodeEl : jsonNode) {
                    liquidityPools.add(liquidityPoolFromResponse(jsonNodeEl));
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return liquidityPools;
    }

    private LiquidityPool liquidityPoolFromResponse(JsonNode poolData) {
        if (poolData instanceof NullNode) {
            return null;
        }
        String currencySymbolAssetA = poolData.path("tokenA").path("symbol").asText();
        String tokenNameAssetA = poolData.path("tokenA").path("address").path("name").asText();
        String tokenPolicyIdAssetA = poolData.path("tokenA").path("address").path("policyId").asText();
        int decimalsAssetA = poolData.path("tokenA").path("decimalPlaces").asInt();

        String currencySymbolAssetB = poolData.path("tokenB").path("symbol").asText();
        String tokenNameAssetB = poolData.path("tokenB").path("address").path("name").asText();
        String tokenPolicyIdAssetB = poolData.path("tokenB").path("address").path("policyId").asText();
        int decimalsAssetB = poolData.path("tokenB").path("decimalPlaces").asInt();

        Asset assetA = !currencySymbolAssetA.equals("ADA") ? new Asset(tokenPolicyIdAssetA, tokenNameAssetA, decimalsAssetA) : new Asset("", LOVELACE, 6);
        Asset assetB = !currencySymbolAssetB.equals("ADA") ? new Asset(tokenPolicyIdAssetB, tokenNameAssetB, decimalsAssetB) : new Asset("", LOVELACE, 6);

        LiquidityPool liquidityPool = new LiquidityPool(
                DexType.Muesliswap.name(),
                assetA,
                assetB,
                new BigInteger(poolData.path("tokenA").path("amount").asText()),
                new BigInteger(poolData.path("tokenB").path("amount").asText()),
                poolData.path("batcherAddress").asText(),
                getMarketOrderAddress(),
                getMarketOrderAddress()
        );
        liquidityPool.setLpToken(new Asset(poolData.path("lpToken").path("address").path("policyId").asText(), poolData.path("lpToken").path("address").path("name").asText(), 0));
        liquidityPool.setIdentifier(poolData.path("poolId").asText());
        liquidityPool.setPoolFeePercent(poolData.path("poolFee").asDouble());
        return liquidityPool;
    }
}
