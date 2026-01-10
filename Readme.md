# Sprite Lab 🧪
### Universal 2D Sprite Sheet Generator

**Sprite Lab** es una herramienta de escritorio profesional diseñada para desarrolladores de videojuegos. Su objetivo principal es facilitar el flujo de trabajo al convertir animaciones de video renderizadas (o clips de juegos) en **Sprite Sheets (hojas de sprites)** optimizadas y listas para usar en motores como **Godot Engine, Unity, Unreal, Construct o WebGL**.

Desarrollado por **FedeiaTech** utilizando **JavaFX 21** con integración nativa de **FFmpeg**, ofrece una interfaz moderna (Dark Mode) y herramientas precisas de edición.

---

## 📸 Capturas de Pantalla

### 1. Interfaz Principal (Estado Inicial)
![Interfaz Principal](assets/screenshot_empty.png)
*Interfaz limpia esperando la carga de archivos mediante Drag & Drop.*

### 2. Editor en Acción (Chroma Key + Recorte)
![Editando Video](assets/screenshot_editing.png)
*Vista previa en tiempo real con eliminación de fondo, grilla automática y visor con zoom.*

*(Nota: Las imágenes deben estar en la carpeta `assets` del repositorio).*

---

## 🚀 Características (Versión 1.0)

Esta herramienta incluye todo lo necesario para transformar un video en un asset de juego utilizable:

* **📺 Soporte Universal de Video:** Acepta formatos estándar como `.MP4`, `.AVI` y `.MOV`.
* **✂️ Recorte de Tiempo Preciso:** Slider de rango visual (RangeSlider) para seleccionar el inicio y fin exacto de la animación (Loop), con precisión de milisegundos.
* **🎨 Chroma Key Profesional:**
    * Eliminación de fondos de color sólido (Verde, Azul, Blanco, etc.).
    * **Herramienta de Gotero (Eyedropper):** Selecciona el color a eliminar haciendo clic directamente en la imagen.
    * Slider de tolerancia para bordes suaves.
* **📐 Cálculo Automático de Grilla:** El software calcula matemáticamente la mejor distribución (Filas x Columnas) y el tamaño de la imagen final basándose en la duración seleccionada y los FPS deseados.
* **🔍 Visor Avanzado:**
    * **Fondo de Ajedrez (Checkerboard):** Generado por software para visualizar transparencias reales sin errores visuales.
    * **Zoom y Paneo:** Navegación libre con la rueda del mouse y modo "Fit" (ajustar a ventana) automático.
* **💾 Exportación Inteligente:** Genera archivos PNG transparentes. El nombre del archivo se autogenera incluyendo las dimensiones de cada frame (ej: `ataque_sheet_128x128.png`), facilitando la importación en Godot/Unity.

---

## 🛠️ Requisitos del Sistema

* **Java Runtime:** JRE/JDK 21 o superior.
* **Sistema Operativo:** Windows 10/11 (Compatible con Linux/Mac compilando desde el código fuente).
* **Dependencia Externa:** Requiere el binario de `ffmpeg` local.

---

## 🔧 Instalación y Desarrollo

### Estructura de Carpetas Requerida
Para que el programa funcione correctamente, necesitas tener el ejecutable `.jar` junto a una carpeta `bin` que contenga `ffmpeg.exe`.

### Compilar desde el Código (NetBeans / Maven)

1.  **Clonar este repositorio:**
    `git clone https://github.com/FedeiaTech/SpriteLab.git`

2.  **Abrir el proyecto:**
    Usar NetBeans o cualquier IDE compatible con Maven.

3.  **Configurar Dependencias:**
    Asegurarse de tener el **JDK 21** instalado.

4.  **FFmpeg:**
    Descargar `ffmpeg.exe` y colocarlo manualmente dentro de una carpeta llamada `bin/` en la raíz del proyecto (al mismo nivel que `src` y `pom.xml`).

5.  **Ejecutar:**
    Usar el comando `Clean and Build` de Maven.

---

## 🤝 Créditos y Licencias

Este proyecto es de código abierto bajo la **Licencia MIT**.

* **Desarrollado por:** [FedeiaTech](https://github.com/FedeiaTech)
* **Librerías GUI:** JavaFX 21 & ControlsFX.
* **Procesamiento de Video:** Utiliza [FFmpeg](https://ffmpeg.org/) (Bajo licencia LGPL). *Nota: FFmpeg es un binario externo y no se distribuye directamente incrustado en el código fuente de este repositorio.*

---
*Hecho con ❤️ para la comunidad de desarrollo de videojuegos indie.*