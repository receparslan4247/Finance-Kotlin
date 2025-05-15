package com.receparslan.finance.viewmodel

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.receparslan.finance.database.CryptocurrencyDatabase
import com.receparslan.finance.model.Cryptocurrency
import com.receparslan.finance.model.KlineData
import com.receparslan.finance.servics.CryptocurrencyAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.ceil

class DetailViewModel(application: Application) : AndroidViewModel(application) {
    lateinit var cryptocurrency: MutableState<Cryptocurrency>

    val savedCryptocurrencyIds = mutableStateOf<List<String>>(emptyList()) // List to hold the saved cryptocurrency IDs

    val klineDataHistoryList = mutableStateOf<List<KlineData>>(emptyList()) // List to hold the historical Kline data

    var isLoading = mutableStateOf(false)

    init {
        observeSavedCryptocurrencies()
    }

    // This function is used to refresh the detail screen by clearing the existing Kline data history list and fetching new data.
    fun refreshDetailScreen() {
        updateCryptocurrency()
        klineDataHistoryList.value = emptyList()
        setCryptocurrencyHistory(System.currentTimeMillis() - 24 * 60 * 60 * 1000L, "1m")
    }

    // This function saves a cryptocurrency to the database.
    fun saveCryptocurrency() {
        viewModelScope.launch(Dispatchers.IO) {
            val cryptocurrencyDao = CryptocurrencyDatabase.getDatabase(getApplication<Application>().applicationContext).cryptocurrencyDao()

            cryptocurrencyDao.insertCryptocurrency(cryptocurrency.value)
        }
    }

    // This function deletes a cryptocurrency from the database.
    fun deleteCryptocurrency() {
        viewModelScope.launch(Dispatchers.IO) {
            val cryptocurrencyDao = CryptocurrencyDatabase.getDatabase(getApplication<Application>().applicationContext).cryptocurrencyDao()

            cryptocurrencyDao.deleteCryptocurrency(cryptocurrency.value)
        }
    }

    // This function fetches the updated cryptocurrency data from the API and updates the saved cryptocurrency list.
    fun updateCryptocurrency() {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading.value = true

            try {
                val response = CryptocurrencyAPI.retrofitService
                    .getCryptocurrencyByIds(cryptocurrency.value.id)

                if (response.isSuccessful) {
                    response.body()?.let {
                        cryptocurrency.value = it.firstOrNull() ?: cryptocurrency.value // Get the updated cryptocurrency data
                    }
                }
            } catch (e: Exception) {
                println("API exception: ${e.message}")
            }
        }.invokeOnCompletion { isLoading.value = false } // Set loading state to false when the loading is completed
    }

    // This function fetches the historical Kline data for a given cryptocurrency symbol and returns the data as a list.
    fun setCryptocurrencyHistory(startTime: Long = System.currentTimeMillis() - 24 * 60 * 60 * 1000L, interval: String = "1m") {
        val symbol = cryptocurrency.value.symbol // Get the symbol of the cryptocurrency

        val klineDataHistoryListHolder = mutableStateListOf<KlineData>() // Create a mutable list to hold the historical Kline data

        val endTime = (System.currentTimeMillis() / 1000) * 1000 // Get the current time in milliseconds

        // Calculate the interval time in milliseconds based on the selected interval
        val intervalTimeMillis =
            if (interval == "1m") 60 * 1000L else if (interval == "1h") 60 * 60 * 1000L else if (interval == "1d") 24 * 60 * 60 * 1000L else 1

        // Calculate the repeat number based on the interval time
        var repeatNumber = ceil((endTime - startTime) / intervalTimeMillis / 1000F).toInt()

        viewModelScope.launch(Dispatchers.IO) {
            isLoading.value = true // Set loading state to true

            klineDataHistoryList.value = emptyList() // Clear the existing Kline data history list

            repeat(repeatNumber) { attempt ->
                try {
                    val response = CryptocurrencyAPI.binanceService.getHistoricalDataByRange(
                        symbol = if (symbol.uppercase() == "USDT") "BTCUSDT" else symbol.uppercase() + "USDT",
                        startTime = if (attempt == repeatNumber - 1) startTime - intervalTimeMillis else endTime - (attempt + 1) * intervalTimeMillis * 1000L,
                        endTime = endTime - attempt * intervalTimeMillis * 1000L,
                        interval = interval
                    )
                    if (response.isSuccessful) {
                        // Handle the successful response
                        response.body()?.let {
                            val list = it.map { array ->
                                val array = array as List<*>

                                val data = KlineData(
                                    openTime = (array[0] as Double).toLong(),
                                    open = if (symbol.uppercase() != "USDT") array[1] as String else "1.0",
                                    high = if (symbol.uppercase() != "USDT") array[2] as String else "1.0",
                                    low = if (symbol.uppercase() != "USDT") array[3] as String else "1.0",
                                    close = if (symbol.uppercase() != "USDT") array[4] as String else "1.0",
                                    closeTime = (array[6] as Double).toLong()
                                )

                                data
                            }

                            klineDataHistoryListHolder.addAll(list.filterNot { it in klineDataHistoryListHolder }) // Set the historical Kline data
                        }
                    } else
                        println("Response unsuccessful: $response")
                } catch (e: Exception) {
                    println("Exception occurred: ${e.message}")
                }
            }
        }.invokeOnCompletion {
            isLoading.value = false // Set loading state to false when the loading is completed
            klineDataHistoryList.value = klineDataHistoryListHolder // Update the Kline data history list
        }
    }

    // This function sets the saved cryptocurrencies by fetching them from the database.
    private fun observeSavedCryptocurrencies() {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading.value = true

            val cryptocurrencyDao = CryptocurrencyDatabase.getDatabase(getApplication<Application>().applicationContext).cryptocurrencyDao()

            cryptocurrencyDao.getAllCryptocurrencies().collect { localList ->
                savedCryptocurrencyIds.value = localList.map { it.id }
            }
        }
    }
}