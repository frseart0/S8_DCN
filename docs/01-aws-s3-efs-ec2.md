# Configuración AWS: S3, EFS y EC2

## 0. Usuario IAM para la aplicación

1. Consola AWS → **IAM → Users → Create user**. Nombre: `ms-guias-app`.
2. **Attach policies directly** (reemplaza `<bucket>`):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::<bucket>",
        "arn:aws:s3:::<bucket>/*"
      ]
    }
  ]
}
```

1. **Security credentials → Access keys → Create access key** (caso de uso: *Application running outside AWS*).
2. Guarda `Access key ID` y `Secret access key`: son los secretos `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY`.

## 1. Bucket S3

1. Consola AWS → **S3** → **Create bucket**.
2. Nombre único: `guias-despacho-frsearto` (secreto `S3_BUCKET`).
3. Región `us-east-2`
4. Crear.

La aplicación organiza los archivos con la estructura requerida:

```
{yyyyMMdd}/{transportista}/{archivo}.pdf
Ejemplo: 20260707/transportistaX/guia123.pdf
```

La clave se construye en `GuiaService.subirArchivo` a partir de la fecha y transportista de la guía.

## 2. EFS (almacenamiento temporal)

1. Consola AWS → **EFS** → **Create file system**.
2. Nombre `efs-guias`, misma VPC que la EC2, opciones por defecto.
3. En **Network**, verifica que el security group del mount target permita **NFS (puerto 2049)** desde el security group de la EC2.

### Montaje en la EC2

```bash
# Instalar cliente NFS (Amazon Linux 2023)
sudo dnf install -y nfs-utils

# Crear punto de montaje
sudo mkdir -p /mnt/efs

# Montar (reemplaza fs-XXXX con el ID de EFS)
sudo mount -t nfs4 -o nfsvers=4.1 fs-XXXX.efs.us-east-2.amazonaws.com:/ /mnt/efs

# Dar permisos de escritura
sudo chmod 777 /mnt/efs

# Montaje automático al reiniciar
echo "fs-XXXX.efs.us-east-1.amazonaws.com:/ /mnt/efs nfs4 nfsvers=4.1,defaults,_netdev 0 0" | sudo tee -a /etc/fstab
```

El contenedor recibe `/mnt/efs` montado en `/app/efs` (ver `docker-compose.yml`, variable `EFS_MOUNT`). La app escribe ahí a través de `EfsService` antes de subir a S3.

## 3. Instancia EC2

1. Consola AWS → **EC2** → **Launch instance**.
2. Nombre `ec2-guias`, AMI **Amazon Linux 2023**, tipo `t2.micro` **o** `t3.micro`
3. Key pair: **Create new key pair**. Descargar el `.pem` generado: su contenido completo es el secreto `EC2_SSH_KEY`.
4. Security group con estas reglas de entrada:
  - SSH (22) desde `0.0.0.0/0` (necesario para GitHub Actions)
  - TCP 8080 (aplicación) desde `0.0.0.0/0`.
  - TCP 15672 (consola RabbitMQ, opcional)
  - NFS 2049 (para montar efs)
1. Lanzar.

### IP fija con Elastic IP

Para no tener que actualizar `EC2_HOST` y la integración del API Gateway cada vez:

1. **EC2 → Network & Security → Elastic IPs → Allocate Elastic IP address**.
2. **Actions → Associate Elastic IP address** → selecciona tu instancia `ec2-guias`.
3. Usa esa IP fija como `EC2_HOST`.

### Instalación de Docker en la EC2

```bash
sudo dnf install -y docker
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user

# Docker Compose v2 (plugin)
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# Cerrar sesión y volver a entrar para aplicar el grupo docker
exit
```

### Directorio de despliegue

El workflow de GitHub Actions copia `docker-compose.yml` a `~/app/` y genera ahí el `.env`. Solo se debe crear el directorio la primera vez:

```bash
mkdir -p ~/app
```

## 4. Credenciales AWS para la aplicación

La app usa el AWS SDK (cadena de credenciales por defecto), que lee las variables de entorno `AWS_ACCESS_KEY_ID` y `AWS_SECRET_ACCESS_KEY` inyectadas por docker-compose desde el `.env`. Al usar un usuario IAM propio (ver sección 0)