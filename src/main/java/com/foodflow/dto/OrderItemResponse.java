package com.foodflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Details of an ordered item with historical price snapshot")
public class OrderItemResponse {

    @Schema(description = "Database ID of the order item", example = "101")
    private Long id;

    @Schema(description = "Database ID of the food item", example = "5")
    private Long foodId;

    @Schema(description = "Name of the food item at order time", example = "Paneer Tikka Protein Bowl")
    private String foodName;

    @Schema(description = "Quantity ordered", example = "2")
    private Integer quantity;

    @Schema(description = "Historical snapshot price per unit at the time order was placed", example = "280.00")
    private BigDecimal priceAtOrderTime;

    @Schema(description = "Subtotal for this item (priceAtOrderTime * quantity)", example = "560.00")
    private BigDecimal subtotal;
}
