package io.adabox.dextreme.dex;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.common.CardanoConstants;
import com.bloxbean.cardano.client.plutus.spec.*;
import com.bloxbean.cardano.client.plutus.spec.serializers.PlutusDataJsonConverter;
import com.bloxbean.cardano.client.util.HexUtil;
import com.fasterxml.jackson.databind.JsonNode;
import io.adabox.dextreme.dex.api.SundaeswapApi;
import io.adabox.dextreme.dex.base.Dex;
import io.adabox.dextreme.dex.base.DexType;
import io.adabox.dextreme.model.*;
import io.adabox.dextreme.provider.ApiProvider;
import io.adabox.dextreme.provider.base.BaseProvider;
import io.adabox.dextreme.provider.base.ClientProvider;
import io.adabox.dextreme.provider.base.ProviderType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class Sundaeswap extends Dex {

    public static final String LP_TOKEN_POLICY_ID = "0029cb7c88c7567b63d1a512c0ed626aa169688ec980730c0473b913";
    public static final String POOL_ADDRESS = "addr1w9qzpelu9hn45pefc0xr4ac4kdxeswq7pndul2vuj59u8tqaxdznu";
    public static final String ORDER_ADDRESS = "addr1wxaptpmxcxawvr3pzlhgnpmzz3ql43n2tc8mn3av5kx0yzs09tqh8";

    /**
     * {@link Sundaeswap}
     * Default Constructor
     */
    public Sundaeswap() {
        this(new ApiProvider());
    }

    /**
     * {@link Sundaeswap}
     * @param provider provider
     */
    public Sundaeswap(BaseProvider provider) {
        super(DexType.Sundaeswap, provider, new SundaeswapApi());
    }

    @Override
    public void updateLiquidityPools() {
        if (getProvider().getProviderType() == ProviderType.API) {
            setLiquidityPoolMap(getApi().liquidityPools().stream()
                    .collect(Collectors.toMap(LiquidityPool::getIdentifier, Function.identity(), (liquidityPool, liquidityPool2) -> {
                        log.error(liquidityPool+" "+liquidityPool2);
                        return liquidityPool;
                    })));
        } else {
            setLiquidityPoolMap(((ClientProvider) getProvider()).utxos(POOL_ADDRESS, null).stream()
                    .map(this::toLiquidityPool)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(LiquidityPool::getIdentifier, Function.identity()
//                            , (liquidityPool, liquidityPool2) -> {
//                                return liquidityPool.getUTxO(). // TODO FIX
//                            }
                    )));
        }
    }

    @Override
    public LiquidityPool toLiquidityPool(UTxO utxo) {
        if (StringUtils.isBlank(utxo.getDatumHash())) {
            return null;
        }
        if (!utxo.containsAssetPolicyId(List.of(getLPTokenPolicyId()))) {
            return null;
        }
        List<Balance> balanceList = utxo.getBalance().stream()
                .filter(balance -> !balance.getAsset().getPolicyId().equals(getLPTokenPolicyId()))
                .toList();

        if (!List.of(2, 3).contains(balanceList.size())) {
            return null;
        }

        int assetAIndex = 0;
        int assetBIndex = 1;

        if (balanceList.size() == 3) {
            assetAIndex = 1;
            assetBIndex = 2;
        }
        LiquidityPool liquidityPool = new LiquidityPool(
                DexType.Sundaeswap.name(),
                balanceList.get(assetAIndex).getAsset(),
                balanceList.get(assetBIndex).getAsset(),
                balanceList.get(assetAIndex).getQuantity(),
                balanceList.get(assetBIndex).getQuantity(),
                utxo.getAddress(),
                getMarketOrderAddress(),
                getLimitOrderAddress());

        Asset lpToken = utxo.getBalance().stream()
                .filter(balance -> !balance.getAsset().isLovelace() && balance.getAsset().getPolicyId().equals(LP_TOKEN_POLICY_ID))
                .findFirst()
                .map(Balance::getAsset)
                .orElse(null);

        if (lpToken != null) {
            lpToken.setNameHex("6c" + lpToken.getNameHex());
            liquidityPool.setLpToken(lpToken);
            liquidityPool.setIdentifier(lpToken.getIdentifier(""));
        }
        liquidityPool.setPoolFeePercent(getPoolFeePercent(utxo));
        return liquidityPool;
    }

    @Override
    public String getFactoryToken() {
        return null;
    }

    @Override
    public List<String> getPoolNFTPolicyIds() {
        return null;
    }

    @Override
    public String getLPTokenPolicyId() {
        return LP_TOKEN_POLICY_ID;
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
        try {
            JsonNode json = ((ClientProvider) getProvider()).datum(utxo.getDatumHash()).orElse(null);
            PlutusData plutusData = PlutusDataJsonConverter.toPlutusData(json);
            ConstrPlutusData source = (ConstrPlutusData) plutusData;
//            List<String> assets = assetsFromPlutusData((ConstrPlutusData)source.getData().getPlutusDataList().get(0));
            return lpFeeFromPlutusData((ConstrPlutusData) source.getData().getPlutusDataList().get(3));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public BigInteger getSwapFee() { // Scooper Processing Fee - An ADA fee paid to the Sundae Scooper Network for processing your order.
        return BigInteger.valueOf(2_500_000);
    }

    @Override
    public BigInteger getLovelaceOutput() { // Deposit - A small ADA deposit that you will get back when your order is processed or cancelled.
        return BigInteger.valueOf(2_000_000);
    }

    @Override
    public PlutusData swapDatum(SwapDatumRequest swapDatumRequest) {
        Address address = new Address(swapDatumRequest.getWalletAddr());
        byte[] stakeKeyHash = AddressProvider.getDelegationCredentialHash(address).orElse(null);
        byte[] paymentKeyHash = AddressProvider.getPaymentCredentialHash(address).orElse(null);
        int swapDirection = 0;
        try {
            return ConstrPlutusData.builder()
                    .alternative(0)
                    .data(ListPlutusData.of(BytesPlutusData.of(swapDatumRequest.getPoolId()),
                            ConstrPlutusData.builder()
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
                                                                                                    .data(ListPlutusData.of(ListPlutusData.of(BytesPlutusData.of(stakeKeyHash))))
                                                                                                    .build()))
                                                                                            .build()
                                                                                    ))
                                                                                    .build()))
                                                                    .build(),
                                                            ConstrPlutusData.builder().alternative(1)
                                                                    .data(ListPlutusData.of())
                                                                    .build()
                                                    ))
                                                    .build(),
                                            ConstrPlutusData.builder().alternative(1)
                                                    .data(ListPlutusData.of())
                                                    .build()
                                    ))
                                    .build(),
                            BigIntPlutusData.of(getSwapFee()),
                            ConstrPlutusData.builder()
                                    .alternative(0)
                                    .data(ListPlutusData.of(ConstrPlutusData.builder()
                                                    .alternative(swapDirection)
                                                    .data(ListPlutusData.of())
                                                    .build(),
                                            BigIntPlutusData.of(swapDatumRequest.getBuyAmount()),
                                            ConstrPlutusData.builder()
                                                    .alternative(0)
                                                    .data(ListPlutusData.of(BigIntPlutusData.of(swapDatumRequest.getBuyAmount())))
                                                    .build()))
                                    .build()))
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Datum creation failed for [" + swapDatumRequest + "]", e);
        }
    }

    private double lpFeeFromPlutusData(ConstrPlutusData constr) {
        BigInteger lpFeeNumerator = ((BigIntPlutusData) constr.getData().getPlutusDataList().get(0)).getValue();
        BigInteger lpFeeDenominator = ((BigIntPlutusData) constr.getData().getPlutusDataList().get(1)).getValue();
        return lpFeeNumerator != null && lpFeeDenominator != null ? lpFeeNumerator.doubleValue() / lpFeeDenominator.doubleValue() * 100 : 0;
    }

    private static List<String> assetsFromPlutusData(ConstrPlutusData constr) {
        return List.of(assetFromPlutusData((ConstrPlutusData) constr.getData().getPlutusDataList().get(0)),
                assetFromPlutusData((ConstrPlutusData) constr.getData().getPlutusDataList().get(1)));
    }

    private static String assetFromPlutusData(ConstrPlutusData constr) {
        var policyIdByte = ((BytesPlutusData) constr.getData().getPlutusDataList().get(0)).getValue();
        var policyIdExtracted = HexUtil.encodeHexString(policyIdByte);
        var tokenNameByte = ((BytesPlutusData) constr.getData().getPlutusDataList().get(1)).getValue();
        var tokenNameExtracted = HexUtil.encodeHexString(tokenNameByte);
        return StringUtils.isNotBlank(policyIdExtracted) || StringUtils.isNotBlank(tokenNameExtracted) ? policyIdExtracted + tokenNameExtracted : CardanoConstants.LOVELACE;
    }
}
