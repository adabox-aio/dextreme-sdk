package io.adabox.dextreme.provider;

import io.adabox.dextreme.provider.base.BaseProvider;
import io.adabox.dextreme.provider.base.ProviderType;

public class ApiProvider extends BaseProvider {

    @Override
    public ProviderType getProviderType() {
        return ProviderType.API;
    }
}
