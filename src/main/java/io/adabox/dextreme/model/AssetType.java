package io.adabox.dextreme.model;

import com.bloxbean.cardano.client.api.util.AssetUtil;
import com.bloxbean.cardano.client.util.Tuple;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;

@Getter
public enum AssetType {

    ADA("", LOVELACE, 6),
    iBTC("f66d78b4a3cb3d37afa0ec36461e51ecbde00f26c8f0a68f94b69880", "69425443", 6),
    iUSD("f66d78b4a3cb3d37afa0ec36461e51ecbde00f26c8f0a68f94b69880", "69555344", 6),
    GERO("10a49b996e2402269af553a8a96fb8eb90d79e9eca79e2b4223057b6", "4745524f", 6),
    SUNDAE("9a9693a9a37912a5097918f97918d15240c92ab729a0b7c4aa144d77", "53554e444145", 6);

    private final Asset asset;

    AssetType(String policyId, String nameHex, int decimals) {
        this.asset = new Asset(policyId, nameHex, decimals);
    }

    public static Asset resolve(String assetId) {
        try {
            if (StringUtils.isNotBlank(assetId)) {
                return valueOf(assetId).getAsset();
            } else {
                return null;
            }
        } catch (IllegalArgumentException e) {
            Tuple<String, String> assetTuple = AssetUtil.getPolicyIdAndAssetName(assetId);
            AssetType assetTypeRes = Arrays.stream(values()).filter(assetType -> assetType.getAsset().getPolicyId().equalsIgnoreCase(assetTuple._1) &&
                    assetType.getAsset().getNameHex().equalsIgnoreCase(assetTuple._2.replace("0x", ""))).findFirst().orElse(null);
            if (assetTypeRes == null) {
                return new Asset(assetTuple._1.toLowerCase(), assetTuple._2.replace("0x","").toLowerCase(), 0);
            }
            return assetTypeRes.getAsset();
        }
    }
}
