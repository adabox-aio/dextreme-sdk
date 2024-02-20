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
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;

/**
 * Sundaeswap API Class
 */
@Slf4j
@Getter
public class SundaeswapApi extends Api {

    private final String marketOrderAddress = "addr1wxaptpmxcxawvr3pzlhgnpmzz3ql43n2tc8mn3av5kx0yzs09tqh8";
    private final String poolAddress = "addr1w9qzpelu9hn45pefc0xr4ac4kdxeswq7pndul2vuj59u8tqaxdznu";
    private static final String BASE_URL = "https://stats.sundaeswap.finance";
    private static final long DAY_IN_MS = 1000 * 60 * 60 * 24;

    public SundaeswapApi() {
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

    @Override
    public List<Ohlcv> priceChart(Asset assetA, Asset assetB, long timeFrom) {
        try {
            LiquidityPool liquidityPool = liquidityPools(assetA, assetB).getFirst();
            Asset lpAsset = liquidityPool.getLpToken();
            if (lpAsset != null && lpAsset.getNameHex().startsWith("6c")) {
                lpAsset.setNameHex(lpAsset.getNameHex().substring(2));
            }
            String lpToken = (lpAsset != null && !lpAsset.isLovelace()) ? lpAsset.getIdentifier(".") : "";
            Date date = new Date(); // TODO Fix Date Handling using timeFrom
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            List<Ohlcv> ohlcvChart = new ArrayList<>(requestPerDate(lpToken, calendar.get(Calendar.YEAR), (calendar.get(Calendar.MONTH) + 1)));
            calendar.add(Calendar.DAY_OF_MONTH, -7);
            ohlcvChart.addAll(requestPerDate(lpToken, calendar.get(Calendar.YEAR), (calendar.get(Calendar.MONTH) + 1)));
            return ohlcvChart.stream().sorted(Comparator.comparing(Ohlcv::getTime)).filter(price -> price.getTime() >= timeFrom).toList();
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    private List<Ohlcv> requestPerDate(String lpToken, int year, int month) throws IOException, InterruptedException {
        String url = BASE_URL + "/api/ticks/" + lpToken + "/" + year + "/" + (month < 10 ? ("0" + month) : (month));
        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .GET()
                .build();
        HttpResponse<String> httpResponse = getClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (httpResponse.statusCode() == 200) {
                return extractPriceChart(httpResponse.body());
        } else {
            log.error("Response Code {} for URL: {}", httpResponse.statusCode(), url);
        }
        return Collections.emptyList();
    }

    private List<Ohlcv> extractPriceChart(String responseBody) {
        List<Ohlcv> ohlcvChart = new ArrayList<>();
        try {
            JsonNode jsonNode = getObjectMapper().readTree(responseBody);
            for (JsonNode priceNode : jsonNode.path("ticks")) {
                ohlcvChart.add(new Ohlcv(priceNode.path("open").asDouble(), priceNode.path("high").asDouble(), priceNode.path("low").asDouble(), priceNode.path("close").asDouble(), priceNode.path("volume").asDouble(), priceNode.path("time").asLong() * 1000));
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return ohlcvChart;
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
                .uri(URI.create(BASE_URL + "/graphql"))
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
