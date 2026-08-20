package com.team1.cityfarm.controller;

import com.team1.cityfarm.dto.OrderCreateRequestDto;
import com.team1.cityfarm.dto.OrderResponseDto;
import com.team1.cityfarm.global.security.user.CustomUserDetails;
import com.team1.cityfarm.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody OrderCreateRequestDto requestDto
    ) {
        OrderResponseDto response = orderService.createClassOrder(userDetails.getUserId(), requestDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getOrderDetails(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId
    ) {
        OrderResponseDto response = orderService.getOrderDetails(userDetails.getUserId(), orderId);
        return ResponseEntity.ok(response);
    }

//    @GetMapping("/me")
//    public ResponseEntity<Page<OrderResponseDto>> getMyOrders(
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
//    ) {
//        Page<OrderResponseDto> response = orderService.getMyOrders(userDetails.getUserId(), pageable);
//        return ResponseEntity.ok(response);
//    }
}