package com.anrstudio.template.data.network

import com.anrstudio.template.data.network.model.NetworkUserListItem
import retrofit2.http.GET

interface UserListService {

    @GET("/repos/square/retrofit/stargazers")
    suspend fun getUserList(): List<NetworkUserListItem>
}