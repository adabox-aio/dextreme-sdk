package io.adabox.dextreme.dex.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import io.adabox.dextreme.dex.api.base.Api;
import io.adabox.dextreme.dex.base.DexType;
import io.adabox.dextreme.model.Asset;
import io.adabox.dextreme.model.LiquidityPool;
import io.adabox.dextreme.utils.AESUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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
public class MinSwapApi extends Api {

    private final String marketOrderAddress = "addr1wxn9efv2f6w82hagxqtn62ju4m293tqvw0uhmdl64ch8uwc0h43gt";
    private final String limitOrderAddress = "addr1zxn9efv2f6w82hagxqtn62ju4m293tqvw0uhmdl64ch8uw6j2c79gy9l76sdg0xwhd7r0c0kna0tycz4y5s6mlenh8pq6s3z70";
    private static final String BASE_URL = "https://monorepo-mainnet-prod.minswap.org/graphql";
    private static final String AES_KEY = "22eaca439bfd89cf125827a7a33fe3970d735dbfd5d84f19dd95820781fc47be";

    public MinSwapApi() {
        super(DexType.Minswap);
    }

    @Override
    public List<LiquidityPool> liquidityPools(Asset assetA, Asset assetB) {
        try {
            if (assetA != null && assetB != null) {
                return poolsByPair(assetA, assetB);
            }
            return getPaginatedLPResponse(0, 20, assetA);
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<LiquidityPool> getPaginatedLPResponse(int page, int maxPerPage, Asset assetA) throws IOException, InterruptedException {
        String query = "{\"query\":\"query PoolsByAsset($asset: InputAsset!, $limit: Int, $offset: Int) {" +
                "  poolsByAsset(asset: $asset, limit: $limit, offset: $offset) {" +
                "    assetA {" +
                "      currencySymbol" +
                "      tokenName" +
                "      ...allMetadata" +
                "    }" +
                "    assetB {" +
                "      currencySymbol" +
                "      tokenName" +
                "      ...allMetadata" +
                "    }" +
                "    reserveA" +
                "    reserveB" +
                "    lpAsset {" +
                "      currencySymbol" +
                "      tokenName" +
                "    }" +
                "    totalLiquidity" +
                "  }" +
                "}" +
                "fragment allMetadata on Asset {" +
                "  metadata {" +
                "    name" +
                "    decimals" +
                "  }" +
                "}\"," +
                "\"variables\":" + getPoolsByAssetVariables(assetA, maxPerPage, page) + "}";
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
        liquidityPools.addAll(getPaginatedLPResponse(page + 1, maxPerPage, assetA));
        return liquidityPools;
    }


    private String getPoolsByAssetVariables(Asset assetA, int maxPerPage, int page) {
        return "{" +
                "\"asset\": {" +
                "\"currencySymbol\":\"" + (assetA != null ? assetA.getPolicyId() : "") + "\"," +
                "\"tokenName\":\"" + (assetA != null ? assetA.getNameHex() : "") +
                "\"}," +
                "\"limit\":" + maxPerPage + "," +
                "\"offset\":" + (page * maxPerPage) +
                "}";
    }

    private List<LiquidityPool> extractLiquidityPoolsByAsset(String responseBody) {
        List<LiquidityPool> liquidityPools = new ArrayList<>();
        try {
            JsonNode responseNode = getObjectMapper().readTree(responseBody);
            JsonNode poolsNode = getObjectMapper().readTree(decryptResponse(responseNode.path("data").path("encryptedData")));
            for (JsonNode poolNode : poolsNode.path("poolsByAsset")) {
                LiquidityPool liquidityPool = liquidityPoolFromResponse(poolNode);
                liquidityPools.add(liquidityPool);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return liquidityPools;
    }

    private List<LiquidityPool> extractLiquidityPoolsByPair(String responseBody) {
        List<LiquidityPool> liquidityPools = new ArrayList<>();
        try {
            JsonNode responseNode = getObjectMapper().readTree(responseBody);
            JsonNode poolsNode = getObjectMapper().readTree(decryptResponse(responseNode.path("data").path("encryptedData")));
            LiquidityPool liquidityPool = liquidityPoolFromResponse(poolsNode.path("poolByPair"));
            if (liquidityPool != null) {
                liquidityPools.add(liquidityPool);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return liquidityPools;
    }

    private List<LiquidityPool> poolsByPair(Asset assetA, Asset assetB) throws IOException, InterruptedException {
        String query = "{\"query\":\"query PoolByPair($pair: InputPoolByPair!) {" +
                "poolByPair(pair: $pair) {" +
                "    assetA {" +
                "        currencySymbol" +
                "        tokenName" +
                "        isVerified" +
                "        ...allMetadata" +
                "    }" +
                "    assetB {" +
                "        currencySymbol" +
                "        tokenName" +
                "        isVerified" +
                "        ...allMetadata" +
                "    }" +
                "    reserveA" +
                "    reserveB" +
                "    lpAsset {" +
                "        currencySymbol" +
                "        tokenName" +
                "    }" +
                "    totalLiquidity" +
                "    profitSharing {" +
                "        feeTo" +
                "    }" +
                "}" +
                "}" +
                "fragment allMetadata on Asset {" +
                "metadata {" +
                "    name" +
                "    ticker" +
                "    url" +
                "    decimals" +
                "    description" +
                "}" +
                "}\"," +
                "\"variables\":" + getPoolsByPairVariables(assetA, assetB) + "}";
        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(query))
                .build();
        HttpResponse<String> httpResponse = getClient().send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = httpResponse.body();
        return extractLiquidityPoolsByPair(responseBody); // Placeholder, replace with actual result
    }

    private String getPoolsByPairVariables(Asset assetA, Asset assetB) {
        return "{\"pair\":{" +
                "\"assetA\":" +
                "{\"currencySymbol\":\"" + assetA.getPolicyId() + "\",\"tokenName\":\"" + assetA.getNameHex() + "\"}," +
                "\"assetB\":" +
                "{\"currencySymbol\":\"" + assetB.getPolicyId() + "\",\"tokenName\":\"" + assetB.getNameHex() + "\"}}}";
    }

    private LiquidityPool liquidityPoolFromResponse(JsonNode poolData) {
        if (poolData instanceof NullNode) {
            return null;
        }
        String currencySymbolAssetA = poolData.path("assetA").path("currencySymbol").asText();
        String tokenNameAssetA = poolData.path("assetA").path("tokenName").asText();
        int decimalsAssetA = poolData.path("assetA").path("metadata").path("decimals").asInt();

        String currencySymbolAssetB = poolData.path("assetB").path("currencySymbol").asText();
        String tokenNameAssetB = poolData.path("assetB").path("tokenName").asText();
        int decimalsAssetB = poolData.path("assetB").path("metadata").path("decimals").asInt();

        Asset assetA = currencySymbolAssetA.isEmpty() ? new Asset("", LOVELACE, 6) : new Asset(currencySymbolAssetA, tokenNameAssetA, decimalsAssetA);
        Asset assetB = currencySymbolAssetB.isEmpty() ? new Asset("", LOVELACE, 6) : new Asset(currencySymbolAssetB, tokenNameAssetB, decimalsAssetB);

        LiquidityPool liquidityPool = new LiquidityPool(
                DexType.Minswap.name(),
                assetA,
                assetB,
                new BigInteger(poolData.path("reserveA").asText()),
                new BigInteger(poolData.path("reserveB").asText()),
                "",
                getMarketOrderAddress(),
                getLimitOrderAddress()
        );

        liquidityPool.setLpToken(new Asset(poolData.path("lpAsset").path("currencySymbol").asText(), poolData.path("lpAsset").path("tokenName").asText(), 0));
        liquidityPool.setPoolFeePercent(0.3);
        liquidityPool.setIdentifier(liquidityPool.getLpToken().getIdentifier(""));
        return liquidityPool;
    }

    private String decryptResponse(JsonNode encryptedResponse) {
        return AESUtils.decrypt(encryptedResponse.asText(), AES_KEY); // Placeholder, replace with actual result
    }
}
