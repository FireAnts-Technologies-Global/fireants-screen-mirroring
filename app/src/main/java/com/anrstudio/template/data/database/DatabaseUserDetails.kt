package com.anrstudio.template.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.anrstudio.template.data.domain.UserDetails

@Entity
data class DatabaseUserDetails(
    @PrimaryKey
    val user: String,
    val avatar: String,
    val name: String,
    val userSince: String,
    val location: String
)

fun DatabaseUserDetails.asDomainModel(): UserDetails {
    return UserDetails(
        user = user,
        avatar = avatar,
        name = name,
        userSince = userSince,
        location = location
    )
}