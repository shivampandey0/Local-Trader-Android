/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.persistence

import android.content.SharedPreferences

class BooleanPreference @JvmOverloads constructor(private val preferences: SharedPreferences, private val key: String, private val defaultValue: Boolean = false) {

    val isSet: Boolean
        get() = preferences.contains(key)

    fun get(): Boolean {
        return preferences.getBoolean(key, defaultValue)
    }

    fun set(value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }

    fun delete() {
        preferences.edit().remove(key).apply()
    }
}
