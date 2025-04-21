package com.receparslan.finance.servics

import com.receparslan.finance.model.Cryptocurrency
import com.receparslan.finance.model.CryptocurrencyList
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private const val BASE_URL = "https://api.coingecko.com/api/v3/"

private val retrofit = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

interface CryptocurrencyApiService {
    @GET("coins/markets?vs_currency=usd&per_page=250")
    suspend fun getCryptocurrencyListByPage(@Query("page") page: Int): Response<List<Cryptocurrency>>

    @GET("search")
    suspend fun searchCryptocurrency(@Query("query") query: String): Response<CryptocurrencyList>

    @GET("coins/markets?vs_currency=usd")
    suspend fun getCryptocurrencyByIds(@Query("ids") ids: String): Response<List<Cryptocurrency>>
}

object CryptocurrencyAPI {
    val retrofitService: CryptocurrencyApiService by lazy {
        retrofit.create(CryptocurrencyApiService::class.java)
    }
}