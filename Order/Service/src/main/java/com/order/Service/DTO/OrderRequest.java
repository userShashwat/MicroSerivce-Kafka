package com.order.Service.DTO;

import lombok.Data;

@Data
public class OrderRequest {
    private Long userId;
    private String product;
    private Integer quantity;
    private Double price;
}
