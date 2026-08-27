package com.team1.cityfarm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class HostPromotionRequestDto {

    @NotBlank(message = "사업자명은 필수입니다.")
    @Size(max = 50)
    private String businessName;

    @NotBlank(message = "사업자등록번호는 필수입니다.")
    @Pattern(regexp = "\\d{3}-\\d{2}-\\d{5}",
            message = "사업자등록번호 형식이 올바르지 않습니다. (예 : 000-00-00000)")
    private String businessRegistrationNumber;

    @NotBlank(message = "사업장 주소는 필수입니다.")
    @Size(max = 200)
    private String businessAddress;
}
