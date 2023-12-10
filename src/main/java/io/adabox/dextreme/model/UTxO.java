package io.adabox.dextreme.model;

import lombok.*;

import java.util.List;
import java.util.stream.Stream;

@Setter
@Getter
@Builder
@ToString
@AllArgsConstructor
public class UTxO {

    private String txHash;
    private int outputIndex;
    private String address;
    private List<Balance> balance;
    private String datumHash;
    private String inlineDatum;
    private String referenceScriptHash;

    public Asset getAssetWithPolicyIds(List<String> policyIds) {
        return getBalance().stream()
                .filter(balance -> policyIds.contains(balance.getAsset().getPolicyId()))
                .findFirst()
                .map(Balance::getAsset)
                .orElse(null);
    }

    public boolean containsAsset(String unit) {
        return getBalance().stream().anyMatch(balance -> balance.getAsset().getIdentifier("").equals(unit));
    }

    public boolean containsAssetPolicyId(List<String> policyIds) {
        return getBalance().stream().anyMatch(balance -> policyIds.contains(balance.getAsset().getPolicyId()));
    }

    public List<Balance> filterByUnitAndPolicies(String unit, String... policyIds) {
        return getBalance().stream()
                .filter(balance -> !balance.getAsset().getIdentifier("").equals(unit) &&
                        Stream.of(policyIds).noneMatch(s -> s.equals(balance.getAsset().getPolicyId())))
                .toList();
    }
}
