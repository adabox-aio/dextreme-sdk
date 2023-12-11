# Dextreme SDK

## Supported DEXs
- [Minswap](https://app.minswap.org/)
- [Muesliswap](https://muesliswap.com/)
- [Sundaeswap](https://sundae.fi/)
- [Wingriders](https://app.wingriders.com/)
- [VyFinance](https://vyfi.io/)

## Features
- Pull Liquidity Pools from DEX APIs or On-chain using [Blockfrost](https://blockfrost.io/) / [Koios](https://www.koios.rest/)
- Explore the Best Prices for Swaps by comparing between all supported DEXs 
- Build Swap Datums across multiple DEXs

## Use as a library in a Java Project

### Add dependency

- For Maven, add the following dependency to project's pom.xml
```xml
<dependency>
    <groupId>io.adabox</groupId>
    <artifactId>dextreme-sdk</artifactId>
    <version>1.0.1</version>
</dependency>
```

- For Gradle, add the following dependency to build.gradle
```
compile group: 'io.adabox', name: 'dextreme-sdk', version: '1.0.1'
```

### Choose Between Different Providers

- API

```java
import io.adabox.dextreme.provider.ApiProvider;

ApiProvider apiProvider = new ApiProvider(); 
```
<hr>

- Koios
```java
import io.adabox.dextreme.provider.KoiosProvider;

KoiosProvider koiosProvider = new KoiosProvider("<API_TOKEN>"); 
```
<hr>

- Blockfrost

```java
import io.adabox.dextreme.provider.BlockfrostProvider;

BlockfrostProvider blockfrostProvider = new BlockfrostProvider("<BF_PROJECT_ID>"); 
```
<hr>

### Get Sundaeswap ADA/iBTC Liquidity Pool Pair

```java
import io.adabox.dextreme.DexFactory;
import io.adabox.dextreme.dex.base.DexType;

import static io.adabox.dextreme.model.AssetType.ADA;
import static io.adabox.dextreme.model.AssetType.iBTC;

Dex sundaeSwapDex = DexFactory.getDex(DexType.Sundaeswap, blockfrostProvider);
Asset assetA = ADA.getAsset();
Asset assetB = iBTC.getAsset();
List<LiquidityPool> liquidityPoolList = sundaeSwapDex.getLiquidityPools(assetA, assetB);
```

<p style="text-align: center">
    <a href="CONTRIBUTING.md">:triangular_ruler: Contributing</a>
</p>