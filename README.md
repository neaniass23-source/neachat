# AnunciosChat

Plugin para Spigot/Paper **1.20.1+** (Java 17+). Envía anuncios automáticos por el chat con:

- Colores `&a` y hexadecimales `&#RRGGBB`, negrita, subrayado...
- Texto **clicable** (abrir enlace, ejecutar comando...) y **hover**.
- Líneas **centradas**.
- **Imagen de píxeles** a la izquierda con el texto a la derecha (como los anuncios de Discord).
- Variables `%player%` y PlaceholderAPI (opcional).

## Compilar
Sube el contenido a GitHub: la pestaña **Actions** compila sola y deja el jar en *Artifacts*. En local: `mvn clean package`.

## Comandos
| Comando | Permiso | Qué hace |
|---|---|---|
| `/anuncios enviar <id>` | `anuncioschat.admin` | Envía un anuncio a todos ahora |
| `/anuncios probar <id>` | `anuncioschat.admin` | Lo ves solo tú |
| `/anuncios lista` | `anuncioschat.admin` | Lista los anuncios |
| `/anuncios recargar` | `anuncioschat.admin` | Recarga `config.yml` |
| `/anuncios silenciar` | `anuncioschat.silenciar` (todos) | Activa/desactiva los anuncios para ti |

## Imágenes
Incluye `discord.png` y `almas.png` de ejemplo. Pon un PNG en `plugins/AnunciosChat/imagenes/` y referencialo en el anuncio. Se reescala a `ancho` x `alto` caracteres, así que usa imágenes pequeñas (pixel art). Los píxeles transparentes se pintan con `imagen-color-transparente`.

El formato completo de las líneas está explicado al principio de `config.yml`.
