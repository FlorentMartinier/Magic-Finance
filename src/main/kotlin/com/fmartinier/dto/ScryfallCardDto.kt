package com.fmartinier.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ScryfallCardDto(
    @JsonProperty("id") val id: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("image_uris") val imageUris: ImageUris?,
    @JsonProperty("prices") val prices: Prices?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ImageUris(
    @JsonProperty("normal") val normal: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Prices(
    @JsonProperty("eur") val eur: String?,
    @JsonProperty("eur_foil") val eurFoil: String?,
    @JsonProperty("eur_etched") val eurEtched: String?,
    @JsonProperty("usd") val usd: String?,
    @JsonProperty("usd_foil") val usdFoil: String?,
    @JsonProperty("usd_etched") val usdEtched: String?,
    @JsonProperty("tix") val tix: String?,
)