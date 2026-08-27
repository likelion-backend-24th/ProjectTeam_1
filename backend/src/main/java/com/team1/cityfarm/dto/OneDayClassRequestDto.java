package com.team1.cityfarm.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class OneDayClassRequestDto {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100)
    private String title;

    @NotBlank(message = "설명은 필수입니다.")
    @Size(max = 1000)
    private String description;

    @NotNull(message = "클래스 날짜는 필수입니다.")
    @Future(message = "클래스 날짜는 현재 이후여야 합니다.")
    private LocalDateTime date;

    @NotBlank(message = "장소는 필수입니다.")
    @Size(max = 100)
    private String location;

    @Positive(message = "정원은 1명 이상이어야 합니다.")
    private int capacity;

    @Positive(message = "가격은 0원보다 커야합니다.")
    private int price;

}
