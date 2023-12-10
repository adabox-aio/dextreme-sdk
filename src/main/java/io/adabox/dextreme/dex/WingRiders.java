package io.adabox.dextreme.dex;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.plutus.spec.*;
import io.adabox.dextreme.dex.api.WingRidersApi;
import io.adabox.dextreme.dex.base.Dex;
import io.adabox.dextreme.dex.base.DexType;
import io.adabox.dextreme.model.*;
import io.adabox.dextreme.provider.ApiProvider;
import io.adabox.dextreme.provider.base.BaseProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.util.List;

/**
 * WingRiders DEX
 */
@Slf4j
public class WingRiders extends Dex {

    public static final String FACTORY_TOKEN = "026a18d04a0c642759bb3d83b12e3344894e5c1c7b2aeb1a2113a5704c";
    public static final String ORDER_ADDRESS = "addr1wxr2a8htmzuhj39y2gq7ftkpxv98y2g67tg8zezthgq4jkg0a4ul4";
    public static final BigInteger MIN_POOL_ADA = BigInteger.valueOf(3000000L);

    /**
     * {@link WingRiders}
     * Default Constructor
     */
    public WingRiders() {
        this(new ApiProvider());
    }

    /**
     * {@link WingRiders}
     * @param provider provider
     */
    public WingRiders(BaseProvider provider) {
        super(DexType.WingRiders, provider, new WingRidersApi());
    }

    @Override
    public LiquidityPool toLiquidityPool(UTxO utxo) {
        if (StringUtils.isBlank(utxo.getDatumHash())) {
            return null;
        }
        Asset validityToken = Asset.fromId(getFactoryToken(), 0);

        List<Balance> balanceList = utxo.getBalance().stream()
                .filter(balance -> !balance.getAsset().getPolicyId().equals(validityToken.getPolicyId()))
                .toList();

        if (balanceList.size() < 2) {
            return null;
        }

        int assetAIndex = 0;
        int assetBIndex = 1;

        if (balanceList.size() == 3) {
            assetAIndex = 1;
            assetBIndex = 2;
        }

        BigInteger assetAQuantity = balanceList.get(assetAIndex).getQuantity();
        BigInteger assetBQuantity = balanceList.get(assetBIndex).getQuantity();

        LiquidityPool liquidityPool = new LiquidityPool(
                DexType.WingRiders.name(),
                balanceList.get(assetAIndex).getAsset(),
                balanceList.get(assetBIndex).getAsset(),
                balanceList.get(assetAIndex).getAsset().isLovelace() ? ((assetAQuantity.longValue() - MIN_POOL_ADA.longValue() < 1000000L) ? assetAQuantity.subtract(MIN_POOL_ADA) : assetAQuantity) : assetAQuantity,
                balanceList.get(assetBIndex).getAsset().isLovelace() ? ((assetBQuantity.longValue() - MIN_POOL_ADA.longValue() < 1000000L) ? assetBQuantity.subtract(MIN_POOL_ADA) : assetBQuantity) : assetBQuantity,
                utxo.getAddress(),
                getMarketOrderAddress(),
                getLimitOrderAddress());

        Asset lpToken = utxo.getBalance().stream()
                .map(Balance::getAsset)
                .filter(asset -> !asset.isLovelace() &&
                        asset.getPolicyId().equals(validityToken.getPolicyId()) &&
                        !asset.getNameHex().equals(validityToken.getNameHex())).findFirst()
                .orElse(null);

        if (lpToken != null) {
            liquidityPool.setLpToken(lpToken);
            liquidityPool.setIdentifier(lpToken.getIdentifier(""));
        }
        liquidityPool.setPoolFeePercent(getPoolFeePercent(utxo));
        return liquidityPool;
    }

