package pe.test.exchange.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "conversiones")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversion {

    private static final int DECIMALES_MONTO = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(name = "moneda_origen", nullable = false, length = 3)
    private String monedaOrigen;

    @Column(name = "moneda_destino", nullable = false, length = 3)
    private String monedaDestino;

    @Column(name = "tipo_cambio", nullable = false, precision = 19, scale = 6)
    private BigDecimal tipoCambio;

    @Column(name = "monto_convertido", nullable = false, precision = 19, scale = 2)
    private BigDecimal montoConvertido;

    @Column(name = "fecha_operacion", nullable = false)
    private LocalDateTime fechaOperacion;

    private Conversion(BigDecimal monto, String monedaOrigen, String monedaDestino, BigDecimal tipoCambio) {
        this.monto = validarPositivo(monto, "monto");
        this.monedaOrigen = Objects.requireNonNull(monedaOrigen, "monedaOrigen");
        this.monedaDestino = Objects.requireNonNull(monedaDestino, "monedaDestino");
        this.tipoCambio = validarPositivo(tipoCambio, "tipoCambio");
        this.montoConvertido = monto.multiply(tipoCambio).setScale(DECIMALES_MONTO, RoundingMode.HALF_UP);
        this.fechaOperacion = LocalDateTime.now();
    }

    public static Conversion crear(BigDecimal monto, String monedaOrigen, String monedaDestino, BigDecimal tipoCambio) {
        return new Conversion(monto, monedaOrigen, monedaDestino, tipoCambio);
    }

    private static BigDecimal validarPositivo(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("El " + campo + " debe ser mayor a cero");
        }
        return valor;
    }
}
