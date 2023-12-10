package io.adabox.dextreme.provider;

import com.bloxbean.cardano.client.api.common.OrderEnum;
import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.backend.api.*;
import com.bloxbean.cardano.client.backend.blockfrost.common.Constants;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.client.backend.model.AssetAddress;
import com.bloxbean.cardano.client.backend.model.TxContentUtxo;
import com.bloxbean.cardano.client.common.CardanoConstants;
import com.fasterxml.jackson.databind.JsonNode;
import io.adabox.dextreme.model.UTxO;
import io.adabox.dextreme.provider.base.BaseProvider;
import io.adabox.dextreme.provider.base.ClientProvider;
import io.adabox.dextreme.provider.base.ProviderType;
import io.adabox.dextreme.provider.mapper.BlockfrostMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Getter
public class BlockfrostProvider extends BaseProvider implements ClientProvider {

    private final UtxoService utxoService;
    private final TransactionService transactionService;
    private final AssetService assetService;
    private final ScriptService scriptService;

    public BlockfrostProvider(String bfProjectId) {
        BackendService backendService = new BFBackendService(Constants.BLOCKFROST_MAINNET_URL, bfProjectId);
        utxoService = backendService.getUtxoService();
        transactionService = backendService.getTransactionService();
        assetService = backendService.getAssetService();
        scriptService = backendService.getScriptService();
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.BLOCKFROST;
    }

    @Override
    public List<UTxO> utxos(String address, String unit) {
        try {
            List<Utxo> result = new ArrayList<>();
            int page = 1;
            while (true) {
                var pageUtxos = StringUtils.equals(CardanoConstants.LOVELACE, unit)
                        ? getUtxoService().getUtxos(address, 1000, page, OrderEnum.desc)
                        : getUtxoService().getUtxos(address, unit, 1000, page, OrderEnum.desc);
                if (pageUtxos == null || pageUtxos.getValue() == null || pageUtxos.getValue().isEmpty()) {
                    return result.stream().map(BlockfrostMapper::toUTxO).toList();
                }
                result.addAll(pageUtxos.getValue());
                page += 1;
            }
        } catch (ApiException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<UTxO> transactionUtxos(String txHash) {
        try {
            Result<TxContentUtxo> transactionContentResult = getTransactionService().getTransactionUtxos(txHash);
            return transactionContentResult.getValue().getOutputs().stream()
                    .map(txContentUtxoOutputs -> BlockfrostMapper.toUTxO(txHash, txContentUtxoOutputs))
                    .toList();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> assetAddresses(String assetUnit) {
        try {
            return assetService.getAllAssetAddresses(assetUnit).getValue().stream().map(AssetAddress::getAddress).toList();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<UTxO> assetUtxos(String assetUnit) {
        List<UTxO> utxos = new ArrayList<>();
        try {
            List<String> addresses = assetAddresses(assetUnit);
            addresses.forEach(address -> utxos.addAll(utxos(address, null)));
            return utxos;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<JsonNode> datum(String datumHash) {
        try {
            return Optional.of(scriptService.getScriptDatum(datumHash).getValue().getJsonValue());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
