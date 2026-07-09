# Configuración RabbitMQ

Sistema de colas de la Semana 8. RabbitMQ corre como **contenedor Docker** junto a la aplicación (mismo `docker-compose.yml`), tanto en local como en la EC2.

## 1. Topología

```
                        guias.exchange (direct)
                       /                       \
        routing key: guias.nueva      routing key: guias.error
                     |                             |
                guias.cola                   guias.errores
                 (cola 1)                      (cola 2)
```

Definida por código en `RabbitConfig` (se crea automáticamente al iniciar la app):

| Elemento | Nombre | Uso |
|---|---|---|
| Exchange | `guias.exchange` | Direct exchange durable |
| Cola 1 | `guias.cola` | Recibe cada guía creada/actualizada |
| Cola 2 | `guias.errores` | Almacena los mensajes con errores |

## 2. Flujo de mensajes

1. **Productor** (`GuiaColaProducer`, Java): al crear o actualizar una guía (`POST /guias`, `PUT /guias/{id}`, `POST /guias/{id}/archivo`) se publica un `GuiaMensaje` (JSON) en la cola 1.
2. **Fallback a cola de errores**: si la publicación en la cola 1 falla, el mensaje se envía a `guias.errores` envuelto en un `GuiaErrorMensaje` con el detalle del error y timestamp. Lo mismo ocurre si falla la persistencia durante el consumo.
3. **Consumidor** (`GuiaColaConsumer`, Java): el endpoint `POST /guias/procesar-cola` vacía la cola 1 y guarda cada mensaje en Oracle Cloud (tabla `GUIAS_PROCESADAS`).

## 3. Despliegue del contenedor

Ya incluido en `docker-compose.yml`:

```yaml
rabbitmq:
  image: rabbitmq:3-management
  ports:
    - "5672:5672"    # AMQP (aplicación)
    - "15672:15672"  # Consola web de administración
  environment:
    RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER}
    RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD}
```

Las credenciales las defines tú en el `.env` (local) o en los GitHub Secrets `RABBITMQ_USER` / `RABBITMQ_PASSWORD` (deploy).

## 4. Consola de administración

- Local: `http://localhost:15672`
- EC2: `http://<IP-PUBLICA-EC2>:15672` (requiere abrir el puerto en el security group)

Desde **Queues** puedes ver `guias.cola` y `guias.errores`, la cantidad de mensajes pendientes y su contenido (Get messages), útil como evidencia.

## 5. Prueba end-to-end

1. `POST /guias` (con token GUIA_OPERADOR) → en la consola, `guias.cola` muestra 1 mensaje.
2. `POST /guias/procesar-cola` → responde `{"procesadas": 1, ...}` y la cola queda en 0; el registro aparece en `GUIAS_PROCESADAS` en Oracle.
3. Para evidenciar la cola de errores: detén Oracle (o usa credenciales inválidas) y ejecuta `procesar-cola`; el mensaje aparece en `guias.errores`.
