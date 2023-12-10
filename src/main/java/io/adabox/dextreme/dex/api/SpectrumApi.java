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

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;

@Slf4j
@Getter
public class SpectrumApi extends Api {

    private static final String BASE_URL = "https://analytics.spectrum.fi";

    public SpectrumApi() {
        super(DexType.Spectrum);
    }

    @Override
    public List<LiquidityPool> liquidityPools() {
        try {
            HttpRequest request = HttpRequest
                    .newBuilder()
                    .uri(URI.create(BASE_URL + "/cardano/pools/overview?after=0"))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> httpResponse = getClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() == 200) {
                return resolveLPs(httpResponse.body());
            }
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    @Override
    public List<LiquidityPool> liquidityPools(Asset assetA, Asset assetB) {
        return liquidityPools().stream()
                .filter(liquidityPool ->
                        !liquidityPool.getReserveA().equals(BigInteger.ZERO) &&
                                !liquidityPool.getReserveB().equals(BigInteger.ZERO))
                .toList();
    }

    private List<LiquidityPool> resolveLPs(String responseBody) {
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

    private LiquidityPool liquidityPoolFromResponse(JsonNode poolData) throws JsonProcessingException {
        if (poolData instanceof NullNode) {
            return null;
        }
        String currencySymbolAssetA = poolData.path("lockedX").path("asset").path("currencySymbol").asText();
        String tokenNameAssetA = HexUtil.encodeHexString(poolData.path("lockedX").path("asset").path("tokenName").asText().getBytes());

        String currencySymbolAssetB = poolData.path("lockedY").path("asset").path("currencySymbol").asText();
        String tokenNameAssetB = HexUtil.encodeHexString(poolData.path("lockedY").path("asset").path("tokenName").asText().getBytes());

        Asset assetA = StringUtils.isNotBlank(currencySymbolAssetA) ? new Asset(currencySymbolAssetA, tokenNameAssetA, 0) : new Asset("", LOVELACE, 6); // TODO Get Decimals
        Asset assetB = StringUtils.isNotBlank(currencySymbolAssetB) ? new Asset(currencySymbolAssetB, tokenNameAssetB, 0) : new Asset("", LOVELACE, 6); // TODO Get Decimals

        String[] lpTokenDetails = poolData.path("lpPolicyId-assetId").asText().split("-");

        String reserveA = poolData.path("lockedX").path("amount").asText();
        String reserveB = poolData.path("lockedY").path("amount").asText();

        //        liquidityPool.setPoolFeePercent((json.path("feeSettings").path("barFee").asDouble() + json.path("feeSettings").path("liqFee").asDouble()) / 100);
        LiquidityPool liquidityPool = new LiquidityPool(
                DexType.Spectrum.name(),
                assetA,
                assetB,
                new BigInteger(StringUtils.isNotBlank(reserveA) ? reserveA : "0"), // TODO get Liquidity
                new BigInteger(StringUtils.isNotBlank(reserveB) ? reserveB : "0"), // TODO get Liquidity
                poolData.path("poolValidatorUtxoAddress").asText(),
                poolData.path("orderValidatorUtxoAddress").asText(),
                poolData.path("orderValidatorUtxoAddress").asText()
        );
        liquidityPool.setLpToken(new Asset(lpTokenDetails[0], lpTokenDetails[1], 0));
        return liquidityPool;
    }
}
