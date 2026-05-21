# JettraPluginStore - Guía del Usuario

**JettraPluginStore** es una suite de plugins y una herramienta de línea de comandos (CLI) basada en Java y Maven. Su objetivo principal es permitir la conversión de proyectos de la familia Jettra (particularmente `JettraWeb`) en plugins distribuibles, los cuales pueden ser almacenados en un repositorio central en GitHub y posteriormente instalados o removidos de otros proyectos de forma sencilla.

---

## Funcionalidad General

El sistema funciona como una aplicación "standalone" que ejecuta las siguientes tareas:
1. Preparar un proyecto local para convertirse en plugin.
2. Empaquetar y refactorizar el proyecto, subiéndolo automáticamente al repositorio `JettraAppStore`.
3. Listar los plugins disponibles en la tienda.
4. Descargar, desempaquetar e integrar un plugin dentro de un proyecto Jettra existente.
5. Remover la integración de un plugin previamente instalado.

La interacción con GitHub requiere que el usuario proporcione sus credenciales (Nombre de usuario y un Token de Acceso Personal - PAT). Estas credenciales se almacenan localmente de forma encriptada usando un algoritmo AES y una **frase clave** que el usuario debe proporcionar en tiempo de ejecución.

---

## Configuración de Credenciales de GitHub

La herramienta almacena las credenciales de GitHub en un archivo llamado `jettraappstore.md` en el directorio donde se ejecute la herramienta. 