    @Override
    public PlutusData swapDatum(SwapDatumRequest swapDatumRequest) {
        Address address = new Address(swapDatumRequest.getWalletAddr());
        byte[] stakeKeyHash = AddressProvider.getDelegationCredentialHash(address).orElse(null);
        byte[] paymentKeyHash = AddressProvider.getPaymentCredentialHash(address).orElse(null);
        int swapDirection = 0;
        long expiration = System.currentTimeMillis() + (60 * 60 * 6 * 1000);
        try {
            return ConstrPlutusData.builder()
                    .alternative(0)
                    .data(ListPlutusData.of(ConstrPlutusData.builder()
                                    .alternative(0)
                                    .data(ListPlutusData.of(ConstrPlutusData.builder()
                                                    .alternative(0)
                                                    .data(ListPlutusData.of(ConstrPlutusData.builder()
                                                                    .alternative(0)
                                                                    .data(ListPlutusData.of(BytesPlutusData.of(paymentKeyHash)))
                                                                    .build(),
                                                            ConstrPlutusData.builder()
                                                                    .alternative(0)
                                                                    .data(ListPlutusData.of(ConstrPlutusData.builder()
                                                                            .alternative(0)
                                                                            .data(ListPlutusData.of(ConstrPlutusData.builder()
                                                                                    .alternative(0)
                                                                                    .data(ListPlutusData.of(BytesPlutusData.of(stakeKeyHash)))
                                                                                    .build()))
                                                                            .build()))
                                                                    .build()))
                                                    .build(),
                                            BytesPlutusData.of(paymentKeyHash),
                                            BigIntPlutusData.of(expiration),
                                            ConstrPlutusData.builder()
                                                    .alternative(0)
                                                    .data(ListPlutusData.of(ConstrPlutusData.builder()
                                                                    .alternative(0)
                                                                    .data(ListPlutusData.of(BytesPlutusData.of(swapDatumRequest.getBuyTokenPolicyID()),
                                                                            BytesPlutusData.of(swapDatumRequest.getBuyTokenName())))
                                                                    .build(),
                                                            ConstrPlutusData.builder()
                                                                    .alternative(0)
                                                                    .data(ListPlutusData.of(BytesPlutusData.of(swapDatumRequest.getSellTokenPolicyID()),
                                                                            BytesPlutusData.of(swapDatumRequest.getSellTokenName())))
                                                                    .build()))
                                                    .build()))
                                    .build(),
                            ConstrPlutusData.builder()
                                    .alternative(0)
                                    .data(ListPlutusData.of(ConstrPlutusData.builder()
                                                    .alternative(swapDirection)
                                                    .data(ListPlutusData.of())
                                                    .build(),
                                            BigIntPlutusData.of(swapDatumRequest.getBuyAmount())))
                                    .build()))
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Datum creation failed for [" + swapDatumRequest + "]", e);
        }
    }

    @Override
    public String getFactoryToken() {
        return FACTORY_TOKEN;
    }

    @Override
    public List<String> getPoolNFTPolicyIds() {
        return null;
    }

    @Override
    public String getLPTokenPolicyId() {
        return null;
    }

    @Override
    public String getMarketOrderAddress() {
        return ORDER_ADDRESS;
    }

    @Override
    public String getLimitOrderAddress() {
        return ORDER_ADDRESS;
    }

    @Override
    public double getPoolFeePercent(UTxO utxo) {
        return 0.35;
    }

    @Override
    public BigInteger getSwapFee() { // Agent Fee - WingRiders DEX employs decentralized Agents to ensure equal access, strict fulfillment ordering and protection to every party involved in exchange for a small fee.
        return BigInteger.valueOf(2_000_000);
    }

    @Override
    public BigInteger getLovelaceOutput() { // Oil - A small amount of ADA has to be bundled with all token transfers on the Cardano Blockchain. We call this "Oil ADA" and it is always returned to the owner when the request gets fulfilled. If the request expires and the funds are reclaimed, the Oil ADA is returned as well.
        return BigInteger.valueOf(2_000_000);
    }
}