package io.adabox.dextreme.dex;

import co.nstant.in.cbor.model.ByteString;
import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.common.CardanoConstants;
import com.bloxbean.cardano.client.plutus.spec.*;
import com.bloxbean.cardano.client.util.HexUtil;
import io.adabox.dextreme.dex.api.MinSwapApi;
import io.adabox.dextreme.dex.base.Dex;
import io.adabox.dextreme.dex.base.DexType;
import io.adabox.dextreme.model.UTxO;
import io.adabox.dextreme.provider.ApiProvider;
import io.adabox.dextreme.provider.base.BaseProvider;
import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.util.List;

public class Minswap extends Dex {

    public static final String FACTORY_TOKEN = "13aa2accf2e1561723aa26871e071fdf32c867cff7e7d50ad470d62f4d494e53574150";
    public static final String LP_TOKEN_POLICY_ID = "e4214b7cce62ac6fbba385d164df48e157eae5863521b4b67ca71d86";
    public static final List<String> POOL_NFT_POLICY_IDs = List.of("0be55d262b29f564998ff81efe21bdc0022621c12f15af08d0f2ddb1");
    public static final String MARKET_ORDER_ADDRESS = "addr1wxn9efv2f6w82hagxqtn62ju4m293tqvw0uhmdl64ch8uwc0h43gt";
    public static final String LIMIT_ORDER_ADDRESS = "addr1zxn9efv2f6w82hagxqtn62ju4m293tqvw0uhmdl64ch8uw6j2c79gy9l76sdg0xwhd7r0c0kna0tycz4y5s6mlenh8pq6s3z70";

    public Minswap() {
        this(new ApiProvider());
    }

    public Minswap(BaseProvider provider) {
        super(DexType.Minswap, provider, new MinSwapApi());
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
        return MARKET_ORDER_ADDRESS;
    }

    @Override
    public String getLimitOrderAddress() {
        return LIMIT_ORDER_ADDRESS;
    }

    @Override
    public double getPoolFeePercent(UTxO uTxO) {
        return 0.3;
    }

    @Override
    public BigInteger getSwapFee() {
        return BigInteger.valueOf(2_000_000);
    }

    @Override
    public BigInteger getLovelaceOutput() {
        return BigInteger.valueOf(2_000_000);
    }

    @Override
    public PlutusData swapDatum(io.adabox.dextreme.model.SwapDatumRequest swapDatumRequest) {
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
                                    .data(ListPlutusData.of(BytesPlutusData.of(paymentKeyHash))).build(), stakeCredConstr))
                    .build();

            return ConstrPlutusData.builder()
                    .alternative(0)
                    .data(ListPlutusData.of(credConstr, credConstr, ConstrPlutusData.builder()
                                    .alternative(1)
                                    .data(ListPlutusData.of())
                                    .build(),
                            ConstrPlutusData.builder()
                                    .alternative(0) // SWAP_EXACT_IN
                                    .data(ListPlutusData.of(ConstrPlutusData.builder()
                                                    .alternative(0)
                                                    .data(ListPlutusData.of(
                                                            StringUtils.isNotBlank(swapDatumRequest.getSellTokenPolicyID()) ?
                                                                    BytesPlutusData.deserialize(new ByteString(HexUtil.decodeHexString(swapDatumRequest.getSellTokenPolicyID()))) :
                                                                    BytesPlutusData.of(""),
                                                            StringUtils.isNotBlank(swapDatumRequest.getSellTokenName()) &&
                                                                    !StringUtils.equalsIgnoreCase(swapDatumRequest.getSellTokenName(), CardanoConstants.LOVELACE) ?
                                                                    BytesPlutusData.of(HexUtil.decodeHexString(swapDatumRequest.getSellTokenName())) :
                                                                    BytesPlutusData.of("")))
                                                    .build(),
                                            BigIntPlutusData.of(swapDatumRequest.getBuyAmount())
                                    ))
                                    .build(),
                            BigIntPlutusData.of(getSwapFee()),
                            BigIntPlutusData.of(getLovelaceOutput())
                    ))
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Datum creation failed for [" + swapDatumRequest + "]", e);
        }
    }
}
