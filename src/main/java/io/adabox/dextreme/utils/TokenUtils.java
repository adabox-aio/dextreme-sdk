package io.adabox.dextreme.utils;

import io.adabox.dextreme.component.TokenRegistry;
import io.adabox.dextreme.model.Asset;
import io.adabox.dextreme.model.LiquidityPool;
import io.adabox.dextreme.model.Token;
import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@UtilityClass
public class TokenUtils {

    public static void insertTokenToMap(Map<String, Token> tokenDtos, Collection<LiquidityPool> liquidityPools,
                                        Asset asset, boolean verifiedOnly) {
        Token token = new Token(asset,
                TokenRegistry.getInstance().getTokensRegistryMap().get(asset.getIdentifier("")),
                TokenRegistry.getInstance().getVerifiedPolicies().contains(asset.getPolicyId()));
        if (verifiedOnly && !token.isVerified()) {
            return;
        }
        token.setCanSwapTo(liquidityPools.stream()
                .filter(liquidityPool2 -> liquidityPool2 != null &&
                        (liquidityPool2.getAssetA().getAssetName().equals(asset.getAssetName()) ||
                                liquidityPool2.getAssetB().getAssetName().equals(asset.getAssetName())))
                .map(liquidityPool3 -> {
                    if (liquidityPool3.getAssetA().getAssetName().equals(asset.getAssetName())) {
                        return liquidityPool3.getAssetB();
                    } else {
                        return liquidityPool3.getAssetA();
                    }
                }).map(asset1 -> asset1.isLovelace() ? "Cardano" :
                        (new Token(asset1,
                                TokenRegistry.getInstance().getTokensRegistryMap().get(asset1.getIdentifier("")),
                                TokenRegistry.getInstance().getVerifiedPolicies().contains(asset1.getPolicyId()))).getTicker())
                .collect(Collectors.toSet()));
        tokenDtos.put(token.getIdentifier(""), token);
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
