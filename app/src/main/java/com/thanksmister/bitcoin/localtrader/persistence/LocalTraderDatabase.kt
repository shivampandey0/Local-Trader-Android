/*
 * Copyright (c) 2018 ThanksMister LLC
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

package com.thanksmister.bitcoin.localtrader.persistence

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context

/**
 * The Room database that contains the Messages table
 */
@Database(entities = arrayOf(User::class, Notification::class, Wallet::class, Currency::class, Method::class, Rate::class), version = 8, exportSchema = false)
abstract class LocalTraderDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun notificationDao(): NotificationDao
    abstract fun walletDao(): WalletDao
    abstract fun currencyDao(): CurrencyDao
    abstract fun methodDao(): MethodDao
    abstract fun rateDao(): RateDao

    companion object {

        @Volatile private var INSTANCE: LocalTraderDatabase? = null

        @JvmStatic fun getInstance(context: Context): LocalTraderDatabase =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
                }

        private fun buildDatabase(context: Context) =
                Room.databaseBuilder(context.applicationContext,
                        LocalTraderDatabase::class.java, "local_trader.db")
                        .fallbackToDestructiveMigration()
                        .build()
    }
}