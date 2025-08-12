# Inka Toys Luna POS — Flutter (Opción A)

**Targets**: Android, iOS, Windows, macOS y Web (PWA).

## Cómo iniciar
1. Crea un proyecto base (puedes usar este directamente o correr `flutter create .` aquí).
2. Copia tus logos en `assets/` como:
   - `assets/logo_color.png` (para la app y cabeceras)
   - `assets/logo_grayscale.png` (para el ticket)
3. `flutter pub get`
4. Ejecuta en la plataforma deseada (ej: `flutter run -d windows`).

## Funcionalidades incluidas
- Inicio de venta con botón **rojo** y letras blancas.
- Generación de **ticket PDF** con logo **en gris** de alta calidad, separadores, **QR** y **Code128**.
- Carpeta **CONTABILIDAD/YYYYMMDD/** con ticket y `{fecha}.csv`.
- Resumen del día con contadores en vivo (placeholder).
- Impresión nativa por plataforma vía `printing`:
  - iOS/macOS: AirPrint
  - Android: print intent / servicios del sistema
  - Windows: diálogo del sistema (silenciosa requiere soluciones específicas).

> Nota: para impresoras térmicas ESC/POS por Bluetooth/Wi‑Fi, integrar paquete específico según hardware.


### Build con un clic (GitHub)
- Sube esta carpeta a un repositorio en GitHub.
- Ve a **Actions** → ejecuta **Flutter Builds**.
- Descarga **Android-APK** y **Windows-EXE** desde los artifacts.
