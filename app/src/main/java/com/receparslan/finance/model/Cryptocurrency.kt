package com.receparslan.finance.model

import com.google.gson.annotations.SerializedName

data class CryptocurrencyList(
    @SerializedName("coins")
    val cryptocurrencies: List<Cryptocurrency>
)

data class Cryptocurrency(
    val id: String,
    val name: String,
    val symbol: String,
    val image: String,

    @SerializedName("current_price")
    val currentPrice: Double,

    @SerializedName("price_change_percentage_24h")
    val priceChangePercentage24h: Double,

    @SerializedName("last_updated")
    val lastUpdated: String,
)