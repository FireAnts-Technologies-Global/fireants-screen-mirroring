package com.hyper.castwave.data.iptv.model

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hyper.castwave.data.iptv.database.manager.ChannelManager.Companion.UNKNOW_FROUP_NAME
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
@Entity(
    tableName = "tb_tv_channel",
    indices = [
        Index("url"),
        Index("groupId"),
        Index("sourceId"),
        Index("isFavorite"),
        Index(value = ["url", "sourceId"], unique = true)
    ]
)
data class TVChannel(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo("id")
    @SerializedName("id")
    val id: Long = 0,

    @ColumnInfo("url")
    @SerializedName("url")
    val url: String = "",

    @ColumnInfo("groupId")
    @SerializedName("groupId")
    var groupId: String = "",

    @ColumnInfo("sourceId")
    @SerializedName("sourceId")
    var sourceId: Long = 0,

    @ColumnInfo("name")
    @SerializedName("name")
    val name: String = "",

    @ColumnInfo("groupTitle")
    @SerializedName("groupTitle")
    val groupTitle: String = "",

    @ColumnInfo("logo")
    @SerializedName("logo")
    val logo: String = "",

    @ColumnInfo("tvgId")
    @SerializedName("tvgId")
    var tvgId: String = "",

    @ColumnInfo("isFavorite")
    @SerializedName("isFavorite")
    var isFavorite: Boolean = false,

    @ColumnInfo("connectedTime")
    @SerializedName("connectedTime")
    var connectedTime: String = "",

    @ColumnInfo("addedTime")
    @SerializedName("addedTime")
    var addedTime: String = "",
) : Parcelable {

    companion object {
        fun convertFromOnline(
            channelOnline: ChannelOnline,
            tvChannelGroup: TVChannelGroup?,
            sourceId: Long = 0
        ) = TVChannel(
            id = 0,
            url = channelOnline.url,
            groupId = tvChannelGroup?.name ?: UNKNOW_FROUP_NAME,
            sourceId = sourceId,
            name = channelOnline.name,
            groupTitle = tvChannelGroup?.displayName ?: channelOnline.groupTitle,
            logo = channelOnline.logo,
            tvgId = channelOnline.tvgId,
            connectedTime = "",
            addedTime = System.currentTimeMillis().toString()
        )
    }
}