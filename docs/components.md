# Componentes reutilizables de Bookie

Los componentes visuales reutilizables de Bookie se encuentran en el paquete:

`org.bookie.app.presentation.components`

Los ejemplos asumen que se utilizan dentro de una función `@Composable` y que se encuentran importados los componentes correspondientes.

## 1. BookieButton

Botón reutilizable que permite seleccionar entre tres variantes visuales.

**Variantes disponibles:**
- `PRIMARY`: botón principal.
- `SECONDARY`: botón secundario.
- `TEXT`: botón de texto.

**Ejemplo de uso:**

```kotlin
BookieButton(
    text = "Iniciar sesión",
    onClick = { /* Acción */ },
    type = BookieButtonType.PRIMARY
)
```

## 2. BookieTextField

Campo de texto reutilizable que permite configurar etiquetas, placeholders y estados de error.

**Ejemplo de uso:**

```kotlin
var email by remember { mutableStateOf("") }

BookieTextField(
    value = email,
    onValueChange = { email = it },
    label = "Email",
    placeholder = "Ingresá tu email",
    isError = false
)
```

Para mostrar un error:

```kotlin
BookieTextField(
    value = email,
    onValueChange = { email = it },
    label = "Email",
    isError = true,
    errorMessage = "El email ingresado no es válido"
)
```

## 3. BookieBookCard

Tarjeta reutilizable para mostrar información de un libro, incluyendo título, autor y, opcionalmente, portada (mediante un objeto Painter) y calificación.

**Ejemplo de uso:**

```kotlin
BookieBookCard(
    title = "Cien años de soledad",
    author = "Gabriel García Márquez",
    rating = "4.8",
    onClick = { /* Acción */ }
)
```

Si no se proporciona una portada, el componente muestra un ícono predeterminado.

## 4. Estados de pantalla

Se ofrecen tres componentes para representar distintos estados de la interfaz.

**BookieLoadingState:** indica que se está cargando contenido.

```kotlin
BookieLoadingState(
    message = "Cargando libros..."
)
```

**BookieEmptyState:** informa que no hay contenido disponible.

```kotlin
BookieEmptyState(
    message = "Todavía no hay libros para mostrar"
)
```

**BookieErrorState:** informa que ocurrió un error y permite configurar una acción opcional para reintentar.

```kotlin
BookieErrorState(
    message = "No se pudieron cargar los libros",
    onRetry = { /* Reintentar */ }
)
```

## Pantalla de ejemplo

El archivo `BookieComponentsScreen.kt` contiene una pantalla demostrativa que reúne los componentes y sus distintas variantes para facilitar su visualización y consulta.

Esta pantalla funciona como referencia de uso y no forma parte del flujo principal de navegación de la aplicación.