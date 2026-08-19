package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.OrderType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OrderCreateRequestDto {

    private Long classId;

    private OrderType orderType;
}