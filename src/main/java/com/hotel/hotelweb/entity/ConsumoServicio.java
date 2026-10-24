package com.hotel.hotelweb.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ConsumoServicio {

    private int idConsumo;
    private int idReserva;
    private int idServicio;

    private String nombreServicio;

    private int cantidad;
    private BigDecimal precioUnitario;
    private LocalDateTime fechaConsumo;

    public ConsumoServicio() {
    }

    public int getIdConsumo() {
        return idConsumo;
    }

    public void setIdConsumo(int idConsumo) {
        this.idConsumo = idConsumo;
    }

    public int getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(int idReserva) {
        this.idReserva = idReserva;
    }

    public int getIdServicio() {
        return idServicio;
    }

    public void setIdServicio(int idServicio) {
        this.idServicio = idServicio;
    }

    public String getNombreServicio() {
        return nombreServicio;
    }

    public void setNombreServicio(String nombreServicio) {
        this.nombreServicio = nombreServicio;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public LocalDateTime getFechaConsumo() {
        return fechaConsumo;
    }

    public void setFechaConsumo(LocalDateTime fechaConsumo) {
        this.fechaConsumo = fechaConsumo;
    }

    // Subtotal calculado: cantidad por precio unitario.
    public BigDecimal getSubtotal() {

        if (precioUnitario == null) {
            return BigDecimal.ZERO;
        }

        return precioUnitario.multiply(
                BigDecimal.valueOf(cantidad)
        );
    }
}