package io.adabox.dextreme.dex.api;

import com.bloxbean.cardano.client.util.HexUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.NoSuchElementException;

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;


@Slf4j
@Getter
public class VyFinanceApi extends Api {

    private static final String BASE_URL = "https://api.vyfi.io";

    public VyFinanceApi() {
        super(DexType.VyFinance);
    }

    @Override
    public List<LiquidityPool> liquidityPools(Asset assetA, Asset assetB) {
        try {
            String urlSuffix;
            if (assetA == null ) {
                urlSuffix = "/lp?networkId=1&v2=true";
            } else {
                String assetAId = assetA.isLovelace() ? LOVELACE : assetA.getIdentifier("");
                String assetBId = (assetB != null && !assetB.isLovelace()) ? assetB.getIdentifier("") : LOVELACE;

                urlSuffix = (assetB != null ? "/lp?networkId=1&v2=true&tokenAUnit=" + assetAId + "&tokenBUnit=" + assetBId : "/lp?networkId=1&v2=true");
            }
            HttpRequest request = HttpRequest
                    .newBuilder()
                    .uri(URI.create(BASE_URL + urlSuffix))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> httpResponse = getClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() == 200) {
                return extractLiquidityPoolsByAsset(httpResponse.body()).stream()
                        .filter(liquidityPool ->
                                !liquidityPool.getReserveA().equals(BigInteger.ZERO) &&
                                        !liquidityPool.getReserveB().equals(BigInteger.ZERO))
                        .toList();
            }
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    private List<LiquidityPool> extractLiquidityPoolsByAsset(String responseBody) {
        List<LiquidityPool> liquidityPools = new ArrayList<>();
        try {
            JsonNode jsonNode = getObjectMapper().readTree(responseBody);
            if (jsonNode.isArray()) {
                for (JsonNode jsonNodeEl : jsonNode) {
                    LiquidityPool liquidityPool = liquidityPoolFromResponse(jsonNodeEl);
                    if (liquidityPool!= null) {
                        liquidityPools.add(liquidityPool);
                    } else {
                      log.warn("Missed LP"); // TODO Fix
                    }
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return liquidityPools;
    }

    private LiquidityPool liquidityPoolFromResponse(JsonNode poolData) throws JsonProcessingException {
        if (poolData instanceof NullNode) {
            return null;
        }
        JsonNode json = getObjectMapper().readTree(poolData.path("json").asText());

        String currencySymbolAssetA = json.path("aAsset").path("currencySymbol").asText();
        String strAssetA = json.path("aAsset").path("tokenName").asText();
        String tokenNameAssetA;
        if (strAssetA.contains("0x")) {
            String[] splittedA = strAssetA.split("0x");
            tokenNameAssetA = splittedA.length > 1 ? splittedA[1] : splittedA[0];
        } else {
           tokenNameAssetA = HexUtil.encodeHexString(strAssetA.getBytes());
        }



        String currencySymbolAssetB = json.path("bAsset").path("currencySymbol").asText();
        String strAssetB = json.path("bAsset").path("tokenName").asText();
        String tokenNameAssetB;
        if (strAssetB.contains("0x")) {
            String[] splittedB = strAssetB.split("0x");
            tokenNameAssetB = splittedB.length > 1 ? splittedB[1] : splittedB[0];
        } else {
            tokenNameAssetB = HexUtil.encodeHexString(strAssetB.getBytes());
        }

        Asset assetA = StringUtils.isNotBlank(currencySymbolAssetA) ? new Asset(currencySymbolAssetA, tokenNameAssetA, 0) : new Asset("", LOVELACE, 6); // TODO Get Decimals
        Asset assetB = StringUtils.isNotBlank(currencySymbolAssetB) ? new Asset(currencySymbolAssetB, tokenNameAssetB, 0) : new Asset("", LOVELACE, 6); // TODO Get Decimals

        if (StringUtils.isBlank(poolData.path("tokenAQuantity").asText()) || StringUtils.isBlank(poolData.path("tokenBQuantity").asText())) {
            List<LiquidityPool> lps = liquidityPools(assetA, assetB);
            try {
                return lps.getFirst();
            } catch (NoSuchElementException e) {
                log.error(e.getMessage(), e); // TODO Fix
                return null;
            }
        }
        String[] lpTokenDetails = poolData.path("lpPolicyId-assetId").asText().split("-");

        String reserveA = poolData.path("tokenAQuantity").asText();
        String reserveB = poolData.path("tokenBQuantity").asText();

        LiquidityPool liquidityPool = new LiquidityPool(
                DexType.VyFinance.name(),
                assetA,
                assetB,
                new BigInteger(reserveA),
                new BigInteger(reserveB),
                poolData.path("poolValidatorUtxoAddress").asText(),
                poolData.path("orderValidatorUtxoAddress").asText(),
                poolData.path("orderValidatorUtxoAddress").asText()
        );
        liquidityPool.setLpToken(new Asset(lpTokenDetails[0], lpTokenDetails[1], 0));
        liquidityPool.setPoolFeePercent((json.path("feesSettings").path("barFee").asDouble() + json.path("feesSettings").path("liqFee").asDouble()) / 100);
        liquidityPool.setIdentifier(liquidityPool.getLpToken().getIdentifier(""));
        return liquidityPool;
    }
}
