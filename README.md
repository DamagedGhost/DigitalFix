# DigitalFix

Plataforma cloud native para la gestión de órdenes de mantención eléctrica de clientes empresariales. El sistema permite crear, consultar y administrar órdenes de trabajo, controlar el catálogo de servicios y repuestos, gestionar mensajería asíncrona desacoplada mediante un clúster RabbitMQ con colas Dead Letter (DLQ), y aplicar autorización por rol mediante Microsoft Entra ID.

Proyecto semestral del ramo **DSY1107: Desarrollo Cloud Native I**, DuocUC, Chile.

---

## Arquitectura del Sistema

El sistema implementa una arquitectura de microservicios orientada a eventos (*Event-Driven Architecture*) desacoplada con RabbitMQ:

```mermaid
flowchart TD
    U["Usuario empresarial"] --> FE["React 19 + MSAL + Vite<br/>Nginx / Cloudflare"]
    FE --> ID["Microsoft Entra ID<br/>OIDC + PKCE (S256)"]
    FE --> APIGW["AWS API Gateway<br/>HTTP API v1 + JWT Authorizer"]
    APIGW --> BFF["BFF (ms-digitalfix-bff)<br/>Spring Boot 4 + Resource Server"]
    
    BFF --> WO["ms-digitalfix-workorders<br/>Órdenes + Máquina de estados"]
    BFF --> CAT["ms-digitalfix-catalog<br/>Servicios + Repuestos"]
    BFF --> NOT["ms-digitalfix-notify<br/>Consumidores + Admin Colas"]
    
    WO --> DB[("PostgreSQL 15<br/>JPA + Flyway")]
    CAT --> DB
    
    WO -- "EventEnvelope (JSON)" --> EX_DIR["Exchange: cmd.direct<br/>RabbitMQ Clúster (2 Nodos)"]
    WO -- "EventEnvelope (JSON)" --> EX_TOP["Exchange: cmd.topic"]
    
    EX_DIR --> Q_EMAIL["q.cmd.email"]
    EX_DIR --> Q_DISP["q.cmd.dispatch"]
    EX_DIR --> Q_REP["q.cmd.report"]
    
    Q_EMAIL --> NOT
    Q_DISP --> NOT
    Q_REP --> NOT
    
    NOT -- "Error irrecuperable / basicNack(false)" --> EX_DEAD["Exchange: cmd.dead (DLX)"]
    EX_DEAD --> DLQ_EMAIL["q.cmd.email.dlq"]
    EX_DEAD --> DLQ_DISP["q.cmd.dispatch.dlq"]
    EX_DEAD --> DLQ_REP["q.cmd.report.dlq"]
```

### Componentes Clave:
1. **Frontend (`frontend-digitalfix`)**: SPA en React 19 y TypeScript con MSAL para autenticación Entra ID. Incluye el Dashboard operativo con KPIs en vivo, feed reactivo de eventos asíncronos y consola administrativa de RabbitMQ.
2. **BFF (`ms-digitalfix-bff`)**: Puerta de entrada para el frontend. Valida JWT, claims y roles (`Admin`, `Supervisor`, `Cliente`, `Auditor`), y expone proxies hacia los microservicios de dominio.
3. **Órdenes (`ms-digitalfix-workorders`)**: Gestión de ciclo de vida de órdenes. Emite eventos AMQP ante cambios de estado (`ASIGNADA`, `EN_DESPLAZAMIENTO`, `CERRADA`).
4. **Catálogo (`ms-digitalfix-catalog`)**: Administración de servicios técnicos y stock de repuestos.
5. **Notificaciones (`ms-digitalfix-notify`)**: Microservicio de mensajería asíncrona. Contiene 3 consumidores modulares, lógica de confirmación ACK/NACK con reintentos y deduplicación por `eventId`, gestión de Dead Letter Queues (DLQ), y API REST administrativa.
6. **Clúster RabbitMQ**: Clúster de 2 nodos (`rabbit1` y `rabbit2`) con replicación, peer discovery y cookie compartida, desplegado en Docker Compose.

---

## Topología de Mensajería RabbitMQ

Toda la infraestructura de mensajería está declarada de manera centralizada en `DigitalfixRabbitProperties` y configurada mediante beans explícitos en `RabbitTopologyConfig`:

### Exchanges
- `cmd.direct` (`DirectExchange`): Enrutamiento directo para comandos y eventos de órdenes.
- `cmd.topic` (`TopicExchange`): Enrutamiento por patrones jerárquicos de eventos.
- `cmd.dead` (`DirectExchange`): Dead Letter Exchange (DLX) para desvío de mensajes no procesables.

### Colas y Dead Letter Queues (DLQ)
Cada cola principal está configurada con los argumentos `x-dead-letter-exchange: cmd.dead` y su respectiva `x-dead-letter-routing-key`:

