/*
 * Copyright (c) 2019 ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thanksmister.bitcoin.localtrader.ui.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.SharedPreferences
import android.location.*
import android.location.Address
import android.os.Bundle
import android.text.TextUtils
import com.google.android.gms.location.LocationRequest
import com.thanksmister.bitcoin.localtrader.BaseApplication
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.network.api.model.*
import com.thanksmister.bitcoin.localtrader.network.api.model.Currency
import com.thanksmister.bitcoin.localtrader.network.exceptions.NetworkException
import com.thanksmister.bitcoin.localtrader.network.exceptions.RetrofitErrorHandler
import com.thanksmister.bitcoin.localtrader.persistence.CurrenciesDao
import com.thanksmister.bitcoin.localtrader.persistence.MethodsDao
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.utils.*
import io.reactivex.Flowable
import pl.charmas.android.reactivelocation2.ReactiveLocationProvider
import retrofit2.HttpException
import timber.log.Timber
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SearchViewModel @Inject
constructor(application: Application, 
            private val methodsDao: MethodsDao, 
            private val currenciesDao: CurrenciesDao,
            private val sharedPreferences: SharedPreferences, 
            private val locationManager: LocationManager,
            private val preferences: Preferences) : BaseViewModel(application) {

    private val address = MutableLiveData<Address>()
    private val addresses = MutableLiveData<List<Address>>()
    private val advertisements = MutableLiveData<List<Advertisement>>()
    
    private val fetcher: LocalBitcoinsFetcher by lazy {
        val endpoint = preferences.getServiceEndpoint()
        val api = LocalBitcoinsApi(getApplication(), endpoint)
        LocalBitcoinsFetcher(getApplication(), api, preferences)
    }

    fun getAdvertisements(): LiveData<List<Advertisement>> {
        return advertisements
    }

    fun getAddresses(): LiveData<List<Address>> {
        return addresses
    }

    fun getAddress(): LiveData<Address> {
        return address
    }

    private fun setAddresses(address: List<Address>) {
        this.addresses.value = address
    }

    private fun setAdvertisements(advertisements: List<Advertisement>) {
        this.advertisements.value = advertisements
    }

    fun setAddress(address: Address) {
        Timber.d("address $address")
        SearchUtils.setSearchLocationAddress(sharedPreferences, address)
        if (address.hasLatitude()) {
            SearchUtils.setSearchLatitude(sharedPreferences, address.latitude)
        }
        if (address.hasLongitude()) {
            SearchUtils.setSearchLongitude(sharedPreferences, address.longitude)
        }
        if (!TextUtils.isEmpty(address.countryCode)) {
            SearchUtils.setSearchCountryCode(sharedPreferences, address.countryCode)
        }
        if (!TextUtils.isEmpty(address.getAddressLine(0))) {
            SearchUtils.setSearchCountryName(sharedPreferences, address.getAddressLine(0))
        }
        this.address.value = address
    }

    init {
        address.value = SearchUtils.getSearchLocationAddress(sharedPreferences)
    }

    fun getMethods(): Flowable<List<Method>> {
        return methodsDao.getItems()
    }

    fun getCurrencies(): Flowable<List<Currency>> {
        return currenciesDao.getItems()
    }

    fun clearSearchLocationAddress() {
        SearchUtils.clearSearchLocationAddress(sharedPreferences)
    }

    fun setSearchCurrency(value: String) {
        SearchUtils.setSearchCurrency(sharedPreferences, value)
    }

    fun getSearchCurrency():String {
        return SearchUtils.getSearchCurrency(sharedPreferences)
    }

    // this is the key
    fun setSearchPaymentMethod(value: String) {
        SearchUtils.setSearchPaymentMethod(sharedPreferences, value)
    }

    fun getSearchPaymentMethod():String {
        return SearchUtils.getSearchPaymentMethod(sharedPreferences)
    }

    fun setSearchTradeType(value: String) {
        SearchUtils.setSearchTradeType(sharedPreferences, value)
    }

    fun getSearchTradeType():String {
        return SearchUtils.getSearchTradeType(sharedPreferences)
    }

    fun setSearchLatitude(value: Double) {
        SearchUtils.setSearchLatitude(sharedPreferences, value)
    }

    fun getSearchLatitude ():Double {
        return SearchUtils.getSearchLatitude(sharedPreferences)
    }

    fun setSearchLongitude(value: Double) {
        SearchUtils.setSearchLongitude(sharedPreferences, value)
    }

    fun getSearchLongitude():Double {
        return SearchUtils.getSearchLongitude(sharedPreferences)
    }

    fun setSearchCountryName(value: String) {
        SearchUtils.setSearchCountryName(sharedPreferences, value)
    }

    fun getSearchCountryName():String {
        return SearchUtils.getSearchCountryName(sharedPreferences)
    }

    fun setSearchCountryCode(value: String) {
        SearchUtils.setSearchCountryCode(sharedPreferences, value)
    }

    private fun getSearchCountryCode():String {
        return SearchUtils.getSearchCountryCode(sharedPreferences)
    }

    fun createContact(tradeType: TradeType?, countryCode: String?, onlineProvider: String?,
                      adId: Int, amount: String, name: String, phone: String,
                      email: String, iban: String, bic: String, reference: String,
                      message: String, sortCode: String, billerCode: String,
                      accountNumber: String, bsb: String, ethereumAddress: String) {

        disposable += fetcher.createContact(adId.toString(), tradeType, countryCode, onlineProvider, amount, name, phone, email,
                iban, bic, reference, message, sortCode, billerCode, accountNumber, bsb, ethereumAddress)
                .applySchedulers()
                .subscribe ({
                    if(it != null) {
                        showAlertMessage(getApplication<BaseApplication>().getString(R.string.toast_trade_request_sent))
                    }
                }, {
                    error -> Timber.e("Error createContact ${error.message}")
                    //{"error": {"message": "Payment method not available.", "error_code": 0}}
                    val errorHandler = RetrofitErrorHandler(getApplication())
                    val networkException = errorHandler.create(error)
                    if (networkException.code == 31) {
                        showNetworkMessage(getApplication<BaseApplication>().getString(R.string.error_trade_requirements), networkException.code)
                    } else {
                        showNetworkMessage(networkException.message, networkException.code)
                    }
                })
    }

    @SuppressLint("MissingPermission")
    fun startLocationMonitoring() {
        val request = LocationRequest.create() //standard GMS LocationRequest
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setNumUpdates(5)
                .setInterval(100)
        val locationProvider = ReactiveLocationProvider(getApplication())
        disposable += locationProvider.getUpdatedLocation(request)
                .timeout(20000, TimeUnit.MILLISECONDS)
                .applySchedulersComputation()
                .subscribe ({
                    if (it == null) {
                        reverseLocationLookup(it);
                    } else {
                        getLocationFromLocationManager()
                    }
                }, {
                    error -> Timber.e("Error startLocationMonitoring ${error.message}")
                    getLocationFromLocationManager()
                })
    }

    private fun getLocationFromLocationManager() {
        Timber.d("getLocationFromLocationManager")
        val criteria = Criteria()
        criteria.accuracy = Criteria.ACCURACY_COARSE
        try {
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location?) {
                    if (location != null) {
                        locationManager.removeUpdates(this)
                        reverseLocationLookup(location)
                    } else {
                        //dialogUtils.hideProgressDialog()
                        showAlertMessage(getApplication<BaseApplication>().getString(R.string.error_location_message))
                    }
                }
                override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
                    Timber.d("onStatusChanged: $status")
                }
                override fun onProviderEnabled(provider: String) {
                    Timber.d("onProviderEnabled")
                }
                override fun onProviderDisabled(provider: String) {
                    Timber.d("onProviderDisabled")
                }
            }
            locationManager.requestLocationUpdates(locationManager.getBestProvider(criteria, true), 100, 5f, locationListener)
        } catch (e: IllegalArgumentException) {
            Timber.e("Location manager could not use network provider", e)
            showAlertMessage(getApplication<BaseApplication>().getString(R.string.error_location_message))
        } catch (e: SecurityException) {
            Timber.e("Location manager could not use network provider", e)
        }
    }

    fun doAddressLookup(locationName: String) {
        val locationProvider =  ReactiveLocationProvider(getApplication());
        disposable += locationProvider.getGeocodeObservable(locationName, MAX_ADDRESSES)
                .applySchedulersComputation()
                .subscribe ({
                    if (it != null) {
                        setAddresses(it)
                    }
                }, {
                    error -> Timber.e("Error doAddressLookup ${error.message}")
                    showAlertMessage(getApplication<BaseApplication>().getString(R.string.error_address_lookup_description))
                })
    }

    private fun reverseLocationLookup(location: Location) {
        val locationProvider = ReactiveLocationProvider(getApplication())
        disposable += locationProvider.getReverseGeocodeObservable(location.getLatitude(), location.getLongitude(), 1)
                .applySchedulersComputation()
                .filter {items -> items.isNotEmpty()}
                .map { items -> items[0]}
                .subscribe ({
                    if (it == null) {
                        Timber.d("Address reverseLocationLookup error");
                        showAlertMessage(getApplication<BaseApplication>().getString(R.string.error_dialog_no_address_edit));
                    } else {
                        setAddress(it);
                    }
                }, {
                    error -> Timber.e("Error reverseLocationLookup ${error.message}")
                    showAlertMessage(getApplication<BaseApplication>().getString(R.string.error_address_lookup_description))
                })
    }

    fun getPlaces(tradeType: TradeType) {
        val latitude = SearchUtils.getSearchLatitude(sharedPreferences)
        val longitude = SearchUtils.getSearchLongitude(sharedPreferences)
        disposable += fetcher.getPlaces(latitude, longitude)
                .applySchedulers()
                .filter {items -> items.isNotEmpty()}
                .map { items -> items[0]}
                .subscribe ({
                    if (it != null) {
                        getAdvertisementsInPlace(it, tradeType)
                    }
                }, {
                    error -> Timber.e("Error getPlaces ${error.message}")
                    when (error) {
                        is HttpException -> {
                            val errorHandler = RetrofitErrorHandler(getApplication())
                            val networkException = errorHandler.create(error)
                            handleNetworkException(networkException)
                        }
                        is NetworkException -> handleNetworkException(error)
                        is SocketTimeoutException -> handleSocketTimeoutException()
                        else -> showAlertMessage(error.message)
                    }
                })
    }

    fun getOnlineAdvertisements(tradeType: TradeType) {
        val currency = getSearchCurrency()
        val paymentKey = getSearchPaymentMethod()
        val countryName = getSearchCountryName()
        val countryCode = getSearchCountryCode()
        val url = if (tradeType == TradeType.ONLINE_BUY) {
            "buy-bitcoins-online";
        } else {
            "sell-bitcoins-online";
        }
        if (countryName.toLowerCase() != "any" && paymentKey.toLowerCase() == "all") {
            val countryNameFix = countryName.replace(" ", "-");
            disposable += fetcher.searchOnlineAds(url, countryCode, countryNameFix)
                    .applySchedulers()
                    .subscribe ({
                       setAdvertisements(it)
                    }, {
                        error -> Timber.e("Error getOnlineAdvertisements ${error.message}")
                        //{"error": {"message": "Payment method not available.", "error_code": 0}}
                        when (error) {
                            is HttpException -> {
                                val errorHandler = RetrofitErrorHandler(getApplication())
                                val networkException = errorHandler.create(error)
                                handleNetworkException(networkException)
                            }
                            is NetworkException -> handleNetworkException(error)
                            is SocketTimeoutException -> handleSocketTimeoutException()
                            else -> showAlertMessage(error.message)
                        }
                    })
        } else if (countryName.toLowerCase() != "any" && paymentKey.toLowerCase() != "all") {
            val countryNameFix = countryName.replace(" ", "-");
            disposable += fetcher.searchOnlineAds(url, countryCode, countryNameFix, paymentKey)
                    .applySchedulers()
                    .subscribe ({
                        setAdvertisements(it)
                    }, {
                        error -> Timber.e("Error getOnlineAdvertisements ${error.message}")
                        when (error) {
                            is HttpException -> {
                                val errorHandler = RetrofitErrorHandler(getApplication())
                                val networkException = errorHandler.create(error)
                                handleNetworkException(networkException)
                            }
                            is NetworkException -> handleNetworkException(error)
                            is SocketTimeoutException -> handleSocketTimeoutException()
                            else -> showAlertMessage(error.message)
                        }
                    })
        } else if (paymentKey.toLowerCase() == "all" && currency.toLowerCase() != "any") {
            disposable += fetcher.searchOnlineAdsCurrency(url, currency)
                    .applySchedulers()
                    .subscribe ({
                        setAdvertisements(it)
                    }, {
                        error -> Timber.e("Error getOnlineAdvertisements ${error.message}")
                        when (error) {
                            is HttpException -> {
                                val errorHandler = RetrofitErrorHandler(getApplication())
                                val networkException = errorHandler.create(error)
                                handleNetworkException(networkException)
                            }
                            is NetworkException -> handleNetworkException(error)
                            is SocketTimeoutException -> handleSocketTimeoutException()
                            else -> showAlertMessage(error.message)
                        }
                    })
        } else if (paymentKey.toLowerCase() != "all" && currency.toLowerCase() == "any") {
            disposable += fetcher.searchOnlineAdsPayment(url, paymentKey)
                    .applySchedulers()
                    .subscribe ({
                        setAdvertisements(it)
                    }, {
                        error -> Timber.e("Error getOnlineAdvertisements ${error.message}")
                        when (error) {
                            is HttpException -> {
                                val errorHandler = RetrofitErrorHandler(getApplication())
                                val networkException = errorHandler.create(error)
                                handleNetworkException(networkException)
                            }
                            is NetworkException -> handleNetworkException(error)
                            is SocketTimeoutException -> handleSocketTimeoutException()
                            else -> showAlertMessage(error.message)
                        }
                    })
        } else if (paymentKey.toLowerCase() != "all" && currency.toLowerCase() != "any") {
            disposable += fetcher.searchOnlineAdsCurrencyPayment(url, currency, paymentKey)
                    .applySchedulers()
                    .subscribe ({
                        setAdvertisements(it)
                    }, {
                        error -> Timber.e("Error getOnlineAdvertisements ${error.message}")
                        when (error) {
                            is HttpException -> {
                                val errorHandler = RetrofitErrorHandler(getApplication())
                                val networkException = errorHandler.create(error)
                                handleNetworkException(networkException)
                            }
                            is NetworkException -> handleNetworkException(error)
                            is SocketTimeoutException -> handleSocketTimeoutException()
                            else -> showAlertMessage(error.message)
                        }
                    })
        } else {
            disposable += fetcher.searchOnlineAdsAll(url)
                    .applySchedulers()
                    .subscribe ({
                        setAdvertisements(it)
                    }, {
                        error ->
                        Timber.e("Error getOnlineAdvertisements ${error.message}")
                        when (error) {
                            is HttpException -> {
                                val errorHandler = RetrofitErrorHandler(getApplication())
                                val networkException = errorHandler.create(error)
                                handleNetworkException(networkException)
                            }
                            is NetworkException -> handleNetworkException(error)
                            is SocketTimeoutException -> handleSocketTimeoutException()
                            else -> showAlertMessage(error.message)
                        }
                    })
        }
    }

    private fun getAdvertisementsInPlace(place: Place, tradeType: TradeType) {
        var url: String? = null
        if (tradeType == TradeType.LOCAL_BUY && place.buyLocalUrl != null) {
            url = when {
                place.buyLocalUrl!!.contains("https://localbitcoins.com/") -> place.buyLocalUrl!!.replace("https://localbitcoins.com/", "")
                place.buyLocalUrl!!.contains("https://localbitcoins.net/") -> place.buyLocalUrl!!.replace("https://localbitcoins.net/", "")
                else -> place.buyLocalUrl
            }
        } else if (tradeType == TradeType.LOCAL_SELL && place.sellLocalUrl != null) {
            url = when {
                place.sellLocalUrl!!.contains("https://localbitcoins.com/") -> place.sellLocalUrl!!.replace("https://localbitcoins.com/", "")
                place.sellLocalUrl!!.contains("https://localbitcoins.net/") -> place.sellLocalUrl!!.replace("https://localbitcoins.net/", "")
                else -> place.sellLocalUrl
            }
        }
        val split = url!!.split("/")
        disposable += fetcher.searchAdsByPlace(split[0], split[1], split[2])
                .applySchedulers()
                .filter {items -> items.isNotEmpty()}
                .subscribe ({
                    Collections.sort(it, AdvertisementNameComparator())
                    setAdvertisements(it)
                }, {
                    error -> Timber.e("Error getAdvertisementsInPlace ${error.message}")
                    when (error) {
                        is HttpException -> {
                            val errorHandler = RetrofitErrorHandler(getApplication())
                            val networkException = errorHandler.create(error)
                            handleNetworkException(networkException)
                        }
                        is NetworkException -> handleNetworkException(error)
                        is SocketTimeoutException -> handleSocketTimeoutException()
                        else -> showAlertMessage(error.message)
                    }
                })
    }

    class AdvertisementNameComparator : Comparator<Advertisement> {
        override fun compare(a1: Advertisement, a2: Advertisement): Int {
            try {
                return java.lang.Double.compare(Doubles.convertToDouble(a1.distance), Doubles.convertToDouble(a2.distance))
            } catch (e: Exception) {
                return 0
            }
        }
    }

    companion object {
        const val MAX_ADDRESSES = 5
    }
}