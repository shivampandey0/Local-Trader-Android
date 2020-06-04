/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.network.api.model

import androidx.room.*
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity
class Profile() : Parcelable {

    @SerializedName("username")
    @Expose
    var username: String = ""

    @SerializedName("feedback_score")
    @Expose
    var feedbackScore: Int = 0

    @SerializedName("last_online")
    @Expose
    var lastOnline: String? = null

    @SerializedName("trade_count")
    @Expose
    var tradeCount: String? = null

    @SerializedName("name")
    @Expose
    var name: String? = null

    constructor(parcel: Parcel) : this() {
        username = parcel.readString()
        feedbackScore = parcel.readInt()
        lastOnline = parcel.readString()
        tradeCount = parcel.readString()
        name = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(username)
        parcel.writeInt(feedbackScore)
        parcel.writeString(lastOnline)
        parcel.writeString(tradeCount)
        parcel.writeString(name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Profile> {
        override fun createFromParcel(parcel: Parcel): Profile {
            return Profile(parcel)
        }

        override fun newArray(size: Int): Array<Profile?> {
            return arrayOfNulls(size)
        }
    }


}