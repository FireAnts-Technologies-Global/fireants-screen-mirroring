package com.anrstudio.template.data.iptv.model

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
@Entity(tableName = "tb_tv_channel_source")
data class TVChannelSource(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo("id")
    @SerializedName("id")
    val id: Long = 0,

    @ColumnInfo("name")
    @SerializedName("name")
    val name: String = "",

    @ColumnInfo("sourceUrl")
    @SerializedName("sourceUrl")
    val sourceUrl: String = "", // URL, file path, or device path

    @ColumnInfo("sourceType")
    @SerializedName("sourceType")
    val sourceType: String = TVChannelSourceType.URL.value, // URL, STREAM, FILE, DEVICE

    @ColumnInfo("importDate")
    @SerializedName("importDate")
    val importDate: Long = System.currentTimeMillis(),

    @ColumnInfo("channelCount")
    @SerializedName("channelCount")
    var channelCount: Int = 0,

    @ColumnInfo("groupCount")
    @SerializedName("groupCount")
    var groupCount: Int = 0,

    @ColumnInfo("isActive")
    @SerializedName("isActive")
    var isActive: Boolean = true,

    @ColumnInfo("lastUpdateTime")
    @SerializedName("lastUpdateTime")
    var lastUpdateTime: Long = System.currentTimeMillis(),

    @ColumnInfo("description")
    @SerializedName("description")
    val description: String = ""
) : Parcelable {

    fun getSourceTypeEnum(): TVChannelSourceType {
        return TVChannelSourceType.fromString(sourceType)
    }
}


