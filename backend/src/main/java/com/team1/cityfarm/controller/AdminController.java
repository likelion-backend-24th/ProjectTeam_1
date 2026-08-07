package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.UserRequestDto;
import com.team1.cityfarm.dto.UserResponseDto;
import com.team1.cityfarm.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자 API", description = "관리자 전용 API")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    //전체 유저 조회
    @Operation(summary = "전체 유저 목록 조회",
            description = "관리자가 페이징을 통해 전체 유저 목록을 조회합니다.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/users")
    public ResponseEntity<Page<UserResponseDto>> getAllUsers(Pageable pageable){
        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }

    //유저 상태 수정
    @Operation(summary = "유저 정보 및 상태 수정",
            description = "관리자가 특정 유저의 정보나 권한/상태를 수정합니다.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PatchMapping("/users/{id}")
    public ResponseEntity<UserResponseDto> patchUser(@PathVariable Long id,
                                                     @RequestBody UserRequestDto dto){

        return ResponseEntity.ok(adminService.modifyUser(id,dto));
    }
}
