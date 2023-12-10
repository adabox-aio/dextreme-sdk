package io.adabox.dextreme.dex.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import io.adabox.dextreme.dex.api.base.Api;
import io.adabox.dextreme.dex.base.DexType;
import io.adabox.dextreme.model.Asset;
import io.adabox.dextreme.model.LiquidityPool;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;

@Slf4j
@Getter
public class WingRidersApi extends Api {

    private final String orderAddress = "addr1wxr2a8htmzuhj39y2gq7ftkpxv98y2g67tg8zezthgq4jkg0a4ul4";
    private static final String BASE_URL = "https://api.mainnet.wingriders.com/graphql";

    public WingRidersApi() {
        super(DexType.WingRiders);
    }

    @Override
    public List<LiquidityPool> liquidityPools() {
        try {
            String query = "{\"query\":\"query LiquidityPoolsWithMarketData($input: PoolsWithMarketdataInput) {" +
                    "poolsWithMarketdata(input: $input) {" +
                    "                        ...LiquidityPoolFragment" +
                    "                    }" +
                    "                }" +
                    "                fragment LiquidityPoolFragment on PoolWithMarketdata {" +
                    "                    issuedShareToken {" +
                    "                        policyId" +
                    "                        assetName" +
                    "                        quantity" +
                    "                    }" +
                    "                    tokenA {" +
                    "                        policyId" +
                    "                        assetName" +
                    "                        quantity" +
                    "                    }" +
                    "                    tokenB {" +
                    "                        policyId" +
                    "                        assetName" +
                    "                        quantity" +
                    "                    }" +
                    "                    treasuryA" +
                    "                    treasuryB" +
                    "                    _utxo {" +
                    "                        address" +
                    "                    }" +
                    "                }\"," +
                    "\"variables\":{" +
                    "  \"input\":{" +
                    "    \"sort\": true" +
                    "  }" +
                    "}}";
            HttpRequest request = HttpRequest
                    .newBuilder()
                    .uri(URI.create(BASE_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(query))
                    .build();
            HttpResponse<String> httpResponse = getClient().send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = httpResponse.body();
            return extractLiquidityPoolsByAsset(responseBody);
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    @Override
    public List<LiquidityPool> liquidityPools(Asset assetA, Asset assetB) {
        return liquidityPools().stream()
                .filter(liquidityPool -> {
                            if (assetB != null) {
                                return (liquidityPool.getAssetA().getAssetName().equals(assetB.getAssetName()) && liquidityPool.getAssetB().getAssetName().equals(assetA.getAssetName())) ||
                                        (liquidityPool.getAssetA().getAssetName().equals(assetA.getAssetName()) && liquidityPool.getAssetB().getAssetName().equals(assetB.getAssetName()));
                            } else {
                                return liquidityPool.getAssetA().getAssetName().equals(assetA.getAssetName()) || liquidityPool.getAssetB().getAssetName().equals(assetA.getAssetName());
                            }
                        }
                ).toList();
    }

    private List<LiquidityPool> extractLiquidityPoolsByAsset(String responseBody) {
        List<LiquidityPool> liquidityPools = new ArrayList<>();
        try {
            JsonNode responseNode = getObjectMapper().readTree(responseBody);
            JsonNode poolsNode = responseNode.path("data").path("poolsWithMarketdata");
            for (JsonNode poolNode : poolsNode) {
                LiquidityPool liquidityPool = liquidityPoolFromResponse(poolNode);
                liquidityPools.add(liquidityPool);
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
        Asset assetA = StringUtils.isNotBlank(poolData.path("tokenA").path("policyId").asText())
                ? new Asset(poolData.path("tokenA").path("policyId").asText(), poolData.path("tokenA").path("assetName").asText(), 0) // TODO Get Decimals
                : new Asset("", LOVELACE, 6);
        Asset assetB = StringUtils.isNotBlank(poolData.path("tokenB").path("policyId").asText())
                ? new Asset(poolData.path("tokenB").path("policyId").asText(), poolData.path("tokenB").path("assetName").asText(), 0) // TODO Get Decimals
                : new Asset("", LOVELACE, 6);

        LiquidityPool liquidityPool = new LiquidityPool(
                DexType.WingRiders.name(),
                assetA,
                assetB,
                new BigInteger(poolData.path("tokenA").path("quantity").asText()).subtract(new BigInteger(poolData.path("treasuryA").asText())),
                new BigInteger(poolData.path("tokenB").path("quantity").asText()).subtract(new BigInteger(poolData.path("treasuryB").asText())),
                poolData.path("_utxo").path("address").asText(),
                getOrderAddress(),
                getOrderAddress()
        );
        liquidityPool.setLpToken(new Asset(poolData.path("issuedShareToken").path("policyId").asText(), poolData.path("issuedShareToken").path("assetName").asText(), 0));
        liquidityPool.setPoolFeePercent(0.35);
        return liquidityPool;
    }
}
