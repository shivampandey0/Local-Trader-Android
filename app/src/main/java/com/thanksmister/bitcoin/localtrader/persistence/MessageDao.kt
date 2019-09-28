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

import androidx.room.*
import androidx.room.Transaction
import com.thanksmister.bitcoin.localtrader.network.api.model.Message

import io.reactivex.Flowable

/**
 * Data Access Object for the messages table.
 */
@Dao
interface MessageDao {

    /**
     * Get a message by id.
     * @return the message from the table with a specific id.
     */
    @Query("SELECT * FROM Messages WHERE contactId = :contact_id")
    fun getMessageById(contact_id: String): Flowable<Message>

    /**
     * Get all messages
     * @return list of all messages
     */
    @Query("SELECT * FROM Messages")
    fun getMessages(): Flowable<List<Message>>

    /**
     * Insert a message in the database. If the message already exists, replace it.
     * @param user the message to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessage(message: Message)

    /**
     * Delete all messages.
     */
    @Query("DELETE FROM Messages")
    fun deleteAllMessages()
}