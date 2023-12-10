package io.adabox.dextreme.dex;

import co.nstant.in.cbor.model.ByteString;
import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.common.CardanoConstants;
import com.bloxbean.cardano.client.plutus.spec.*;
import com.bloxbean.cardano.client.plutus.spec.serializers.PlutusDataJsonConverter;
import com.bloxbean.cardano.client.util.HexUtil;
import com.fasterxml.jackson.databind.JsonNode;
import io.adabox.dextreme.dex.api.MuesliSwapApi;
import io.adabox.dextreme.dex.base.Dex;
import io.adabox.dextreme.dex.base.DexType;
import io.adabox.dextreme.model.SwapDatumRequest;
import io.adabox.dextreme.model.UTxO;
import io.adabox.dextreme.provider.ApiProvider;
import io.adabox.dextreme.provider.base.BaseProvider;
import io.adabox.dextreme.provider.base.ClientProvider;
import io.adabox.dextreme.utils.BigIntegerUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

/**
 * Muesliswap DEX
 */
@Slf4j
public class Muesliswap extends Dex {

    public static final String FACTORY_TOKEN = "de9b756719341e79785aa13c164e7fe68c189ed04d61c9876b2fe53f4d7565736c69537761705f414d4d";
    public static final String LP_TOKEN_POLICY_ID = "af3d70acf4bd5b3abb319a7d75c89fb3e56eafcdd46b2e9b57a2557f";
    public static final List<String> POOL_NFT_POLICY_IDs = List.of("909133088303c49f3a30f1cc8ed553a73857a29779f6c6561cd8093f", "7a8041a0693e6605d010d5185b034d55c79eaf7ef878aae3bdcdbf67");
    public static final String ORDER_ADDRESS = "addr1zyq0kyrml023kwjk8zr86d5gaxrt5w8lxnah8r6m6s4jp4g3r6dxnzml343sx8jweqn4vn3fz2kj8kgu9czghx0jrsyqqktyhv";
    public static final boolean ALLOW_PARTIAL_FILL = true;

    /**
     * {@link Muesliswap}
     * Default Constructor
     */
    public Muesliswap() {
        this(new ApiProvider());
    }

    /**
     * {@link Muesliswap}
     * @param provider provider
     */
    public Muesliswap(BaseProvider provider) {
        super(DexType.Muesliswap, provider, new MuesliSwapApi());
    }

    @Override
    public String getFactoryToken() {
        return FACTORY_TOKEN;
    }

    @Override
    public List<String> getPoolNFTPolicyIds() {
        return POOL_NFT_POLICY_IDs;
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
            BigInteger lpFeeBigInt = ((BigIntPlutusData) source.getData().getPlutusDataList().get(3)).getValue();
            return new BigDecimal(lpFeeBigInt).divide(new BigDecimal("10000"), 4, RoundingMode.HALF_DOWN).doubleValue();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public BigInteger getSwapFee() {
        return BigInteger.valueOf(950_000);
    }

    @Override
    public BigInteger getLovelaceOutput() {
        return BigInteger.valueOf(1_700_000);
    }

    @Override
    public PlutusData swapDatum(SwapDatumRequest swapDatumRequest) {
        Address address = new Address(swapDatumRequest.getWalletAddr());
        byte[] stakeKeyHash = AddressProvider.getDelegationCredentialHash(address).orElse(null);
        byte[] paymentKeyHash = AddressProvider.getPaymentCredentialHash(address).orElse(null);
        try {
            ConstrPlutusData stakeCredConstr = ConstrPlutusData.builder()
                    .alternative(0)
                    .data(ListPlutusData.of(ConstrPlutusData.builder()
                            .alternative(0)
                            .data(ListPlutusData.of(ConstrPlutusData.builder()
                                    .alternative(0) // key == 0, script == 1
                                    .data(ListPlutusData.of(BytesPlutusData.of(stakeKeyHash)))
                                    .build()))
                            .build()))
                    .build();
            ConstrPlutusData credConstr = ConstrPlutusData.builder()
                    .alternative(0)
                    .data(ListPlutusData.of(
                            ConstrPlutusData.builder()
                                    .alternative(0) // key == 0, script == 1
                                    .data(ListPlutusData.of(BytesPlutusData.of(paymentKeyHash)))
                                    .build(),
                            stakeCredConstr))
                    .build();

            return ConstrPlutusData.builder()
                    .alternative(0)
                    .data(ListPlutusData.of(
                            ConstrPlutusData.builder()
                                    .alternative(0)
                                    .data(ListPlutusData.of(credConstr,
                                            StringUtils.isNotBlank(swapDatumRequest.getSellTokenPolicyID())
                                                    ? BytesPlutusData.deserialize(new ByteString(HexUtil.decodeHexString(swapDatumRequest.getSellTokenPolicyID())))
                                                    : BytesPlutusData.of(""), // SwapOutTokenPolicyId
                                            StringUtils.isNotBlank(swapDatumRequest.getSellTokenName()) && !StringUtils.equalsIgnoreCase(swapDatumRequest.getSellTokenName(), CardanoConstants.LOVELACE) // TODO
                                                    ? BytesPlutusData.of(HexUtil.decodeHexString(swapDatumRequest.getSellTokenName()))
                                                    : BytesPlutusData.of(""), // SwapOutTokenAssetName
                                            StringUtils.isNotBlank(swapDatumRequest.getBuyTokenPolicyID())
                                                    ? BytesPlutusData.deserialize(new ByteString(HexUtil.decodeHexString(swapDatumRequest.getBuyTokenPolicyID())))
                                                    : BytesPlutusData.of(""), // SwapInTokenPolicyId
                                            StringUtils.isNotBlank(swapDatumRequest.getBuyTokenName()) && !StringUtils.equalsIgnoreCase(swapDatumRequest.getBuyTokenName(), CardanoConstants.LOVELACE) // TODO
                                                    ? BytesPlutusData.of(HexUtil.decodeHexString(swapDatumRequest.getBuyTokenName()))
                                                    : BytesPlutusData.of(""), // SwapInTokenAssetName
                                            BigIntPlutusData.of(swapDatumRequest.getBuyAmount()), // MinReceive
                                            ConstrPlutusData.builder()          // AllowPartialFill
                                                    .alternative(ALLOW_PARTIAL_FILL ? 1 : 0)
                                                    .data(ListPlutusData.of())
                                                    .build(),
                                            BigIntPlutusData.of(BigIntegerUtils.sum(getLovelaceOutput(), getSwapFee()))))
                                    .build()))
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Datum creation failed for [" + swapDatumRequest + "]", e);
        }
    }
}
