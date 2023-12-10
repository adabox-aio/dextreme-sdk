package io.adabox.dextreme.model;

import lombok.*;
import rest.koios.client.backend.api.asset.model.AssetTokenRegistry;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Token extends Asset {

    private String name;
    private String ticker;
    private String image;
    private boolean verified;
    private String description;
    private Set<String> canSwapTo = new HashSet<>();

    public Token(String policyId, String nameHex, String name, String ticker, String image, boolean verified, int decimals, String description) {
        super(policyId, nameHex, decimals);
        this.name = name;
        this.ticker = ticker;
        this.image = image;
        this.verified = verified;
        this.description = description;
    }

    public Token(Asset asset, AssetTokenRegistry assetTokenRegistry, boolean isVerified) {
        if (asset.isLovelace()) {
            setNameHex(LOVELACE);
            setPolicyId(asset.getPolicyId());
            setName("Cardano");
            setTicker("ADA");
            setDecimals(6);
            setVerified(true);
        } else {
            setNameHex(asset.getNameHex());
            setPolicyId(asset.getPolicyId());
            if (assetTokenRegistry != null) {
                setName(assetTokenRegistry.getAssetNameAscii());
                setTicker(assetTokenRegistry.getTicker());
                setImage(assetTokenRegistry.getLogo());
                setDecimals(assetTokenRegistry.getDecimals());
                setDescription(assetTokenRegistry.getDescription());
            } else {
                setName(asset.getAssetName());
                setTicker(asset.getAssetName());
            }
            setVerified(isVerified);
        }
    }

    public String getId() {
        return getPolicyId() + getNameHex();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Token token = (Token) o;
        return Objects.equals(getNameHex(), token.getNameHex()) && Objects.equals(getPolicyId(), token.getPolicyId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNameHex(), getPolicyId());
    }
}
