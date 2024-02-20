package io.adabox.dextreme.provider.mapper;

import io.adabox.dextreme.component.TokenRegistry;
import io.adabox.dextreme.model.Asset;
import io.adabox.dextreme.model.AssetType;
import io.adabox.dextreme.model.Balance;
import io.adabox.dextreme.model.UTxO;
import rest.koios.client.backend.api.asset.model.AssetTokenRegistry;
import rest.koios.client.backend.api.transactions.model.TxIO;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class KoiosMapper {

    public static UTxO toUTxO(rest.koios.client.backend.api.base.common.UTxO utxo) {
        String inlineDatum = utxo.getInlineDatum() != null ? utxo.getInlineDatum().getValue().toString() : null;
        String referenceScript = utxo.getReferenceScript() != null ? utxo.getReferenceScript().getValue().toString() : null;
        return new UTxO(utxo.getTxHash(), utxo.getTxIndex(), utxo.getAddress(), toBalance(utxo.getAssetList(), utxo.getValue()),
                utxo.getDatumHash(), inlineDatum, referenceScript);
    }

    private static List<Balance> toBalance(List<rest.koios.client.backend.api.base.common.Asset> assetList, String lovelace) {
        List<Balance> balance = new ArrayList<>();
        balance.add(new Balance(AssetType.ADA.getAsset(), new BigInteger(lovelace)));
        balance.addAll(assetList.stream()
                .map(asset -> {
                    String unit = asset.getPolicyId()+asset.getAssetName();
                    AssetTokenRegistry assetTokenRegistry = TokenRegistry.getInstance().getRegistry(unit);
                    int decimals = assetTokenRegistry == null ? 0 : assetTokenRegistry.getDecimals();
                    return new Balance(Asset.fromId(unit, decimals), new BigInteger(asset.getQuantity()));
                })
                .toList());
        return balance;
    }

    public static List<UTxO> toUtxos(List<TxIO> outputs) {
        List<UTxO> utxos = new ArrayList<>();
        outputs.forEach(txUtxo -> {
            UTxO.UTxOBuilder builder = UTxO.builder()
                    .address(txUtxo.getPaymentAddr().getBech32())
                    .txHash(txUtxo.getTxHash())
                    .balance(toBalance(txUtxo.getAssetList(), txUtxo.getValue()))
                    .datumHash(txUtxo.getDatumHash())
                    .outputIndex(txUtxo.getTxIndex());
            if (txUtxo.getInlineDatum() != null) {
                builder.inlineDatum(txUtxo.getInlineDatum().getValue().toString());
            }
            if (txUtxo.getReferenceScript() != null) {
                builder.referenceScriptHash(txUtxo.getReferenceScript().getValue().toString());
            }
            utxos.add(builder.build());
        });
        return utxos;
    }
}
