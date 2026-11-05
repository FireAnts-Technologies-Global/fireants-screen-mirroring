package com.anrstudio.template.data.iptv.network

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Query

interface TVChannelService {

    @GET("api/v1/servers")
    suspend fun getListVpn(
        @HeaderMap headerMap: Map<String, String>,
        @Query("page") page: Int = 1,
        @Query("pageSize") size: Int = 100
    ): ResponseBody

}