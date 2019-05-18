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

package com.thanksmister.bitcoin.localtrader.persistence

import android.arch.persistence.room.*
import android.arch.persistence.room.Transaction
import com.thanksmister.bitcoin.localtrader.network.api.model.*

import io.reactivex.Flowable

/**
 * Data Access Object.
 */
@Dao
interface NotificationsDao {

    /**
     * Get a item by id.
     * @return the item from the table with a specific id.
     */
    @Query("SELECT * FROM Notifications WHERE notificationId = :id")
    fun getItemById(id: String): Flowable<Notification>

    /**
     * Get a unread item by contact id.
     * @return the item from the table with a specific id and read is false.
     */
    @Query("SELECT * FROM Notifications WHERE read = :read")
    fun getItemUnreadItems(read: Boolean): Flowable<List<Notification>>

    /**
     * Get a unread item by contact id.
     * @return the item from the table with a specific id and read is false.
     */
    @Query("SELECT * FROM Notifications WHERE contactId = :id AND read = :read")
    fun getItemUnreadItemByContactId(id: Int, read: Boolean): Flowable<Notification>

    /**
     * Get all items ordered by date
     * @return list of all items with specific date
     */
    @Query("SELECT * FROM Notifications ORDER BY createdAt DESC")
    fun getItems(): Flowable<List<Notification>>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateItem(item: Notification)

    /**
     * Insert an item in the database. If the item already exists, replace it.
     * @param item the item to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertItem(item: Notification)

    /**
     * Insert an item in the database. If the item already exists, replace it.
     * @param item the item to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertItems(items: List<Notification>)

    @Transaction
    fun replaceItems(items: List<Notification>) {
        deleteAllItems()
        insertItems(items)
    }

    /**
     * Delete all items.
     */
    @Query("DELETE FROM Notifications WHERE notificationId = :id")
    fun deleteItem(id: String)

    /**
     * Delete all items.
     */
    @Query("DELETE FROM Notifications")
    fun deleteAllItems()
}