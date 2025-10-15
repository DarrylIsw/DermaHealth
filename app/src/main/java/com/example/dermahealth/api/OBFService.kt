package com.example.dermahealth.api

import com.example.dermahealth.data.OBFResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OBFService {
    @GET("cgi/search.pl")
    suspend fun getProducts(
        @Query("search_terms") searchTerms: String = "skincare",
        @Query("search_simple") simple: Int = 1,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): OBFResponse
}
