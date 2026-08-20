package com.team1.cityfarm.dto;

import com.team1.cityfarm.entity.OneDayClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class OneDayClassResponseDto {

    private Long id;
    private Long hostId;
    private String hostNickname;
    private String title;
    private String description;
    private LocalDateTime date;
    private String location;
    private int capacity;
    private int price;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OneDayClassResponseDto from(OneDayClass entity){
        return OneDayClassResponseDto.builder()
                .id(entity.getId())
                .hostId(entity.getHost().getId())
                .hostNickname(entity.getHost().getNickname())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .date(entity.getDate())
                .location(entity.getLocation())
                .capacity(entity.getCapacity())
                .price(entity.getPrice())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
