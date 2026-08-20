package com.team1.cityfarm.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 리소스를 URL 접근 가능하도록 등록하는 메서드
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry){
        // uploads/ 폴더에서 파일을 찾아서 응답
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
