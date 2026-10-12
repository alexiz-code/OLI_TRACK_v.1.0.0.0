package com.example.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Línea de producto enviada al crear un pedido. El precio unitario y el
 * subtotal se recalculan siempre en el backend a partir del catálogo real,
 * nunca se confía en lo que mande el cliente.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetallePedidoDTO {

    private String id;

    @NotBlank(message = "El productoId es obligatorio")
    private String productoId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    /** Solo informativo en la respuesta; el backend lo calcula, no lo lee del request. */
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}
