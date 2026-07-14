package com.fmartinier.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ScryfallCardDto(
    @JsonProperty("id") val id: String,
    @JsonProperty("name") val nameEn: String,
    @JsonProperty("printed_name") val nameFr: String?,
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
    @JsonProperty("eur_foil") val eurFoil: String?
)