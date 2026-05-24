# JettraPluginStore

## Descripción General
`JettraPluginStore` es una tienda o repositorio de extensiones e integraciones creadas por la comunidad o equipo base para el JettraStack. Esta aplicación posee una interfaz visual de usuario, manejada con JavaFX.

## Detalles Específicos
- **Arquitectura general**: Aplicación de escritorio desarrollada en JavaFX (`ProjectModifier`, `PreparePluginCommand`) que sirve como gestor central.
- **Dependencias clave**: JavaFX para la UI, Maven Invoker o herramientas similares para manipular los `pom.xml` e inyectar dependencias en los proyectos destino.
- **Roles dentro del sistema**: Facilitar la distribución, descubrimiento e instalación de componentes adicionales al entorno de trabajo del usuario. Modifica directamente el código y los poms (`ProjectModifier`).

## Características Detalladas
- **Modificador de Proyectos**: Utilidad (`ProjectModifier`) que añade dependencias y configuraciones automáticamente en el `pom.xml` de los proyectos objetivo al instalar un plugin.
- **Lanzador de Comandos**: Clase `PreparePluginCommand` para empaquetar o lanzar acciones predefinidas por el plugin.
- **Temas y Estilos**: Soporte para UI de temas blanco y oscuro, adaptado a la preferencia del sistema a través de CSS.

## Guía de Entrenamiento (AI / Nuevas Características)
- Para soportar un nuevo tipo de "instalación" en la tienda de plugins, extiende el `ProjectModifier` de forma robusta utilizando parsers XML para el POM.
- Cualquier modificación a la UI de JavaFX debe aplicar sus respectivos estilos CSS para asegurar compatibilidad con temas claros y oscuros.
- Los comandos deben ejecutarse en procesos separados o con manejo adecuado de hilos de JavaFX (`Platform.runLater()`) para no bloquear la pantalla.
