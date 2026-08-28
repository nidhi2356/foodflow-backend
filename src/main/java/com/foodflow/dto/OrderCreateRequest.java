package com.foodflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for creating a new order")
public class OrderCreateRequest {

    @NotNull(message = "Restaurant ID is required")
    @Schema(description = "Database ID of the restaurant from which food is being ordered", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long restaurantId;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    @Schema(description = "List of items to order from the specified restaurant", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<OrderItemRequest> items;
}
