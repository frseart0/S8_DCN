# Configuración Microsoft Entra ID

IDaaS del proyecto (Semanas 6 y 8). La guía original pide Azure AD B2C, pero este proyecto usa **Microsoft Entra ID** (mismo proveedor de identidad de Microsoft, sucesor de Azure AD): la autenticación del backend y del API Gateway se valida contra el tenant de Entra, y los 2 roles requeridos se implementan como **App Roles**.

Portal: [entra.microsoft.com](https://entra.microsoft.com) (o portal.azure.com → Microsoft Entra ID).

## 1. App registration

1. **Identity → Applications → App registrations → New registration**.
2. Nombre: `ms-guias-despacho`.
3. Supported account types: **Accounts in this organizational directory only** (single tenant).
4. Redirect URI: tipo **Web**, valor `https://oauth.pstmn.io/v1/callback` (para poder obtener tokens desde Postman).
5. **Register**.

En **Overview** anota:

- **Application (client) ID** → variable/secreto `AZURE_CLIENT_ID`.
- **Directory (tenant) ID** → variable/secreto `AZURE_TENANT_ID`.

## 2. Client secret (para obtener tokens desde Postman)

1. En la app → **Certificates & secrets → New client secret**.
2. Descripción `postman`, expiración 6 meses.
3. Copia el **Value** inmediatamente (no se vuelve a mostrar). Se usa solo en Postman, no en el backend.

## 3. Crear los 2 App Roles

En la app → **App roles → Create app role** (dos veces):

| Display name | Value | Allowed member types | Descripción |
|---|---|---|---|
| Lector de guías | `GUIA_LECTOR` | Users/Groups | Solo puede descargar guías |
| Operador de guías | `GUIA_OPERADOR` | Users/Groups | Puede usar el resto de los endpoints |

El `Value` es lo que llega en el claim `roles` del JWT y lo que Spring Security convierte en `ROLE_GUIA_LECTOR` / `ROLE_GUIA_OPERADOR` (ver `SecurityConfig`).

## 4. Asignar roles a usuarios

1. **Identity → Applications → Enterprise applications** → busca `ms-guias-despacho`.
2. **Users and groups → Add user/group**.
3. Asigna un usuario con rol **Lector de guías** y otro con **Operador de guías** (crea usuarios de prueba en **Identity → Users** si es necesario).

## 5. Exponer la API y scope

Para que el token de acceso incluya los roles y tenga como audiencia esta app:

1. En la app → **Expose an API → Add** (Application ID URI): acepta el valor `api://<client-id>`.
2. **Add a scope**: nombre `acceso_api`, consentimiento **Admins and users**, estado habilitado.
3. En **API permissions → Add a permission → My APIs** → selecciona la app → marca `acceso_api` → **Grant admin consent**.

## 6. Obtener token en Postman (evidencia)

En Postman, pestaña **Authorization** del request o colección:

- Type: **OAuth 2.0**
- Grant type: **Authorization Code**
- Callback URL: `https://oauth.pstmn.io/v1/callback` (marca *Authorize using browser*)
- Auth URL: `https://login.microsoftonline.com/<TENANT_ID>/oauth2/v2.0/authorize`
- Access Token URL: `https://login.microsoftonline.com/<TENANT_ID>/oauth2/v2.0/token`
- Client ID: `<CLIENT_ID>`
- Client Secret: `<client secret del paso 2>`
- Scope: `api://<CLIENT_ID>/acceso_api`

**Get New Access Token** → inicia sesión con el usuario de prueba → usa el token. Puedes verificar el contenido del JWT en [jwt.ms](https://jwt.ms): debe incluir el claim `"roles": ["GUIA_OPERADOR"]` (o `GUIA_LECTOR`) y el issuer `https://login.microsoftonline.com/<TENANT_ID>/v2.0`.

## 7. Cómo lo valida el backend

- `application.properties` define el issuer:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://login.microsoftonline.com/${AZURE_TENANT_ID}/v2.0
```

- `SecurityConfig` exige JWT en todos los endpoints y aplica las reglas:
  - `GET /guias/{id}/descargar` → requiere rol `GUIA_LECTOR`.
  - Cualquier otro endpoint → requiere rol `GUIA_OPERADOR`.

## 8. Matriz de pruebas para evidencias

| Escenario | Resultado esperado |
|---|---|
| Sin token | 401 Unauthorized |
| Token usuario GUIA_LECTOR → `GET /guias/1/descargar` | 200 OK |
| Token usuario GUIA_LECTOR → `POST /guias` | 403 Forbidden |
| Token usuario GUIA_OPERADOR → `POST /guias` | 201 Created |
| Token usuario GUIA_OPERADOR → `GET /guias/1/descargar` | 403 Forbidden |
