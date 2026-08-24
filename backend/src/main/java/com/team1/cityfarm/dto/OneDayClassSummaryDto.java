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
    private String description;
    private LocalDateTime date;
    private String location;
    private int capacity;
    private long enrolledCount;
    private int price;

    public static OneDayClassSummaryDto from(OneDayClass entity, long enrolledCount){
        return OneDayClassSummaryDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .date(entity.getDate())
                .location(entity.getLocation())
                .capacity(entity.getCapacity())
                .enrolledCount(enrolledCount)
                .price(entity.getPrice())
                .build();
    }
}
