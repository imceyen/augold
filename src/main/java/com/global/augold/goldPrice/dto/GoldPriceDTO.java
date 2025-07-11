package com.global.augold.goldPrice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GoldPriceDTO {

    @JsonProperty("effective_date")
    private String effectiveDate;

    @JsonProperty("price_per_gram")
    private double pricePerGram;
}

