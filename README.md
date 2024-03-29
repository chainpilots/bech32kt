# bech32kt
bech32kt is a Kotlin implementation of the [Bech32](https://en.bitcoin.it/wiki/Bech32) address format.

The implementation is heavily based on [Bitcoinj Bech32 implementation](https://github.com/bitcoinj/bitcoinj/blob/master/core/src/main/java/org/bitcoinj/core/Bech32.java).

# Downloading
This library is available on Jitpack: [![](https://jitpack.io/v/chainpilots/bech32kt.svg)](https://jitpack.io/#chainpilots/bech32kt)

In order to download it, firstly include the Jitpack repository on your project `build.gradle.kts` file:

```kts
allprojects {
    repositories {
        maven { setUrl("https://jitpack.io") }
    }
}
```

Then, insert the following dependency inside your module's `build.gradle.kts` file:

```kts 
dependencies {
    implementation("com.github.chainpilots:bech32kt:{version}")
}
```

# Usage

```kotlin
// Encode given a human-readable part and a byte array
val bech32 = Bech32.encode(hrp = "bc", data = byteArrayOf(1, 2 ,3).toWords())
val bech32m = Bech32M.encode(hrp = "bc", data = byteArrayOf(1, 2 ,3).toWords())

// Encode given a human-readable part and a byte array with extension
val bech32 = byteArrayOf(1, 2 ,3).encodeToBech32(hrp = "bc")
val bech32m = byteArrayOf(1, 2 ,3).encodeToBechm32(hrp = "bc")

// Decode the data
val bech32Value = "abcdef1qpzry9x8gf2tvdw0s3jn54khce6mua7lmqqqxw"
val (hrp, data) = Bech32.decode(bech32Value)

val bech32mValue = "abcdef1qpzry9x8gf2tvdw0s3jn54khce6mua7lmqqqxw"
val (hrp, data) = Bech32M.decode(bech32Value)

// Decode the data with extension
val bech32Value = "abcdef1qpzry9x8gf2tvdw0s3jn54khce6mua7lmqqqxw"
val (hrp, data) = bech32Value.decodeBech32ToBech32Data()
```