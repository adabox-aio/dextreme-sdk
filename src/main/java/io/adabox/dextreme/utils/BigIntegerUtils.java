package io.adabox.dextreme.utils;

import lombok.experimental.UtilityClass;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

@UtilityClass
public class BigIntegerUtils {

    public static BigInteger sum(BigInteger... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        return Arrays.stream(values)
                .filter(Objects::nonNull)
                .reduce(BigInteger::add)
                .orElse(null);
    }
}
