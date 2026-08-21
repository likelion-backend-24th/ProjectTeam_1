package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.SettlementResponseDto;
import com.team1.cityfarm.global.security.user.CustomUserDetails;
import com.team1.cityfarm.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "호스트 API", description = "호스트 전용 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/host")
@RequiredArgsConstructor
public class HostController {

    private final SettlementService settlementService;

    /**
     * [호스트] 내 정산내역 목록 조회
     */
    @Operation(summary = "내 정산 내역 조회",
            description = "로그인한 호스트의 정산 내역 리스트를 최신순으로 조회합니다.")
    @GetMapping("/settlements")
    public ResponseEntity<List<SettlementResponseDto>> getMySettlements(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long hostId = userDetails.getUserId();
        List<SettlementResponseDto> response = settlementService.getHostSettlements(hostId);
        return ResponseEntity.ok(response);
    }
}