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

import android.arch.persistence.room.*
import io.reactivex.Flowable

/**
 * Data Access Object for the messages table.
 */
@Dao
interface CurrencyDao {

    /**
     * Get all messages
     * @return list of all messages
     */
    @Query("SELECT * FROM Currency")
    fun getItems(): Flowable<List<Currency>>

    /**
     * Insert a message in the database. If the message already exists, replace it.
     * @param user the message to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(items: List<Currency>)

    @Transaction
    fun updateItem(items: List<Currency>) {
        deleteAllItems()
        insertAll(items)
    }

    /**
     * Delete all messages.
     */
    @Query("DELETE FROM Currency")
    fun deleteAllItems()
}