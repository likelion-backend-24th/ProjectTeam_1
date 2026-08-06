package com.team1.cityfarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "로그인 응답 DTO")
public class LoginResponseDto {

    @Schema(description = "accessToken", example = "MAFCPtab2K7eLhW5oOXPnCQOqzeD1rs9b1O9mRDAcRA")
    private String accessToken;

    @Schema(description = "refreshToken", example = "39Mi/4xYqJuoNUvRVd+Nm8+SnjscMCOguulSpwmLJgA=")
    private String refreshToken;
}