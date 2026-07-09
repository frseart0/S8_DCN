# Secretos de GitHub Actions

El workflow [.github/workflows/deploy.yml](../.github/workflows/deploy.yml) necesita estos secretos. Se configuran en el repositorio de GitHub: **Settings → Secrets and variables → Actions → New repository secret**.

## Tabla completa

| Secreto | Dónde encontrarlo | Notas |
|---|---|---|
| `DOCKERHUB_USERNAME` | Tu nombre de usuario de [hub.docker.com](https://hub.docker.com) | |
| `DOCKERHUB_TOKEN` | Docker Hub → avatar → **Account Settings → Security → Personal access tokens → Generate new token** (permisos Read & Write) | Se usa en lugar de la contraseña |
| `EC2_HOST` | Consola AWS → **EC2 → Instances →** tu instancia → **Public IPv4 address** (o mejor, la Elastic IP asociada, ver [01-aws-s3-efs-ec2.md](01-aws-s3-efs-ec2.md)) | Con Elastic IP no cambia al reiniciar la instancia |
| `EC2_USER` | Depende de la AMI: `ec2-user` (Amazon Linux) o `ubuntu` (Ubuntu) | |
| `EC2_SSH_KEY` | El `.pem` generado al crear el key pair de tu instancia (**EC2 → Key Pairs → Create key pair**) → pega el contenido completo (incluidas las líneas BEGIN/END) | Guárdalo bien: AWS solo lo entrega una vez |
| `AWS_ACCESS_KEY_ID` | **IAM → Users →** tu usuario (ej. `ms-guias-app`) → **Security credentials → Access keys** | Credencial de larga duración; no expira sola (ver sección 0 de [01-aws-s3-efs-ec2.md](01-aws-s3-efs-ec2.md)) |
| `AWS_SECRET_ACCESS_KEY` | Mismo lugar, se muestra una única vez al crear la access key | Guárdalo al crearla; si lo pierdes debes generar una nueva |
| `S3_BUCKET` | Consola AWS → **S3** → nombre de tu bucket | Ej: `guias-despacho-frsea` |
| `AZURE_TENANT_ID` | [entra.microsoft.com](https://entra.microsoft.com) → **App registrations →** tu app → **Overview → Directory (tenant) ID** | |
| `AZURE_CLIENT_ID` | Mismo lugar → **Application (client) ID** | |
| `RABBITMQ_USER` | Lo defines tú (no viene de ningún portal) | Ej: `admin` |
| `RABBITMQ_PASSWORD` | Lo defines tú | Usa una contraseña fuerte |
| `ORACLE_DB_URL` | OCI Console → **Autonomous Database → Database connection → TNS name** (ej: `guiasdb_high`) | Solo el alias, no la cadena completa |
| `ORACLE_DB_USER` | Usuario creado en Database Actions (ver [04-oracle-cloud.md](04-oracle-cloud.md)) | Ej: `GUIAS_APP` |
| `ORACLE_DB_PASSWORD` | La contraseña que definiste al crear ese usuario | |
| `ORACLE_WALLET_B64` | Wallet zip descargado de OCI (**Database connection → Download wallet**) codificado en base64 | Ver comando abajo |

## Cómo generar `ORACLE_WALLET_B64`

```powershell
# Windows (PowerShell) - deja el base64 en el portapapeles
[Convert]::ToBase64String([IO.File]::ReadAllBytes("Wallet_guiasdb.zip")) | Set-Clipboard
```

```bash
# Linux / macOS
base64 -w0 Wallet_guiasdb.zip
```

## Cuenta AWS propia: sin rotación por sesión

A diferencia de AWS Academy (credenciales temporales que expiran cada pocas horas), con un usuario IAM de tu cuenta propia `AWS_ACCESS_KEY_ID` y `AWS_SECRET_ACCESS_KEY` son de larga duración: no necesitas actualizarlas en GitHub antes de cada deploy.

Buenas prácticas igual recomendadas:

1. Usa una **Elastic IP** en la EC2 (ver [01-aws-s3-efs-ec2.md](01-aws-s3-efs-ec2.md)) para que `EC2_HOST` no cambie nunca y no tengas que tocar tampoco la integración del API Gateway ([02-aws-api-gateway.md](02-aws-api-gateway.md)).
2. Rota manualmente el access key cada cierto tiempo o si sospechas que se filtró (**IAM → Users → Security credentials → Deactivate/Delete** la antigua y crea una nueva).
3. Mantén la política IAM del usuario acotada solo a las acciones y al bucket que la app necesita (principio de menor privilegio, ver sección 0 de [01-aws-s3-efs-ec2.md](01-aws-s3-efs-ec2.md)).

## Qué NO va en GitHub Secrets

- El archivo `.env` local (está en `.gitignore`).
- El wallet descomprimido (`wallet/`, también ignorado).
- El client secret de Postman (solo se usa para obtener tokens de prueba, el backend no lo necesita).
