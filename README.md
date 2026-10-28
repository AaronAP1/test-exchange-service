# Desafío Técnico – API de Tipo de Cambio

## Descripción

Microservicio en Java para consumir un servicio externo de tipo de cambio y exponer una API REST que permita convertir montos entre distintas monedas.

---

## Stack Tecnológico

| Tecnología | Versión |
|---|---|
| Java | 17 |
| Spring Boot | 4.0.6 |
| Spring Cloud OpenFeign | 2025.1.1 |
| Resilience4j (Retry + Circuit Breaker) | 2.3.0 |
| H2 (base de datos en memoria) | Runtime |
| Spring Data JPA | Incluido |
| Lombok | Incluido |
| Maven | Wrapper incluido |

---

## Lo que ya está configurado

### Cliente HTTP externo (OpenFeign)

El cliente Feign ya está implementado y apuntando a la API pública de tipo de cambio:

- **Interfaz:** `infrastructure/external/ExchangeRateApiClient.java`
- **DTO de respuesta:** `infrastructure/external/dto/ExchangeRateApiResponse.java`
- **API usada:** `https://v6.exchangerate-api.com/v6`
- **Endpoint:** `GET /{apiKey}/latest/{baseCurrency}`

La clase principal ya tiene habilitado `@EnableFeignClients`.

### Base de datos H2

Datasource, JPA y consola H2 listos en `application.yaml`:

| Parámetro | Valor |
|---|---|
| JDBC URL | `jdbc:h2:mem:testdb` |
| Usuario | `sa` |
| Password | `password` |
| Consola | `http://localhost:8080/h2-console` |
| DDL | `update` (auto-creación de tablas) |

### Estructura de paquetes sugerida

```
pe.test.exchange
├── domain
│   ├── model           ← Entidades JPA
│   ├── repository      ← Interfaces de repositorio
│   ├── service         ← Contratos de dominio
│   └── exception       ← Excepciones de dominio
├── application
│   ├── controller      ← Endpoints REST
│   ├── service         ← Lógica de aplicación
│   └── dto             ← Request / Response
└── infrastructure
    ├── external        ← Cliente Feign (YA IMPLEMENTADO)
    └── persistence     ← Implementaciones de repositorio
```

---

## Lo que debes implementar

### 1. Endpoint de conversión

Exponer `POST /api/conversion` con el siguiente contrato:

**Request:**
```json
{
  "monto": 100,
  "monedaOrigen": "USD",
  "monedaDestino": "PEN"
}
```

**Response:**
```json
{
  "monto": 100,
  "montoConvertido": 370.00,
  "monedaOrigen": "USD",
  "monedaDestino": "PEN",
  "tipoCambio": 3.70
}
```

### 2. Integración con el cliente Feign existente

Usar `ExchangeRateApiClient` para obtener el tipo de cambio real desde:

```
https://v6.exchangerate-api.com/v6/{apiKey}/latest/{baseCurrency}
```

