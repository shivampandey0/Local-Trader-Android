/*
 * Copyright (c) 2020 ThanksMister LLC
 *  http://www.thanksmister.com
 *
 *  Mozilla Public License 2.0
 *
 *  Permissions of this weak copyleft license are conditioned on making
 *  available source code of licensed files and modifications of those files
 *  under the same license (or in certain cases, one of the GNU licenses).
 *  Copyright and license notices must be preserved. Contributors provide
 *  an express grant of patent rights. However, a larger work using the
 *  licensed work may be distributed under different terms and without source
 *  code for files added in the larger work.
 */

package com.thanksmister.bitcoin.localtrader.persistence

import androidx.room.*
import androidx.room.Transaction
import com.thanksmister.bitcoin.localtrader.network.api.model.Currency


import io.reactivex.Flowable

/**
 * Data Access Object.
 */
@Dao
interface CurrenciesDao {
    /**
     * Get all items
     * @return list of all data items.
     */
    @Query("SELECT * FROM Currencies")
    fun getItems(): Flowable<List<Currency>>

    /**
     * Insert feeds in the database. If the feed already exists, replace it.
     * @param feed the feed to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(items: List<Currency>)

    @Transaction
    fun replaceItem(items: List<Currency>) {
        deleteAllItems()
        insertAll(items)
    }

    /**
     * Delete all items.
     */
    @Query("DELETE FROM Currencies")
    fun deleteAllItems()
}