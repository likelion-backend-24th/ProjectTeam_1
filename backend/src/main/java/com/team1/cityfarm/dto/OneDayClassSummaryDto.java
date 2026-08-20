package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.OneDayClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class OneDayClassSummaryDto {

    private Long id;
    private String title;
    private LocalDateTime date;
    private String location;
    private int capacity;
    private int price;

    public static OneDayClassSummaryDto from (OneDayClass entity){
        return OneDayClassSummaryDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .date(entity.getDate())
                .location(entity.getLocation())
                .capacity(entity.getCapacity())
                .price(entity.getPrice())
                .build();
    }
}
