package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.UserRequestDto;
import com.team1.cityfarm.dto.UserResponseDto;
import com.team1.cityfarm.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    //전체 유저 조회
    @GetMapping("/users")
    public ResponseEntity<Page<UserResponseDto>> getAllUsers(Pageable pageable){
        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }

    //유저 상태 수정
    @PatchMapping("/users/{id}")
    public ResponseEntity<UserResponseDto> patchUser(@PathVariable Long id,
                                                     @RequestBody UserRequestDto dto){

        return ResponseEntity.ok(adminService.modifyUser(id,dto));
    }
}
