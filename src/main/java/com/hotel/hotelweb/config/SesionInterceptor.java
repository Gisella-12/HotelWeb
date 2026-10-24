package com.hotel.hotelweb.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SesionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        HttpSession session = request.getSession(false);

        // Si no existe sesión o no hay usuario logueado
        if (session == null ||
                session.getAttribute("usuarioLogueado") == null) {

            response.sendRedirect("/login");
            return false;
        }

        // Si existe sesión, permite continuar
        return true;
    }
}