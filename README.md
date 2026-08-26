# Carretero Backend — API del Sistema de Ventas

API REST del sistema de punto de venta de **El Carretero (Burgers & Wings)**, un restaurante en Perú. Gestiona todo el ciclo de la venta: toma de pedidos en salón, venta rápida en mostrador, comandas a las estaciones de cocina en tiempo real, cobros, turnos de caja y emisión de comprobantes.

El frontend que consume esta API está en el repositorio [Carretero-frontend-angular](https://github.com/JhersonMes/Carretero-frontend-angular).

---

## Qué hace

| Módulo | Descripción |
|---|---|
| **Salón** | Mesas (`Mesa 1-6`, `Barra 1-2`) con estados libre/ocupada, pedido por mesa y checkout. |
| **Venta rápida** | Flujo de mostrador sin asignar mesa. |
| **Cocina** | Comandas que llegan por WebSocket a tres estaciones: `COCINA`, `PARRILLA`, `BEBIDAS`. Cada producto se rutea a su estación. |
| **Carta** | Categorías, productos, y *sabores/opciones* con ajuste de precio (ej. el café Latte suma S/ 1.00 sobre el americano). |
| **Caja** | Apertura y cierre de turnos, movimientos de efectivo, arqueo. |
| **Cobros** | Métodos de pago sembrados: EFECTIVO, YAPE, PLIN, TARJETA, TRANSFERENCIA. |
| **Comprobantes** | Boleta (`B001`), Factura (`F001`) y Nota de Venta (`NV01`). Consulta de DNI/RUC vía SUNAT. |
| **Usuarios y roles** | 6 roles: `ADMIN`, `CAJERO`, `MESERO`, `COCINA`, `PARRILLA`, `BEBIDAS`. |
| **Impresión** | Endpoint para tickets en impresora térmica. |

## Stack

- **Java 17** · **Spring Boot 3.3.0**
- Spring Security + **JWT** (jjwt 0.12.5)
- Spring Data JPA · **Hibernate 6.5.2** · **MySQL 8** · HikariCP
- WebSocket **STOMP** (broker simple, endpoint `/ws`)
- ModelMapper · Spring HATEOAS · Bean Validation · Lombok

---

## Requisitos

| Herramienta | Versión | Nota |
|---|---|---|
| **JDK** | 17 o superior | Verifica con `java -version` |
| **MySQL Server** | 8.x | Debe estar **corriendo** en el puerto 3306 |
| Maven | — | **No hace falta instalarlo**, el proyecto trae `mvnw` |

No necesitas crear la base de datos a mano: la URL usa `createDatabaseIfNotExist=true` y Hibernate genera las tablas con `ddl-auto=update`.

---

## Cómo ponerlo a funcionar

### 1. Clonar

```bash
git clone https://github.com/JhersonMes/Carretero-backend.git
cd Carretero-backend
```

### 2. Verificar que MySQL esté encendido

```powershell
# Windows
Get-Service MySQL80
```

```bash
# Linux / macOS
sudo systemctl status mysql
```

### 3. Crear el archivo `.env`

El repositorio **no incluye credenciales**. Copia la plantilla:

```bash
cp .env.example .env      # Linux / macOS
copy .env.example .env    # Windows CMD
```

Y complétala con **tus** datos de MySQL:

```env
DB_USERNAME=root
DB_PASSWORD=tu_password_de_mysql
JWT_SECRET=<genera uno, ver abajo>
```

### 4. Generar el `JWT_SECRET`

⚠️ **No inventes una cadena corta.** El token se firma con `Keys.hmacShaKeyFor(secret.getBytes())`, así que el secreto necesita **mínimo 64 bytes** para HS512. Si es más corto, la app arranca pero falla al iniciar sesión.

```powershell
# Windows PowerShell
$b = New-Object byte[] 64; [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

```bash
# Linux / macOS
openssl rand -base64 64 | tr -d '\n'
```

Pega el resultado en `JWT_SECRET`.

### 5. Arrancar

```powershell
.\mvnw.cmd spring-boot:run    # Windows
```

```bash
./mvnw spring-boot:run        # Linux / macOS
```

Cuando veas esto ya está listo en **http://localhost:8080**:

```
Started CarreteroBackendApplication in 5.3 seconds
```

---

## ⚠️ Lo que más rompe la instalación

### Las variables de entorno de Windows le ganan al `.env`

La librería `spring-dotenv` registra sus valores **después** de las variables del sistema (`addAfter(SYSTEM_ENVIRONMENT...)`), o sea con **menos prioridad**. Si tienes una variable `DB_PASSWORD` definida en Windows, tu `.env` será ignorado por completo y verás:

```
Access denied for user 'root'@'localhost' (using password: YES)
```

...aunque el `.env` tenga la contraseña correcta. Para revisarlo y limpiarlo:

```powershell
# ¿Existen?
[Environment]::GetEnvironmentVariables("User").GetEnumerator() | Where-Object { $_.Name -match "^DB_|^JWT_" }

# Borrarlas
foreach ($v in "DB_USERNAME","DB_PASSWORD","JWT_SECRET") { [Environment]::SetEnvironmentVariable($v, $null, "User") }
```

Después **reinicia el IDE**: IntelliJ hereda las variables de cuando se abrió, no las relee solo.

---

## Datos que se crean solos (primer arranque)

`DataInitializer` siembra todo automáticamente. **Usuarios listos para entrar:**

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `admin123` | ADMIN |
| `caja1` … `caja4` | `caja1123` … `caja4123` | CAJERO |
| `mesero1` … `mesero4` | `mesero1123` … `mesero4123` | MESERO |
| `cocina` | `cocina123` | COCINA |
| `parrilla` | `parrilla123` | PARRILLA |
| `bebidas` | `bebidas123` | BEBIDAS |

> 🔒 Son credenciales de desarrollo. **Cámbialas antes de usar el sistema en el restaurante.**

También se siembran: los 6 roles, los 5 métodos de pago, las 8 mesas, la configuración del negocio y la carta completa (ADICIONALES, ALITAS DE CERDO, ALITAS DE POLLO, BEBIDAS) con sus sabores.

📌 **Los precios se siembran en `0.00`** porque la carta original no los traía. Después del primer arranque entra a la pantalla de **Productos / Carta** y cárgalos. La única excepción es el Café (S/ 7.00 base).

---

## Endpoints principales

Todo requiere `Authorization: Bearer <token>` salvo los marcados como públicos.

| Método | Ruta | |
|---|---|---|
| POST | `/login` | 🔓 público — devuelve `{ jwtToken }` |
| POST | `/register` | 🔓 público |
| GET | `/auth/user` | datos del usuario del token |
| GET | `/auth/logout` | |
| — | `/ws` | 🔓 WebSocket STOMP (topics en `/topic`) |

Recursos REST: `/products`, `/categories`, `/flavors`, `/orders`, `/payments`, `/payment-methods`, `/clients`, `/users`, `/roles`, `/tables`, `/cash-shifts`, `/ingredients`, `/menus`, `/invoices`, `/business-config`, `/addresses`, `/sunat`, `/printers`.

---

## Problemas comunes

| Síntoma | Causa y solución |
|---|---|
| `Access denied for user 'root'@'localhost' (using password: YES)` | Contraseña incorrecta **o** una variable de entorno de Windows pisando el `.env` (ver arriba). Prueba tus credenciales con `mysql -u root -p`. |
| `Communications link failure` | MySQL no está corriendo. Levanta el servicio. |
| `Could not resolve placeholder 'DB_USERNAME'` | Falta el archivo `.env`, o lo ejecutas desde otra carpeta. `spring-dotenv` lo busca en el **directorio de trabajo**: debe ser la raíz de `Carretero-backend`. |
| `Port 8080 was already in use` | Otra instancia sigue viva. Ciérrala o cambia `server.port` en `application.properties`. |
| Login devuelve 500 o token inválido | `JWT_SECRET` demasiado corto. Regenera con el comando del paso 4. |

---

## Lo que NO se sube al repositorio

`.env` (credenciales), `target/` (compilados), `.idea/` y `*.iml` (config del IDE), `*.log`.

El archivo `.env.example` **sí** se sube: es la plantilla, y va vacío.