| Flujo | Cola Principal | Routing Key Direct | Dead Letter Queue (DLQ) | Routing Key DLQ |
|---|---|---|---|---|
| **Email** | `q.cmd.email` | `email.send` | `q.cmd.email.dlq` | `email.dead` |
| **Despacho** | `q.cmd.dispatch` | `dispatch.ticket` | `q.cmd.dispatch.dlq` | `dispatch.dead` |
| **Reporte** | `q.cmd.report` | `report.gen` | `q.cmd.report.dlq` | `report.dead` |

---

## Manejo de Confirmaciones (ACK/NACK) y Resiliencia

El componente `ConsumerAckSupport` implementa una máquina de decisiones robusta para garantizar que **ningún mensaje se pierda**:

1. **Idempotencia / Deduplicación**: Almacena en memoria los `eventId` procesados. Si un mensaje se redelivery, se confirma (`basicAck`) y se descarta de forma segura.
2. **Procesamiento Exitoso**: Se invoca el servicio de negocio correspondiente y se emite `basicAck(deliveryTag, false)`.
3. **Error Temporal (`RecoverableMessageException`)**: Se reintenta hasta `max-attempts` veces (por defecto 3) aplicando backoff configurable (por defecto 1000 ms). Si los reintentos se agotan, se ejecuta `basicNack(requeue=false)` enviando el mensaje a la DLQ.
4. **Error Irrecuperable (`UnrecoverableMessageException`) o Inesperado**: Errores semánticos (ej. orden sin técnico asignado) no se reintentan; se ejecuta de inmediato `basicNack(deliveryTag, false, false)` sin requeue. RabbitMQ traslada el mensaje a la cola `.dlq` de forma automática.
5. **Registro de Auditoría**: Todo desvío a DLQ se registra en `NotificationHistoryService` con categoría `"DLQ"` para reflejarse en tiempo real en la interfaz.

---

## API Administrativa de Colas (`ms-digitalfix-notify`)

Disponible bajo `/api/v1/admin/queues` protegida por rol `Admin` en el BFF:

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/v1/admin/queues` | Lista las 6 colas del sistema con conteo de mensajes y consumidores |
| `GET` | `/api/v1/admin/queues/{name}` | Consulta métricas de una cola específica |
| `POST` | `/api/v1/admin/queues` | Aprovisiona una nueva cola durable y la vincula a un exchange |
| `DELETE` | `/api/v1/admin/queues/{name}` | Elimina una cola personalizada (las colas del sistema están protegidas) |
| `DELETE` | `/api/v1/admin/queues/{flow}/purge?dlq={true\|false}` | Vacía los mensajes de la cola principal o DLQ |
| `POST` | `/api/v1/admin/queues/{flow}/reprocess?max=100` | Extrae mensajes de la DLQ y los reinyecta a la cola principal |
| `POST` | `/api/v1/admin/queues/simulate-dlq` | Inyecta un mensaje intencionalmente inválido para demostrar el desvío a DLQ |

---

## Estructura del Repositorio

```text
DigitalFix/
├── frontend-digitalfix/       # Aplicación React 19, TypeScript, MSAL y Nginx
│   └── src/components/
│       ├── Dashboard.tsx      # Dashboard con feed reactivo y alertas DLQ
│       └── AdminRabbitPanel.tsx # Consola administrativa y botón de fallo DLQ
├── ms-digitalfix-bff/         # BFF Spring Boot 4, Resource Server y Proxies
│   └── src/.../controller/
│       ├── AdminQueueProxyController.java # Proxy para gestión de colas
│       └── NotificationProxyController.java # Proxy para feed de eventos
├── ms-digitalfix-workorders/  # Microservicio de Órdenes y productor AMQP
│   └── src/.../messaging/
│       └── WorkOrderEventProducer.java # Emisor de EventEnvelope ante cambios de estado
├── ms-digitalfix-catalog/     # Servicios técnicos y control de repuestos
├── ms-digitalfix-notify/      # Microservicio de Notificaciones y RabbitMQ
│   └── src/.../
│       ├── config/            # Topología RabbitMQ, Clúster, Exchanges y DLQ
│       ├── consumer/          # Consumidores modulares (email, dispatch, report, support)
│       ├── admin/             # API y Servicio de administración de colas
│       └── notification/      # Servicio de historial y feed de eventos
├── infra/                     # Configuración de infraestructura cloud
└── docker-compose.yml         # Orquestación con PostgreSQL, RabbitMQ Clúster y Microservicios
```

---

## Despliegue en Producción (AWS EC2)

```bash
cd DigitalFix
git pull

# Reconstruir los servicios actualizados
docker compose build notify frontend bff
docker compose up -d notify frontend bff

# Verificar estado de todos los contenedores
docker compose ps
```

---

## Equipo de Desarrollo

- **Diego Parra:** Frontend React, autenticación MSAL y componentes de interfaz.
- **Joaquín:** BFF, seguridad OAuth2 y microservicio de órdenes.
- **Felipe Vásquez (DamagedGhost):** Infraestructura AWS, clúster RabbitMQ, microservicio de notificaciones, DLQs y automatización de despliegue.

Repositorio oficial: [DamagedGhost/DigitalFix](https://github.com/DamagedGhost/DigitalFix)
