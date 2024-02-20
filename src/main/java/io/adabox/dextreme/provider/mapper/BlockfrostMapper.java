package io.adabox.dextreme.provider.mapper;

import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.backend.model.TxContentOutputAmount;
import com.bloxbean.cardano.client.backend.model.TxContentUtxoOutputs;
import io.adabox.dextreme.component.TokenRegistry;
import io.adabox.dextreme.model.Asset;
import io.adabox.dextreme.model.Balance;
import io.adabox.dextreme.model.UTxO;
import rest.koios.client.backend.api.asset.model.AssetTokenRegistry;

import java.math.BigInteger;
import java.util.List;

public class BlockfrostMapper {


    public static UTxO toUTxO(Utxo utxo) {
        return new UTxO(utxo.getTxHash(), utxo.getOutputIndex(), utxo.getAddress(), toBalance(utxo.getAmount()),
                utxo.getDataHash(), utxo.getInlineDatum(), utxo.getReferenceScriptHash());
    }

    private static List<Balance> toBalance(List<Amount> amounts) {
        return amounts.stream()
                .map(amount -> {
                    AssetTokenRegistry assetTokenRegistry = TokenRegistry.getInstance().getRegistry(amount.getUnit());
                    int decimals = assetTokenRegistry == null ? 0 : assetTokenRegistry.getDecimals();
                    return new Balance(Asset.fromId(amount.getUnit(), decimals), amount.getQuantity());
                })
                .toList();
    }

    public static UTxO toUTxO(String txHash, TxContentUtxoOutputs utxo) {
        return new UTxO(txHash, utxo.getOutputIndex(), utxo.getAddress(), toBalance2(utxo.getAmount()),
                utxo.getDataHash(), utxo.getInlineDatum(), utxo.getReferenceScriptHash());
    }

    private static List<Balance> toBalance2(List<TxContentOutputAmount> amounts) {
        return amounts.stream()
                .map(amount -> {
                    AssetTokenRegistry assetTokenRegistry = TokenRegistry.getInstance().getRegistry(amount.getUnit());
                    int decimals = assetTokenRegistry == null ? 0 : assetTokenRegistry.getDecimals();
                    return new Balance(Asset.fromId(amount.getUnit(), decimals), new BigInteger(amount.getQuantity()));
                })
                .toList();
    }
}