Debido a que este archivo contiene información sensible (Usuario y Password/Token de GitHub con permisos de escritura para el repositorio https://github.com/avbravo/JettraAppStore.git), el sistema lo encriptará. Cada vez que ejecutes un comando que se conecte a GitHub (como `createplugin` o `list`), la consola te solicitará ingresar tu frase clave para descifrar el acceso.

*Nota de seguridad:* GitHub ya no permite la autenticación por contraseña tradicional. El "password" que registres en el sistema debe ser un **Personal Access Token (PAT)** con los permisos adecuados sobre el repositorio.

---

## Comandos y Flujo de Trabajo

### 1. Preparar un Plugin: `prepareplugin`

```bash
java -jar JettraPluginStore.jar -c prepareplugin
```
**Descripción:** 
Este comando analiza el archivo `pom.xml` del proyecto actual y genera un archivo de descripción inicial llamado `plugin-descriptor.md`. 

Este archivo incluye la siguiente estructura (que puede ser modificada manualmente luego de su generación):
```markdown
Name: Nombre de plugin
ArtifactId: nombre de artefacto en minúsculas
Plugin-Package: com.jettrapluginstore.<nombre-plugin>
Versión: <extraída del pom.xml>
Autor: Nombre del autor
Email: correo@ejemplo.com
WebSite: https://ejemplo.com
Description: Descripción del plugin
Dependencies: jettraServer, JettraReport, JettraWUI
DateTime: 2026-05-21 12:00:00
```

---

### 2. Crear un Plugin: `createplugin`

```bash
java -jar JettraPluginStore.jar -c createplugin
```
**Descripción:** 
Convierte el proyecto actual en un plugin Jettra distribuible y lo sube al repositorio `JettraAppStore.git`. El sistema asume que la configuración ya fue establecida previamente en `plugin-descriptor.md`.

**Procesos automatizados:**
1. **Validación:** Se conecta a GitHub para validar que no exista conflicto con el `Name`, `ArtifactId` y `Versión` en el repositorio remoto.
2. **Refactorización de Paquetes:** Clona el proyecto en un directorio temporal en tu disco. Renombra toda la estructura del proyecto Java para emparejarla con el `Plugin-Package` especificado.
3. **Manejo de Propiedades:** Modifica el `messages.properties` para evitar conflictos añadiendo el prefijo `artifactId_` a cada etiqueta del plugin (por ejemplo: `nombre` se transforma en `factura_nombre`).
4. **Manejo de Handlers y Menús:**
   - Lee el archivo `Main.java` u homólogo, extrae los "handlers" y crea un `main.md`. Elimina el archivo Java original de la distribución.
   - Lee el archivo `DashboardBasePage.java`, extrae la configuración de menús al archivo `dashboardbasepage.md` y elimina el archivo Java.
5. **Empaquetado:** Comprime la estructura refactorizada en un archivo `.zip`.
6. **Subida a GitHub:** Crea un directorio con el nombre del `artifactId` y subdirectorio para la versión, sube el `.zip`, actualiza el catálogo `plugin-db.md` (y el de handlers `main-db.md`), y hace el "push" final.

---

### 3. Listar Plugins: `list`

```bash
java -jar JettraPluginStore.jar -c list
```
**Descripción:**
Se conecta al repositorio `JettraAppStore.git`, lee el archivo catalogador `plugin-db.md` y muestra en la consola una lista estructurada de todos los plugins que se encuentran habilitados y disponibles para su descarga.

---

### 4. Instalar un Plugin: `installplugin`

```bash
java -jar JettraPluginStore.jar -c installplugin -artifactid <artifactid> -v <version>
```
**Descripción:**
Integra un plugin remoto en el proyecto JettraWeb actual.

**Procesos automatizados:**
1. **Descarga:** Localiza el archivo `.zip` en el repositorio en base al `artifactId` y la versión, y lo descomprime sobre el código del proyecto actual.
2. **Dependencias:** Lee el `plugin-descriptor.md` del paquete y verifica dependencias cruzadas con el `pom.xml` local, para evitar duplicidades y actualizarlo en caso necesario.
3. **Propiedades:** Lee las etiquetas de `messages.properties` (que ya vienen con el prefijo) y las añade al `messages.properties` del proyecto base.
4. **Main & Handlers:** Inserta los "handlers" alojados en el `main.md` dentro de la clase `Main.java` del proyecto local e informa al usuario sobre potenciales conflictos de "path".
5. **Dashboard:** Lee el `dashboardbasepage.md` para integrar las nuevas opciones al menú dentro del `DashboardPage.java` local.
6. **Reporte:** Al finalizar, se crea un informe de resultados en la raíz del proyecto llamado `plugin-result.md` listando qué fue modificado y posibles problemas no automatizables.

---

### 5. Remover un Plugin: `removeplugin`

```bash
java -jar JettraPluginStore.jar -c removeplugin -artifactid <artifactid>
```
**Descripción:**
Limpia un proyecto deshaciendo la integración de un plugin previamente instalado.

**Procesos automatizados:**
1. **Propiedades:** Elimina del `messages.properties` todas aquellas etiquetas que empiecen con `artifactId_`.
2. **Dependencias:** Lee las dependencias que originó el plugin y si no son usadas por otros módulos, las retira del `pom.xml`.
3. **Handlers y Dashboard:** Revisa los archivos `.md` del caché del plugin y retira estas líneas específicas de código del `Main.java` y `DashboardPage.java`.
4. **Código Fuente:** Borra el paquete Java que corresponde exclusivamente al módulo desinstalado.

---

## Compilación del Proyecto y Generación del JAR

Para utilizar el sistema `JettraPluginStore`, primero debes compilar el proyecto en un archivo ejecutable "fat jar" que contenga todas sus dependencias. Hemos incluido un script automatizado para facilitar este proceso.

### Ejecutando el script de compilación (Linux/Mac)

En la raíz del proyecto `JettraPluginStore`, puedes utilizar el script proporcionado:

```bash
./build.sh
```

Este script ejecuta `mvn clean package` y, si la compilación es exitosa, generará el archivo JAR empaquetado dentro de la carpeta `target/`. Verás un mensaje indicando la ruta exacta (generalmente `target/JettraPluginStore-1.0-SNAPSHOT-shaded.jar`).

### Compilación Manual (Todas las plataformas)

Si prefieres usar la consola o estás en un ambiente de Windows, puedes ejecutar:

```bash
mvn clean package
```

Una vez finalizado, busca el archivo ejecutable que termina en `-shaded.jar` en la carpeta `target/`.

---

## Integración y Uso como Dependencia o Plugin

La suite JettraPluginStore puede usarse de dos formas:

### 1. Como Herramienta CLI Independiente (Recomendado)

La forma principal de uso es invocar el archivo JAR directamente desde el directorio de **tu proyecto JettraWeb destino** (y no desde la carpeta de JettraPluginStore).

1. Abre una terminal y navega hasta el directorio de tu proyecto Jettra (ej. `mi-proyecto-web`).
2. Llama a la herramienta apuntando al archivo JAR generado:

```bash
java -jar /ruta/a/JettraPluginStore/target/JettraPluginStore-1.0-SNAPSHOT-shaded.jar -c prepareplugin
```

> **Consejo:** Puedes crear un alias en tu sistema operativo (`~/.bashrc` o `~/.zshrc`) para hacer que el comando sea global:
> `alias jettrapluginstore="java -jar /ruta/a/JettraPluginStore/target/JettraPluginStore-1.0-SNAPSHOT-shaded.jar"`
> Esto te permitirá usar `jettrapluginstore -c <comando>` libremente desde cualquier carpeta.

### 2. Como Dependencia en otros Proyectos Maven

Si deseas utilizar la lógica de `JettraPluginStore` a nivel programático dentro de otro proyecto (por ejemplo, para construir tu propia consola personalizada integrada en otro plugin de la suite), puedes añadirlo como dependencia de Maven.

Primero, instálalo en tu repositorio local `.m2`:

```bash
cd JettraPluginStore
mvn clean install
```

Luego, en el `pom.xml` del proyecto destino donde deseas usar sus capacidades, añade lo siguiente:

```xml
<dependency>
    <groupId>com.jettra</groupId>
    <artifactId>JettraPluginStore</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

A partir de este momento, podrás instanciar libremente `CredentialsManager`, `GitUtils`, o invocar las clases Command (ej. `InstallPluginCommand`) programáticamente dentro de tu propio código.
