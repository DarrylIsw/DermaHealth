package com.example.dermahealth.api

import com.example.dermahealth.data.OBFResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OBFService {

    // Retrofit GET request to fetch products from Open Beauty Facts API
    // Endpoint: "cgi/search.pl"
    @GET("cgi/search.pl")
    suspend fun getProducts(
        // Search keyword, default is "skincare"
        @Query("search_terms") searchTerms: String = "skincare",

        // Simple search flag, 1 = yes
        @Query("search_simple") simple: Int = 1,

        // Action type for API, usually "process"
        @Query("action") action: String = "process",

        // Return results in JSON format, 1 = yes
        @Query("json") json: Int = 1,

        // Page number for pagination
        @Query("page") page: Int = 1,

        // Number of results per page
        @Query("page_size") pageSize: Int = 20
    ): OBFResponse // Returns a response object representing the API result
}

