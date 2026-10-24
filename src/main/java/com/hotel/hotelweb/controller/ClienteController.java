package com.hotel.hotelweb.controller;

import com.hotel.hotelweb.dao.ClienteDAO;
import com.hotel.hotelweb.entity.Cliente;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ClienteController {

    private final ClienteDAO clienteDAO;

    public ClienteController(ClienteDAO clienteDAO) {
        this.clienteDAO = clienteDAO;
    }

    // LISTAR CLIENTES
    @GetMapping("/clientes")
    public String listarClientes(Model model) {

        model.addAttribute(
                "clientes",
                clienteDAO.listarClientes()
        );

        return "clientes";
    }

    // FORMULARIO NUEVO CLIENTE
    @GetMapping("/clientes/nuevo")
    public String nuevoCliente(Model model) {

        model.addAttribute(
                "cliente",
                new Cliente()
        );

        return "formCliente";
    }

    // FORMULARIO EDITAR CLIENTE
    @GetMapping("/clientes/editar/{id}")
    public String editarCliente(
            @PathVariable int id,
            Model model) {

        Cliente cliente =
                clienteDAO.buscarCliente(id);

        model.addAttribute(
                "cliente",
                cliente
        );

        return "formCliente";
    }

    // GUARDAR O ACTUALIZAR CLIENTE
    @PostMapping("/clientes/guardar")
    public String guardarCliente(
            @ModelAttribute Cliente cliente) {

        if (cliente.getIdCliente() == 0) {

            clienteDAO.guardarCliente(cliente);

        } else {

            clienteDAO.actualizarCliente(cliente);
        }

        return "redirect:/clientes";
    }

    // ELIMINAR CLIENTE
    @GetMapping("/clientes/eliminar/{id}")
    public String eliminarCliente(
            @PathVariable int id) {

        clienteDAO.eliminarCliente(id);

        return "redirect:/clientes";
    }
}