La API key se configura en `application.yaml` bajo `exchange-rate.api.key`, que la lee de la variable `EXCHANGE_API_KEY` (ver [Configuración de la API key](#configuración-de-la-api-key)).

### 3. Persistencia

Almacenar cada conversión en H2 con los siguientes datos:

- Monto original
- Moneda origen / destino
- Tipo de cambio aplicado
- Monto convertido
- Fecha de la operación

---

## Consideraciones de Diseño

- Aplicar **Domain-Driven Design (DDD)** con separación clara de capas
- Manejo adecuado de errores (moneda inválida, servicio externo no disponible)
- Código limpio y mantenible

---

## Solución implementada

### Estructura

```
pe.test.exchange
├── domain
│   ├── model           Conversion: entidad JPA con la regla de cálculo
│   ├── repository      ConversionRepository: contrato de persistencia
│   ├── service         ExchangeRateProvider: contrato para obtener el tipo de cambio
│   └── exception       MonedaInvalidaException, ServicioTipoCambioNoDisponibleException
├── application
│   ├── controller      ConversionController, GlobalExceptionHandler
│   ├── service         ConversionService: caso de uso de conversión
│   └── dto             ConversionRequest, ConversionResponse, ErrorResponse
└── infrastructure
    ├── external        ExchangeRateApiClient (Feign) y ExchangeRateApiAdapter
    └── persistence     ConversionJpaRepository y ConversionRepositoryImpl
```

### Flujo

1. `ConversionController` recibe el `POST /api/conversion` y valida el request.
2. `ConversionService` normaliza las monedas y pide el tipo de cambio a `ExchangeRateProvider`.
3. `ExchangeRateApiAdapter` consulta ExchangeRate-API con el cliente Feign.
4. `Conversion.crear(...)` calcula el monto convertido.
5. `ConversionRepository` guarda la conversión en H2.
6. Se responde con un `ConversionResponse`.

### Decisiones de diseño

- **El dominio no depende de Feign ni de Spring Data.** Solo define los contratos `ExchangeRateProvider` y `ConversionRepository`, y la infraestructura los implementa.
- **La regla de negocio vive en el modelo.** `Conversion.crear(...)` calcula `monto × tipoCambio` con `BigDecimal`, redondea a 2 decimales con `HALF_UP` y valida que el monto y el tipo de cambio sean positivos.
- **Moneda no soportada.** ExchangeRate-API responde `404` con `"error-type": "unsupported-code"` en el cuerpo. El cliente Feign usa `dismiss404` para decodificar ese cuerpo y el adaptador lo convierte en `MonedaInvalidaException`. Si la moneda destino no aparece entre las tasas, también se trata como moneda inválida.
- **Servicio externo no disponible.** Las fallas transitorias se reintentan y un circuit breaker deja de llamar a la API cuando falla de forma repetida (ver [Resiliencia](#resiliencia)). Si al final no se obtiene el tipo de cambio, se lanza `ServicioTipoCambioNoDisponibleException` y se responde 503.
- **La API key no se expone.** No se versiona, porque vive en `.env` y git lo ignora. Tampoco se escribe en los logs: el mensaje de `FeignException` incluye la URL con la key, así que solo se registra el status.
- **El caso de uso no lleva `@Transactional`.** Así no se retiene una conexión a la base de datos mientras se espera a la API externa. El `save` del repositorio ya es transaccional.

---

## Resiliencia

`ExchangeRateApiAdapter.obtenerTipoCambio` lleva `@Retry` y `@CircuitBreaker` de Resilience4j, ambos con la instancia `exchangeRateApi`, configurada en `application.yaml`. Retry envuelve al Circuit Breaker, así que cada intento cuenta para el circuito y, si el circuito está abierto, no se reintenta.

Las `FeignException` salen del método sin traducir para que Resilience4j decida qué hacer con ellas. Después, el fallback `servicioNoDisponible` las convierte en `ServicioTipoCambioNoDisponibleException`, sin registrar el mensaje original porque contiene la URL con la key.

### Retry

| Parámetro | Valor |
|---|---|
| Intentos máximos | 3 (1 + 2 reintentos) |
| Espera entre intentos | 500 ms, con backoff exponencial ×2 (500 ms, 1 s) |
| Se reintenta | Timeouts y errores de conexión (`RetryableException`) y respuestas 5xx (`FeignServerException`) |
| No se reintenta | Moneda inválida, key inválida (403), cuota agotada (429) ni circuito abierto, porque reintentar no cambiaría el resultado |

Los timeouts de Feign son de 2 s para conectar y 3 s para leer, así que el peor caso de una llamada ronda los 10 s.

### Circuit Breaker

| Parámetro | Valor |
|---|---|
| Ventana | Últimas 10 llamadas, con un mínimo de 5 para evaluar |
| Umbral de apertura | 50 % de fallos |
| Tiempo abierto | 30 s; luego pasa solo a semiabierto |
| Llamadas de prueba en semiabierto | 2 |
| Cuenta como fallo | Cualquier `FeignException`. Una moneda inválida no cuenta, porque la API respondió bien. |

Mientras el circuito está abierto, el servicio responde 503 de inmediato, sin llamar a la API. Así no se acumulan hilos esperando timeouts y no se gasta la cuota del plan gratuito.

Comportamiento medido con la API caída (puerto cerrado):

| Llamada | Qué pasa | Tiempo |
|---|---|---|
| 1 | 3 intentos con backoff → 503 | ~1.9 s |
| 2 | Al 5.º fallo se abre el circuito y el 3.er intento se rechaza → 503 | ~1.5 s |
| 3 en adelante | Circuito abierto → 503 sin llamar a la API | ~5 ms |

---

## Configuración de la API key

1. Obtén una key gratuita en [exchangerate-api.com](https://www.exchangerate-api.com). El plan Free incluye 1,500 consultas al mes.
2. Crea un archivo `.env` en la raíz del proyecto:

   ```properties
   EXCHANGE_API_KEY=tu_api_key
   ```

La aplicación carga ese archivo con `spring.config.import: optional:file:.env[.properties]`. También puedes definir `EXCHANGE_API_KEY` como variable de entorno del sistema, que tiene prioridad sobre el archivo.

Los tests no necesitan la key porque simulan la API externa.

---

## Cómo ejecutar

```bash
./mvnw spring-boot:run
```

En Windows usa `mvnw.cmd spring-boot:run`.

La aplicación levanta en `http://localhost:8080`. La consola H2 está en `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:testdb`, usuario `sa`, password `password`) y las conversiones se guardan en la tabla `CONVERSIONES`.

### Ejemplo

```bash
curl -X POST http://localhost:8080/api/conversion \
  -H "Content-Type: application/json" \
  -d '{"monto": 100, "monedaOrigen": "USD", "monedaDestino": "PEN"}'
```

```json
{
  "monto": 100,
  "montoConvertido": 344.86,
  "monedaOrigen": "USD",
  "monedaDestino": "PEN",
  "tipoCambio": 3.4486
}
```

Los códigos de moneda también se aceptan en minúsculas: `usd` se normaliza a `USD`.

---

## Manejo de errores

Todas las respuestas de error tienen el mismo formato:

```json
{
  "timestamp": "2026-10-27T20:28:19.0852707",
  "status": 400,
  "mensaje": "La solicitud contiene datos inválidos",
  "detalles": [
    "monedaDestino: es obligatoria",
    "monedaOrigen: debe ser un código de moneda de 3 letras (ej. USD)",
    "monto: debe ser mayor a cero"
  ]
}
```

| Caso | HTTP | Mensaje |
|---|---|---|
| Datos inválidos: monto ≤ 0, más de 2 decimales, o moneda vacía o que no tiene 3 letras | 400 | `La solicitud contiene datos inválidos`, con `detalles` |
| JSON mal formado | 400 | `El cuerpo de la solicitud no es un JSON válido` |
| Moneda no soportada (por ejemplo `XYZ`) | 400 | `La moneda 'XYZ' no es válida o no está soportada` |
| API externa caída, timeout, key inválida o cuota agotada | 503 | `El servicio de tipo de cambio no está disponible, intente nuevamente más tarde` |
| Error inesperado | 500 | `Ocurrió un error inesperado` |

---

## Tests

```bash
./mvnw test
```

| Clase | Qué cubre |
|---|---|
| `ConversionTest` | Cálculo, redondeo `HALF_UP`, fecha de la operación e invariantes del dominio |
| `ConversionServiceTest` | Orquestación del caso de uso, normalización y que no se guarde nada ante un error |
| `ExchangeRateApiAdapterTest` | Conversión de respuestas de la API en errores de dominio |
| `ExchangeRateApiAdapterIntegrationTest` | Cliente Feign real y Resilience4j contra un servidor HTTP local que imita la API: 404 `unsupported-code` sin reintento, 403 sin reintento, reintentos ante 5xx y timeouts, recuperación en el segundo intento, apertura del circuito y que una moneda inválida no cuente como fallo |
| `ConversionRepositoryImplTest` | Persistencia en H2 con `@DataJpaTest` |
| `ConversionControllerTest` | Contrato JSON y respuestas 200, 400, 405 y 503 |
| `ConversionIntegrationTest` | Flujo completo, del endpoint a H2, con la API externa simulada |
