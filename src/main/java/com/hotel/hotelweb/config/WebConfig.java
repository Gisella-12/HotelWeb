package com.hotel.hotelweb.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SesionInterceptor sesionInterceptor;

    public WebConfig(SesionInterceptor sesionInterceptor) {
        this.sesionInterceptor = sesionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(sesionInterceptor)
                .addPathPatterns(
                        "/panel",
                        "/habitaciones/**",
                        "/clientes/**",
                        "/reservas/**",
                        "/servicios/**",
                        "/pagos/**"
                );
    }
}