package io.adabox.dextreme.model;

import com.bloxbean.cardano.client.util.HexUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;

@Data
@Slf4j
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class Asset {

    private String policyId;
    private String nameHex;
    private int decimals;

    public static Asset fromId(String id, int decimals) {
        id = id.replace(".", "");
        if (id.equals(LOVELACE)) {
            return AssetType.ADA.getAsset();
        } else {
            return new Asset(id.substring(0, 56), id.substring(56), decimals);
        }
    }

    public String getIdentifier(String delimiter) {
        return policyId + delimiter + nameHex;
    }

    public String getNameHex() {
        return isLovelace() ? "" : nameHex;
    }

    @JsonIgnore
    public boolean isLovelace() {
        return nameHex.equalsIgnoreCase(LOVELACE);
    }

    public String getAssetName() {
        return isLovelace() ? "ADA" : new String(HexUtil.decodeHexString(getNameHex()), StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return getAssetName();
    }
}