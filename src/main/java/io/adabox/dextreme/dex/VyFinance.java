package io.adabox.dextreme.dex;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.plutus.spec.*;
import io.adabox.dextreme.dex.api.VyFinanceApi;
import io.adabox.dextreme.dex.base.Dex;
import io.adabox.dextreme.dex.base.DexType;
import io.adabox.dextreme.model.SwapDatumRequest;
import io.adabox.dextreme.model.UTxO;
import io.adabox.dextreme.provider.ApiProvider;
import io.adabox.dextreme.provider.base.BaseProvider;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.List;

/**
 * VyFinance DEX
 */
@Slf4j
public class VyFinance extends Dex {

    /**
     * {@link VyFinance}
     * Default Constructor
     */
    public VyFinance() {
        this(new ApiProvider());
    }

    /**
     * {@link VyFinance}
     * @param provider provider
     */
    public VyFinance(BaseProvider provider) {
        super(DexType.VyFinance, provider, new VyFinanceApi());
    }

    @Override
    public PlutusData swapDatum(SwapDatumRequest swapDatumRequest) {
        Address address = new Address(swapDatumRequest.getWalletAddr());
        int swapDirection = 0;
        try {
            return ConstrPlutusData.builder()
                    .alternative(0)
                    .data(ListPlutusData.of(BytesPlutusData.of(address.getBytes()), ConstrPlutusData.builder()
                            .alternative(swapDirection)
                            .data(ListPlutusData.of(BigIntPlutusData.of(swapDatumRequest.getBuyAmount())))
                            .build()))
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Datum creation failed for [" + swapDatumRequest + "]", e);
        }
    }

    @Override
    public String getFactoryToken() {
        log.warn("Not implemented as VyFinance pools are not easily identifiable on-chain.");
        return null;
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
        return null;
    }

    @Override
    public String getLimitOrderAddress() {
        return null;
    }

    @Override
    public double getPoolFeePercent(UTxO utxo) {
        return 0;
    }

    @Override
    public BigInteger getSwapFee() { // Process Fee - Fee paid to the off-chain processor fulfilling order.
        return BigInteger.valueOf(1_900_000);
    }

    @Override
    public BigInteger getLovelaceOutput() { // MinADA - MinADA will be held in the UTxO and returned when the order is processed.
        return BigInteger.valueOf(2_000_000);
    }
}
