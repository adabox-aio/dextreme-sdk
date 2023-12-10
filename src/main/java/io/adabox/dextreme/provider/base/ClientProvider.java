package io.adabox.dextreme.provider.base;

import com.fasterxml.jackson.databind.JsonNode;
import io.adabox.dextreme.model.UTxO;

import java.util.List;
import java.util.Optional;

public interface ClientProvider {

    public abstract List<UTxO> utxos(String address, String assetUnit);

    public abstract List<UTxO> transactionUtxos(String txHash);

    public abstract List<String> assetAddresses(String assetUnit);

    public abstract List<UTxO> assetUtxos(String assetUnit);

    public abstract Optional<JsonNode> datum(String datumHash);
}
