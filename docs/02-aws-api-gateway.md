# Configuración AWS API Gateway

Registro y securitización de todos los endpoints del microservicio en API Gateway (Semanas 6 y 8).
## 1. Crear la API

1. Consola AWS → **API Gateway** → **Create API** → **HTTP API** → **Build**.
2. Nombre: `api-guias-despacho`.
3. No agregues integraciones todavía; **Next** hasta crear.

1. En la API → **Integrations** → **Manage integrations** → **Create**.
2. Tipo: **HTTP URI**.
3. URL: `http://<IP-PUBLICA-EC2>:8080/{proxy}` con método `ANY`.

> Usa la Elastic IP de la EC2 (ver [01-aws-s3-efs-ec2.md](01-aws-s3-efs-ec2.md), sección Elastic IP) para que esta URL no deba actualizarse si la instancia se reinicia.


## 3. Registrar las rutas (endpoints)

En **Routes** crear una ruta por cada endpoint del microservicio, apuntando a la integración anterior:


| Método | Ruta                    | Descripción                         |
| ------ | ----------------------- | ----------------------------------- |
| POST   | `/guias`                | Crear guía de despacho              |
| POST   | `/guias/{id}/archivo`   | Subir guía a S3 (pasa por EFS)      |
| GET    | `/guias/{id}/descargar` | Descargar guía (rol GUIA_LECTOR)    |
| PUT    | `/guias/{id}`           | Modificar guía                      |
| DELETE | `/guias/{id}`           | Eliminar guía                       |
| GET    | `/guias`                | Consultar por transportista y fecha |
| GET    | `/guias/{id}`           | Obtener guía por id                 |
| POST   | `/guias/procesar-cola`  | Consumir cola 1 y guardar en Oracle |
| GET    | `/s3/{bucket}/objects`  | Listar objetos S3                   |
| GET    | `/s3/{bucket}/object`   | Descargar objeto S3                 |
| POST   | `/s3/{bucket}/object`   | Subir objeto S3                     |
| POST   | `/s3/{bucket}/move`     | Mover objeto S3                     |
| DELETE | `/s3/{bucket}/object`   | Eliminar objeto S3                  |


Alternativa rápida: una única ruta `ANY /{proxy+}` que reenvía todo al backend (la validación fina de roles la hace Spring Security de todas formas).

## 4. Securitizar con JWT de Microsoft Entra ID

1. En la API → **Authorization** → **Create authorizer** → tipo **JWT**.
2. Nombre: `entra-jwt`.
	1. **Issuer URL**: `https://login.microsoftonline.com/<AZURE_TENANT_ID>/v2.0`
3. **Audience**: `<AZURE_CLIENT_ID>` (Application ID del app registration; si tu token trae `aud` con formato `api://<client-id>`, usa ese valor).
4. Asocia el authorizer a **todas las rutas** (Attach authorizer). El header esperado es `Authorization: Bearer <token>`.

Con esto el API Gateway rechaza con `401` cualquier request sin token válido del tenant, y Spring Security valida además el **rol** (`GUIA_LECTOR` / `GUIA_OPERADOR`) de cada endpoint.

## 5. Deploy y URL final

1. **Stages**: usa el stage por defecto `$default` con auto-deploy, o crea el stage `prod`.
2. La URL final queda como:

```
	https://<api-id>.execute-api.us-east-1.amazonaws.com/guias
```

Esta es la URL a usar en Postman para las evidencias (con el token en el header `Authorization`).

## 6. Prueba en Postman

1. Obtén un token de Entra ID (ver [03-microsoft-entra-id.md](03-microsoft-entra-id.md), sección Postman).
2. Request de ejemplo:

```
POST https://<api-id>.execute-api.us-east-1.amazonaws.com/guias
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "numeroGuia": "G-0001",
  "transportista": "transportistaX",
  "fecha": "2026-07-07"
}
```

1. Sin token → `401 Unauthorized` (evidencia de securitización).
2. Con token de rol `GUIA_LECTOR` en un endpoint distinto a descargar → `403 Forbidden`.

