package io.adabox.dextreme.dex.base;

import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import io.adabox.dextreme.dex.api.base.Api;
import io.adabox.dextreme.model.*;
import io.adabox.dextreme.provider.base.BaseProvider;
import io.adabox.dextreme.provider.base.ClientProvider;
import io.adabox.dextreme.provider.base.ProviderType;
import io.adabox.dextreme.utils.TokenUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Setter
public abstract class Dex {

    private final DexType dexType;
    private final BaseProvider provider;
    private final Api api;
    private Map<String, LiquidityPool> liquidityPoolMap = new HashMap<>();

    public Dex(DexType dexType, BaseProvider provider, Api api) {
        this.dexType = dexType;
        this.provider = provider;
        this.api = api;
        updateLiquidityPools();
        log.info("{} - Loaded: {} Liquidity Pools.", dexType.name(), getLiquidityPoolMap().size());
    }

    public void updateLiquidityPools() {
        if (getProvider().getProviderType() == ProviderType.API) {
            setLiquidityPoolMap(getApi().liquidityPools().stream()
                    .collect(Collectors.toMap(LiquidityPool::getIdentifier, Function.identity(), ((liquidityPool, liquidityPool2) -> liquidityPool))));
        } else {
            List<UTxO> assetUtxos = ((ClientProvider) getProvider()).assetUtxos(getFactoryToken());
            setLiquidityPoolMap(assetUtxos.stream()
                    .map(this::toLiquidityPool)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(LiquidityPool::getIdentifier, Function.identity(), ((liquidityPool, liquidityPool2) -> liquidityPool))));
        }

    }

    public Map<String, Token> getTokens(boolean verifiedOnly) {
        Map<String, Token> tokenDtos = new HashMap<>();
        getLiquidityPoolMap().values().forEach(liquidityPool -> {
            if (!tokenDtos.containsKey(liquidityPool.getAssetA().getIdentifier(""))) {
                TokenUtils.insertTokenToMap(tokenDtos, getLiquidityPoolMap().values(), liquidityPool.getAssetA(), verifiedOnly);
            }
            if (!tokenDtos.containsKey(liquidityPool.getAssetB().getIdentifier(""))) {
                TokenUtils.insertTokenToMap(tokenDtos, getLiquidityPoolMap().values(), liquidityPool.getAssetB(), verifiedOnly);
            }
        });
        return tokenDtos;
    }

    public List<LiquidityPool> getLiquidityPools(Asset assetA, Asset assetB) {
        if (getProvider().getProviderType() != ProviderType.API || assetA.isLovelace()) {
            return getLiquidityPoolMap().values().stream().filter(liquidityPool -> {
                if (assetA != null && assetB == null) {
                    return liquidityPool.getAssetA().getAssetName().equals(assetA.getAssetName());
                } else if (assetA != null) {
                    return (liquidityPool.getAssetA().getAssetName().equals(assetA.getAssetName()) && liquidityPool.getAssetB().getAssetName().equals(assetB.getAssetName())) ||
                            (liquidityPool.getAssetA().getAssetName().equals(assetB.getAssetName()) && liquidityPool.getAssetB().getAssetName().equals(assetA.getAssetName()));
                } else {
                    return false;
                }
            }).toList();
        }
        return getApi().liquidityPools(assetA, assetB);
    }

    public List<Ohlcv> getPriceChart(Asset assetA, Asset assetB, long timeFrom) {
        return getApi().priceChart(assetA, assetB, timeFrom);
    }

    public LiquidityPool toLiquidityPool(UTxO utxo) {
        if (StringUtils.isBlank(utxo.getDatumHash())) {
            return null;
        }
        if (!utxo.containsAsset(getFactoryToken())) {
            return null;
        }
        if (!utxo.containsAssetPolicyId(getPoolNFTPolicyIds())) {
            return null;
        }
        List<String> policyList = new ArrayList<>(getPoolNFTPolicyIds());
        policyList.add(getLPTokenPolicyId());
        List<Balance> balanceList = utxo.filterByUnitAndPolicies(getFactoryToken(), policyList.toArray(new String[0]));
        if (balanceList.size() < 2 || balanceList.size() > 3) {
            return null;
        }
        int assetAIndex = 0;
        int assetBIndex = 1;

        if (balanceList.size() == 3) {
            assetAIndex = 1;
            assetBIndex = 2;
        }
        LiquidityPool liquidityPool = new LiquidityPool(
                getDexType().name(),
                balanceList.get(assetAIndex).getAsset(),
                balanceList.get(assetBIndex).getAsset(),
                balanceList.get(assetAIndex).getQuantity(),
                balanceList.get(assetBIndex).getQuantity(),
                utxo.getAddress(),
                getMarketOrderAddress(),
                getLimitOrderAddress());

        Asset poolNft = utxo.getAssetWithPolicyIds(getPoolNFTPolicyIds());

        if (poolNft == null) {
            return null;
        }

        liquidityPool.setLpToken(new Asset(getLPTokenPolicyId(), poolNft.getNameHex(), 0));
        liquidityPool.setIdentifier(liquidityPool.getLpToken().getIdentifier(""));
        liquidityPool.setPoolFeePercent(getPoolFeePercent(utxo));
        liquidityPool.setUTxO(utxo);
        return liquidityPool;
    }

    public abstract String getFactoryToken();

    public abstract List<String> getPoolNFTPolicyIds();

    public abstract String getLPTokenPolicyId();

    public abstract String getMarketOrderAddress();

    public abstract String getLimitOrderAddress();

    public abstract double getPoolFeePercent(UTxO utxo);

    public abstract BigInteger getSwapFee(); // the (batcher) fee applicable for this DEX

    public abstract BigInteger getLovelaceOutput(); // the amount of lovelace which will be returned on swaps

    public abstract PlutusData swapDatum(SwapDatumRequest swapDatumRequest);
}
