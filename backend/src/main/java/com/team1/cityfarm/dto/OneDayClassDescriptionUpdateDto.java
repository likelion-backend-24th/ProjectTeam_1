package com.team1.cityfarm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class OneDayClassDescriptionUpdateDto {

    @NotBlank(message = "설명은 필수입니다.")
    @Size(max = 1000)
    private String description;

}
