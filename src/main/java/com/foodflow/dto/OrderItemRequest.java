package com.foodflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Individual item in an order creation request")
public class OrderItemRequest {

    @NotNull(message = "Food ID is required")
    @Schema(description = "Database ID of the food item", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long foodId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Quantity of the food item (must be at least 1)", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;
}
