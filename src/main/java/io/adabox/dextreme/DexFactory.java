package io.adabox.dextreme;

import io.adabox.dextreme.component.TokenRegistry;
import io.adabox.dextreme.dex.*;
import io.adabox.dextreme.dex.base.Dex;
import io.adabox.dextreme.dex.base.DexType;
import io.adabox.dextreme.provider.ApiProvider;
import io.adabox.dextreme.provider.base.BaseProvider;

/**
 * Dex Factory
 */
public class DexFactory {

    private DexFactory() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Get Dex Object
     *
     * @return {@link Dex}
     */
    public static Dex getDex(DexType dexType, BaseProvider baseProvider) {
        TokenRegistry.getInstance().updateTokens();
        switch (dexType) {
            case Minswap -> {
                return new Minswap(baseProvider);
            }
            case Muesliswap -> {
                return new Muesliswap(baseProvider);
            }
            case Sundaeswap -> {
                return new Sundaeswap(baseProvider);
            }
            case WingRiders -> {
                return new WingRiders(baseProvider);
            }
            case VyFinance -> {
                return new VyFinance(new ApiProvider());
            }
//            case Spectrum -> {
//                return new Spectrum(baseProvider);
//            }
            case null, default ->
                    throw new IllegalArgumentException(dexType + " is not yet Supported!");
        }
    }
}
