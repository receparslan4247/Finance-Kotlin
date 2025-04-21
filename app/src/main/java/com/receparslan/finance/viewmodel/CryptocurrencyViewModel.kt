package com.receparslan.finance.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receparslan.finance.model.Cryptocurrency
import com.receparslan.finance.servics.CryptocurrencyAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

class CryptocurrencyViewModel() : ViewModel() {

    companion object {
        private var page = 1 // Current page number for pagination
    }

    // State variables to hold the list of cryptocurrencies and their search results
    val cryptocurrencyList = mutableStateListOf<Cryptocurrency>()
    val cryptocurrencySearchList = mutableStateListOf<Cryptocurrency>()
    val cryptocurrencyGainerList = mutableStateListOf<Cryptocurrency>()
    val cryptocurrencyLoserList = mutableStateListOf<Cryptocurrency>()

    var isLoading = mutableStateOf(false) // Loading state to show/hide loading indicators

    // This function is called when the ViewModel is initialized to fetch the initial list of cryptocurrencies.
    init {
        addCryptocurrenciesByPage(page) // Fetch the initial list of cryptocurrencies
        setGainersAndLosersList() // Set the gainers and losers list
    }

    // This function is used to refresh the list of cryptocurrencies.
    fun refreshCryptocurrencies() {
        page = 1 // Reset the page number to 1
        cryptocurrencyList.clear() // Clear the existing list of cryptocurrencies
        addCryptocurrenciesByPage(page) // Fetch the initial list of cryptocurrencies
        setGainersAndLosersList() // Set the gainers and losers list
    }

    // This function is used to set the current page number for pagination.
    fun loadMore() {
        println("isLoading = ${isLoading.value}")
        if (!isLoading.value) {
            println("page = $page")
            page++
            addCryptocurrenciesByPage(page)
        }
    }

    // This function fetches the list of cryptocurrencies from the API and updates the cryptocurrencyList variable.
    private fun addCryptocurrenciesByPage(page: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading.value = true // Set loading state to true

            // Make a network request to fetch the list of cryptocurrencies
            repeat(15) { attempt ->
                try {
                    val response = CryptocurrencyAPI.retrofitService.getCryptocurrencyListByPage(page)

                    if (response.isSuccessful) {
                        response.body()?.let {
                            cryptocurrencyList.addAll(it.filter { newItem -> newItem !in cryptocurrencyList })
                        }
                        return@launch
                    }
                } catch (e: Exception) {
                    println("Exception occurred: ${e.message}")
                }

                delay(5000) // Wait for 5 seconds before retrying
            }
        }.invokeOnCompletion { isLoading.value = false } // Set loading state to false when the loading is completed
    }

    // This function fetches a list of cryptocurrencies based on the search query and returns the results.
    fun searchCryptocurrencies(name: String){
        viewModelScope.launch(Dispatchers.IO) {
            isLoading.value = true // Set loading state to true

            // Make a network request to fetch the list of cryptocurrencies
            repeat(15) { attempt ->
                try {
                    val response = CryptocurrencyAPI.retrofitService.searchCryptocurrency(name)

                    if (response.isSuccessful) {
                        // If the response is successful, add the cryptocurrencies to the search list
                        response.body()?.let { cryptocurrencyList ->
                            val ids = cryptocurrencyList.cryptocurrencies.joinToString(",") { it.id }// Get the list of cryptocurrency IDs

                            try {
                                val response = CryptocurrencyAPI.retrofitService.getCryptocurrencyByIds(ids)

                                // If the response is successful, add the cryptocurrencies to the search list
                                if (response.isSuccessful) {
                                    response.body()?.let {
                                        cryptocurrencySearchList.clear()
                                        cryptocurrencySearchList.addAll(it)
                                    }
                                    return@launch
                                } else{
                                    println("Response unsuccessful: ${response.code()}")
                                    delay(5000) // Wait for 5 second before retrying
                                }
                            } catch (e: Exception) {
                                println("Exception occurred: ${e.message}")
                            }
                        }
                    } else{
                        println("Response unsuccessful: ${response.code()}")
                        delay(5000) // Wait for 5 seconds before retrying
                    }
                } catch (e: Exception) {
                    println("Exception occurred: ${e.message}")
                }
            }
        }.invokeOnCompletion { isLoading.value = false } // Set loading state to false when the search is completed
    }

    // This function fetches the list of gainers and losers from the CoinGecko website using Jsoup.
    private fun setGainersAndLosersList() {
        val url = "https://www.coingecko.com/en/crypto-gainers-losers" // URL for gainers and losers

        val client = OkHttpClient() // Create an instance of OkHttpClient

        // Create a request to fetch the HTML content from the URL
        val request = Request.Builder()
            .url(url)
            .build()

        // Clear the previous lists
        cryptocurrencyGainerList.clear()
        cryptocurrencyLoserList.clear()

        // Make a network request to fetch the HTML content
        viewModelScope.launch(Dispatchers.IO){
            try {
                val response = client.newCall(request).execute()

                if (response.isSuccessful){
                    var html : String = response.body?.string() ?: "" // Get the HTML response as a string
                    val document = Jsoup.parse(html) // Parse the HTML response using Jsoup
                    val headers = document.select("tbody") // Select the table body from the HTML document
                    val gainers = headers[0] // Select the first table body for gainers
                    val losers = headers[1] // Select the second table body for losers

                    // Iterate through the rows of the gainers tables
                    gainers.select("tr").forEach {
                        val symbol = it.select("a").select("div > div > div").text() // Get the symbol of the cryptocurrency
                        val name = it.select("a").select("div > div").text().substringBeforeLast("$symbol $symbol") // Get the name of the cryptocurrency
                        val image = it.select("img").attr("src").substringBefore("?") // Get the image URL of the cryptocurrency
                        val price = it.select("td")[3].text().substringAfter("$").toDouble() // Get the price of the cryptocurrency
                        val change = it.select("td")[5].text().substringBefore("%").toDouble() // Get the change percentage of the cryptocurrency

                        val cryptocurrency = Cryptocurrency(
                            id = "",
                            name = name,
                            symbol = symbol,
                            image = image,
                            currentPrice = price,
                            priceChangePercentage24h = change,
                            lastUpdated = ""
                        )

                        cryptocurrencyGainerList.add(cryptocurrency) // Add the cryptocurrency to the gainers list
                    }

                    // Iterate through the rows of the losers table
                    losers.select("tr").forEach {
                        val symbol = it.select("a").select("div > div > div").text() // Get the symbol of the cryptocurrency
                        val name = it.select("a").select("div > div").text().substringBeforeLast("$symbol $symbol") // Get the name of the cryptocurrency
                        val image = it.select("img").attr("src").substringBefore("?") // Get the image URL of the cryptocurrency
                        val price = it.select("td")[3].text().substringAfter("$").toDouble() // Get the price of the cryptocurrency
                        val change = it.select("td")[5].text().substringBefore("%").toDouble() // Get the change percentage of the cryptocurrency

                        val cryptocurrency = Cryptocurrency(
                            id = "",
                            name = name,
                            symbol = symbol,
                            image = image,
                            currentPrice = price,
                            priceChangePercentage24h = change,
                            lastUpdated = ""
                        )

                        cryptocurrencyLoserList.add(cryptocurrency) // Add the cryptocurrency to the losers list
                    }
                }else
                    println("Response unsuccessful: ${response.message}")
            }catch (e : Exception){
                println("Exception occurred: ${e.message}")
            }
        }
    }
}