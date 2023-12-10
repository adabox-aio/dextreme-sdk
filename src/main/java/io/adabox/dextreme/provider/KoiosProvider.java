package io.adabox.dextreme.provider;

import com.bloxbean.cardano.client.api.util.AssetUtil;
import com.fasterxml.jackson.databind.JsonNode;
import io.adabox.dextreme.model.UTxO;
import io.adabox.dextreme.provider.base.BaseProvider;
import io.adabox.dextreme.provider.base.ClientProvider;
import io.adabox.dextreme.provider.base.ProviderType;
import io.adabox.dextreme.provider.mapper.KoiosMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import rest.koios.client.backend.api.address.AddressService;
import rest.koios.client.backend.api.asset.AssetService;
import rest.koios.client.backend.api.asset.model.AssetAddress;
import rest.koios.client.backend.api.asset.model.PaymentAddress;
import rest.koios.client.backend.api.base.Result;
import rest.koios.client.backend.api.base.exception.ApiException;
import rest.koios.client.backend.api.script.ScriptService;
import rest.koios.client.backend.api.script.model.DatumInfo;
import rest.koios.client.backend.api.transactions.TransactionsService;
import rest.koios.client.backend.api.transactions.model.TxInfo;
import rest.koios.client.backend.factory.BackendFactory;
import rest.koios.client.backend.factory.BackendService;
import rest.koios.client.backend.factory.options.Offset;
import rest.koios.client.backend.factory.options.Options;
import rest.koios.client.utils.Tuple;

import java.util.*;

@Slf4j
public class KoiosProvider extends BaseProvider implements ClientProvider {

    private final AddressService addressService;
    private final AssetService assetService;
    private final TransactionsService transactionsService;
    private final ScriptService scriptService;

    public KoiosProvider() {
        this(null);
    }

    public KoiosProvider(String apiToken) {
        BackendService backendService;
        if (StringUtils.isBlank(apiToken)) {
            backendService = BackendFactory.getKoiosMainnetService();
        } else {
            backendService = BackendFactory.getKoiosMainnetService(apiToken);
        }
        addressService = backendService.getAddressService();
        assetService = backendService.getAssetService();
        transactionsService = backendService.getTransactionsService();
        scriptService = backendService.getScriptService();
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.KOIOS;
    }

    @Override
    public List<UTxO> utxos(String address, String assetUnit) {
        List<UTxO> utxos = new ArrayList<>();
        int page = 0;
        int count = 1000;
        List<rest.koios.client.backend.api.base.common.UTxO> addressUTxOs = null;
        do {
            try {
                Options options = Options.builder().option(Offset.of((long) count * page++)).build();
                Result<List<rest.koios.client.backend.api.base.common.UTxO>> utxosResult = addressService.getAddressUTxOs(List.of(address), true, options);
                if (utxosResult.isSuccessful()) {
                    addressUTxOs = utxosResult.getValue();
                    if (StringUtils.isNotBlank(assetUnit)) {
                        utxos.addAll(addressUTxOs
                                .stream()
                                .filter(uTxO -> null == uTxO.getAssetList()
                                        .stream()
                                        .filter(asset -> AssetUtil.getUnit(asset.getPolicyId(), asset.getAssetName()).equals(assetUnit))
                                        .findFirst()
                                        .orElse(null))
                                .map(KoiosMapper::toUTxO)
                                .toList());
                    } else {
                        utxos.addAll(addressUTxOs.stream().map(KoiosMapper::toUTxO).toList());
                    }
                }
            } catch (ApiException e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        } while (!CollectionUtils.isEmpty(addressUTxOs) && addressUTxOs.size() == count);
        return utxos;
    }

    @Override
    public List<UTxO> transactionUtxos(String txHash) {
        List<UTxO> utxos = new ArrayList<>();
        try {
            Result<TxInfo> txInfoResult = transactionsService.getTransactionInformation(txHash);
            if (txInfoResult.isSuccessful() && txInfoResult.getValue() != null) {
                utxos.addAll(KoiosMapper.toUtxos(txInfoResult.getValue().getOutputs()));
            }
        } catch (ApiException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return utxos;
    }

    @Override
    public List<String> assetAddresses(String assetUnit) {
        List<String> addresses = new ArrayList<>();
        com.bloxbean.cardano.client.util.Tuple<String, String> tuple = AssetUtil.getPolicyIdAndAssetName(assetUnit);
        int page = 0;
        int count = 1000;
        List<AssetAddress> assetsAddresses = null;
        do {
            try {
                Options options = Options.builder().option(Offset.of((long) count * page++)).build();
                Result<List<AssetAddress>> assetsAddressesResult = assetService.getAssetsAddresses(tuple._1, tuple._2.replace("0x",""), options);
                if (assetsAddressesResult.isSuccessful()) {
                    assetsAddresses = assetsAddressesResult.getValue();
                    addresses.addAll(assetsAddresses.stream().map(PaymentAddress::getPaymentAddress).toList());
                }
            } catch (ApiException e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        } while (!CollectionUtils.isEmpty(assetsAddresses) && assetsAddresses.size() == count);
        return addresses;
    }

    @Override
    public List<UTxO> assetUtxos(String assetUnit) {
        List<UTxO> utxos = new ArrayList<>();
        com.bloxbean.cardano.client.util.Tuple<String, String> tuple = AssetUtil.getPolicyIdAndAssetName(assetUnit);
        List<Tuple<String, String>> tupleList = List.of(new Tuple<>(tuple._1, tuple._2.replace("0x","")));
        int page = 0;
        int count = 1000;
        List<rest.koios.client.backend.api.base.common.UTxO> assetUTxOs = null;
        do {
            try {
                Options options = Options.builder().option(Offset.of((long) count * page++)).build();
                Result<List<rest.koios.client.backend.api.base.common.UTxO>> utxosResult = assetService.getAssetUTxOs(tupleList, true, options);
                if (utxosResult.isSuccessful()) {
                    assetUTxOs = utxosResult.getValue();
                    utxos.addAll(assetUTxOs.stream().map(KoiosMapper::toUTxO).toList());
                }
            } catch (ApiException e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        } while (!CollectionUtils.isEmpty(assetUTxOs) && assetUTxOs.size() == count);
        return utxos;
    }

    @Override
    public Optional<JsonNode> datum(String datumHash) {
        Optional<JsonNode> result = Optional.empty();
        try {
            Result<List<DatumInfo>> response = scriptService.getDatumInformation(List.of(datumHash), Options.EMPTY);
            if (response.isSuccessful() && !CollectionUtils.isEmpty(response.getValue())) {
                DatumInfo datumInfo = response.getValue().get(0);
                result = Optional.of(datumInfo.getValue());
            }
        } catch (ApiException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return result;
    }
}
