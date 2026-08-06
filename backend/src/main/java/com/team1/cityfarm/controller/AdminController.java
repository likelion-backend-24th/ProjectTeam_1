package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.UserRequestDto;
import com.team1.cityfarm.dto.UserResponseDto;
import com.team1.cityfarm.entity.User;
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

    @GetMapping("/users")
    public ResponseEntity<Page<UserResponseDto>> getAllUsers(Pageable pageable){
        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }

    @PatchMapping("/users/{id}")
    public ResponseEntity<UserResponseDto> patchUser(@PathVariable Long id,
                                                     @RequestBody UserRequestDto dto){

        return ResponseEntity.ok(null);
    }
}
