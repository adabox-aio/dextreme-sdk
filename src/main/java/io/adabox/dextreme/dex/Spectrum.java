package io.adabox.dextreme.dex;

import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import io.adabox.dextreme.dex.api.SpectrumApi;
import io.adabox.dextreme.dex.base.Dex;
import io.adabox.dextreme.dex.base.DexType;
import io.adabox.dextreme.model.SwapDatumRequest;
import io.adabox.dextreme.model.UTxO;
import io.adabox.dextreme.provider.ApiProvider;
import io.adabox.dextreme.provider.base.BaseProvider;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.List;

@Slf4j
public class Spectrum extends Dex {

    public Spectrum() {
        this(new ApiProvider());
    }

    public Spectrum(BaseProvider provider) {
        super(DexType.Spectrum, provider, new SpectrumApi());
    }

    @Override
    public PlutusData swapDatum(SwapDatumRequest swapDatumRequest) {
        return null;
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
    public BigInteger getSwapFee() {
        return null;
    }

    @Override
    public BigInteger getLovelaceOutput() {
        return null;
    }
}