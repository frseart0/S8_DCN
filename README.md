# Sistema de Gestión de Pedidos y Guías de Despacho

Microservicio Cloud Native (Spring Boot 3 / Java 21) para una empresa transportista. Cubre las actividades sumativas de las **Semanas 3, 6 y 8** de Desarrollo Cloud Native (CDY2204).

## Arquitectura

```
Postman ──> AWS API Gateway (authorizer JWT) ──> Spring Boot en EC2 (Docker)
                                                   │
             Microsoft Entra ID (JWT + roles) ─────┤ (validación Spring Security)
                                                   │
                    ┌──────────────┬───────────────┼──────────────────┐
                    ▼              ▼               ▼                  ▼
                  EFS            AWS S3        RabbitMQ           Oracle Cloud
              (temporal)   (fecha/transp.)   cola 1: guias.cola   GUIAS_DESPACHO
                                             cola 2: guias.errores GUIAS_PROCESADAS
```

- **Semana 3**: CRUD de guías, almacenamiento temporal en EFS, subida a S3 organizada `/{yyyyMMdd}/{transportista}/{archivo}`, CI/CD con GitHub Actions (Docker Hub + EC2).
- **Semana 6**: todos los endpoints registrados y securitizados en API Gateway; autenticación con Microsoft Entra ID (en lugar de Azure AD B2C) y 2 roles: `GUIA_LECTOR` (solo descargar) y `GUIA_OPERADOR` (el resto).
- **Semana 8**: 2 colas RabbitMQ en Docker (cola de guías + cola de errores), productor/consumidor en Java, y endpoint que consume la cola 1 y persiste en Oracle Cloud (tabla `GUIAS_PROCESADAS`).

## Endpoints

| Método | Ruta | Rol requerido |
|---|---|---|
| POST | `/guias` | GUIA_OPERADOR |
| POST | `/guias/{id}/archivo` (subir a S3) | GUIA_OPERADOR |
| GET | `/guias/{id}/descargar` | GUIA_LECTOR |
| PUT | `/guias/{id}` | GUIA_OPERADOR |
| DELETE | `/guias/{id}` | GUIA_OPERADOR |
| GET | `/guias?transportista=&fecha=` | GUIA_OPERADOR |
| GET | `/guias/{id}` | GUIA_OPERADOR |
| POST | `/guias/procesar-cola` (cola 1 → Oracle) | GUIA_OPERADOR |
| * | `/s3/**` (utilitarios S3) | GUIA_OPERADOR |

Todos requieren `Authorization: Bearer <token de Entra ID>`.

## Documentación de configuración cloud

| Documento | Contenido |
|---|---|
| [docs/01-aws-s3-efs-ec2.md](docs/01-aws-s3-efs-ec2.md) | S3, EFS (montaje) y EC2 con Docker |
| [docs/02-aws-api-gateway.md](docs/02-aws-api-gateway.md) | Registro de rutas y authorizer JWT |
| [docs/03-microsoft-entra-id.md](docs/03-microsoft-entra-id.md) | App registration, App Roles, token en Postman |
| [docs/04-oracle-cloud.md](docs/04-oracle-cloud.md) | Autonomous DB, wallet, usuario y tablas |
| [docs/05-rabbitmq.md](docs/05-rabbitmq.md) | Colas, exchange y despliegue en Docker |
| [docs/06-github-secrets.md](docs/06-github-secrets.md) | Todos los secretos de GitHub Actions y dónde encontrarlos |

## Ejecución local

Requisitos: Docker + Docker Compose.

```bash
# 1. Configura las variables (mínimo AWS y Entra para probar completo)
cp .env.example .env

# 2. Levanta RabbitMQ + aplicación
docker compose up --build
```

- App: `http://localhost:8080` (perfil `default` usa H2 en memoria; usa `SPRING_PROFILES_ACTIVE=prod` para Oracle).
- Consola RabbitMQ: `http://localhost:15672`.

Sin Docker: `./mvnw spring-boot:run` (necesita un RabbitMQ accesible en `localhost:5672`).

## CI/CD

Push a `main` dispara [.github/workflows/deploy.yml](.github/workflows/deploy.yml):

1. Build Maven + tests.
2. Build de imagen Docker y push a Docker Hub (`latest` + SHA).
3. Copia `docker-compose.yml` a la EC2 por SCP, genera el `.env` desde los GitHub Secrets, restaura el wallet de Oracle y ejecuta `docker compose up -d`.

Los secretos necesarios están detallados en [docs/06-github-secrets.md](docs/06-github-secrets.md). Al usar una cuenta AWS propia con usuario IAM y Elastic IP no es necesario rotar credenciales ni `EC2_HOST` entre despliegues.

## Estructura del proyecto

```
src/main/java/cl/duoc/ejemplo/ms/administracion/archivos/
├── config/          SecurityConfig (Entra ID), RabbitConfig (colas)
├── controller/      AwsS3Controller (utilitarios S3)
├── dto/             DTOs S3 y errores
├── exception/       Excepciones + GlobalExceptionHandler
├── guia/            Dominio guías: entidades, repos, servicio,
│                    controller, productor y consumidor RabbitMQ
└── service/         AwsS3Service, EfsService
```
