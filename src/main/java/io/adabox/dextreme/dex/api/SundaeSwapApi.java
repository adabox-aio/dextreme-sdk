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
public class SundaeSwapApi extends Api {

    private final String marketOrderAddress = "addr1wxaptpmxcxawvr3pzlhgnpmzz3ql43n2tc8mn3av5kx0yzs09tqh8";
    private final String poolAddress = "addr1w9qzpelu9hn45pefc0xr4ac4kdxeswq7pndul2vuj59u8tqaxdznu";
    private static final String BASE_URL = "https://stats.sundaeswap.finance/graphql";

    public SundaeSwapApi() {
        super(DexType.Minswap);
    }

    @Override
    public List<LiquidityPool> liquidityPools(Asset assetA, Asset assetB) {
        try {
            String tokenA = (assetA != null && !assetA.isLovelace()) ? assetA.getIdentifier(".") : "";
            String tokenB = (assetB != null && !assetB.isLovelace()) ? assetB.getIdentifier(".") : "";
            return getPaginatedResponse(0, 100, tokenA, tokenB).stream()
                    .filter(liquidityPool -> {
                                if (assetA != null && assetB != null) {
                                    return (liquidityPool.getAssetA().getAssetName().equals(assetB.getAssetName()) && liquidityPool.getAssetB().getAssetName().equals(assetA.getAssetName())) ||
                                            (liquidityPool.getAssetA().getAssetName().equals(assetA.getAssetName()) && liquidityPool.getAssetB().getAssetName().equals(assetB.getAssetName()));
                                } else if (assetA != null) {
                                    return liquidityPool.getAssetA().getAssetName().equals(assetA.getAssetName()) || liquidityPool.getAssetB().getAssetName().equals(assetA.getAssetName());
                                }
                                return true;
                            }
                    ).toList();
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<LiquidityPool> getPaginatedResponse(int page, int maxPerPage, String tokenA, String tokenB) throws IOException, InterruptedException {
        String query = "{\"query\":\"query getPoolsByAssetIds($assetIds: [String!]!, $pageSize: Int, $page: Int) {" +
                "  pools(assetIds: $assetIds, pageSize: $pageSize, page: $page) {" +
                "    ...PoolFragment" +
                "  }" +
                "}" +
                "fragment PoolFragment on Pool {" +
                "  assetA {" +
                "    ...AssetFragment" +
                "  }" +
                "  assetB {" +
                "    ...AssetFragment" +
                "  }" +
                "  assetLP {" +
                "    ...AssetFragment" +
                "  }" +
                "  name" +
                "  fee" +
                "  quantityA" +
                "  quantityB" +
                "  quantityLP" +
                "  ident" +
                "  assetID" +
                "}" +
                "fragment AssetFragment on Asset {" +
                "  assetId" +
                "  decimals" +
                "}\"," +
                "\"variables\":{" +
                "  \"page\":" + page + "," +
                "  \"pageSize\":" + maxPerPage + "," +
                "  \"assetIds\": [\"" + (StringUtils.isNotBlank(tokenB) ? tokenB : tokenA) + "\"]" +
                "}}";
        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(query))
                .build();
        HttpResponse<String> httpResponse = getClient().send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = httpResponse.body();
        List<LiquidityPool> liquidityPools = extractLiquidityPoolsByAsset(responseBody);
        if (liquidityPools.size() < maxPerPage) {
            return liquidityPools;
        }
        liquidityPools.addAll(getPaginatedResponse(page + 1, maxPerPage, tokenA, tokenB));
        return liquidityPools;
    }

    private List<LiquidityPool> extractLiquidityPoolsByAsset(String responseBody) {
        List<LiquidityPool> liquidityPools = new ArrayList<>();
        try {
            JsonNode responseNode = getObjectMapper().readTree(responseBody);
            JsonNode poolsNode = responseNode.path("data").path("pools");
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
        String assetAId = poolData.path("assetA").path("assetId").asText();
        int assetADecimals = poolData.path("assetA").path("decimals").asInt();

        String assetBId = poolData.path("assetB").path("assetId").asText();
        int assetBDecimals = poolData.path("assetB").path("decimals").asInt();

        Asset assetA = StringUtils.isNotBlank(assetAId) ? Asset.fromId(assetAId, assetADecimals) : new Asset("", LOVELACE, 6);
        Asset assetB = StringUtils.isNotBlank(assetBId) ? Asset.fromId(assetBId, assetBDecimals) : new Asset("", LOVELACE, 6);

        LiquidityPool liquidityPool = new LiquidityPool(
                DexType.Sundaeswap.name(),
                assetA,
                assetB,
                new BigInteger(poolData.path("quantityA").asText()),
                new BigInteger(poolData.path("quantityB").asText()),
                getPoolAddress(),
                getMarketOrderAddress(),
                getMarketOrderAddress()
        );
        liquidityPool.setLpToken(Asset.fromId(poolData.path("assetLP").path("assetId").asText(), 0));
        liquidityPool.setIdentifier(poolData.path("ident").asText());
        liquidityPool.setPoolFeePercent(poolData.path("fee").asDouble());
        return liquidityPool;
    }
}
