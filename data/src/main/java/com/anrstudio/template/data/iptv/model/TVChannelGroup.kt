package com.hyper.castwave.data.iptv.model

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
@Entity(
    tableName = "tb_tv_channel_group",
    indices = [Index("sourceId")]
)
data class TVChannelGroup(
    @ColumnInfo("name") @PrimaryKey @SerializedName("name") val name: String,
    @ColumnInfo("sourceId") @SerializedName("sourceId") var sourceId: Long = 0, // Reference to TVChannelSource
    @ColumnInfo("displayName") @SerializedName("displayName") val displayName: String = "",
    @ColumnInfo("channelNumber") @SerializedName("channelNumber") var channelNumber: Int = 0,
    @ColumnInfo("isFavorite") @SerializedName("isFavorite") var isFavorite: Boolean = false,
) : Parcelable {

    companion object
}