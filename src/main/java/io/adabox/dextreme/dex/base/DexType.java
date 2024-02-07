package io.adabox.dextreme.dex.base;

import java.util.Arrays;

public enum DexType {
    Minswap,
    Muesliswap,
    Sundaeswap,
    VyFinance,
    Spectrum,
    WingRiders;

    public static DexType resolveDexType(String dexTypeString) {
        return Arrays.stream(values()).filter(dexType -> dexType.name().equalsIgnoreCase(dexTypeString)).findFirst().orElse(null);
    }
}
