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

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "conversiones")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Los Value Objects se guardan como columnas simples con los converters de infrastructure/persistence
    @Column(nullable = false, precision = 19, scale = Monto.DECIMALES)
    private Monto monto;

    @Column(name = "moneda_origen", nullable = false, length = 3)
    private Moneda monedaOrigen;

    @Column(name = "moneda_destino", nullable = false, length = 3)
    private Moneda monedaDestino;

    @Column(name = "tipo_cambio", nullable = false, precision = 19, scale = TipoCambio.DECIMALES)
    private TipoCambio tipoCambio;

    // 28 dígitos: los 17 enteros que admite el monto más los 9 del tipo de cambio, para que nunca desborde
    @Column(name = "monto_convertido", nullable = false, precision = 28, scale = Monto.DECIMALES)
    private Monto montoConvertido;

    @Column(name = "fecha_operacion", nullable = false)
    private LocalDateTime fechaOperacion;

    private Conversion(Monto monto, Moneda monedaOrigen, Moneda monedaDestino, TipoCambio tipoCambio) {
        this.monto = validarPositivo(monto);
        this.monedaOrigen = Objects.requireNonNull(monedaOrigen, "monedaOrigen");
        this.monedaDestino = Objects.requireNonNull(monedaDestino, "monedaDestino");
        this.tipoCambio = Objects.requireNonNull(tipoCambio, "tipoCambio");
        this.montoConvertido = monto.convertir(tipoCambio);
        this.fechaOperacion = LocalDateTime.now();
    }

    public static Conversion crear(Monto monto, Moneda monedaOrigen, Moneda monedaDestino, TipoCambio tipoCambio) {
        return new Conversion(monto, monedaOrigen, monedaDestino, tipoCambio);
    }

    // Monto ya garantiza que no es negativo; convertir cero no tiene sentido
    private static Monto validarPositivo(Monto monto) {
        if (monto == null || monto.esCero()) {
            throw new IllegalArgumentException("El monto a convertir debe ser mayor a cero");
        }
        return monto;
    }
}
