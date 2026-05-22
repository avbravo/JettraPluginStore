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

Este script ejecuta `mvn clean package` y, si la compilación es exitosa, generará el archivo JAR empaquetado dentro de la carpeta `target/`. Verás un mensaje indicando la ruta exacta (generalmente `target/JettraPluginStore-1.0-SNAPSHOT.jar`).

### Compilación Manual (Todas las plataformas)

Si prefieres usar la consola o estás en un ambiente de Windows, puedes ejecutar:

```bash
mvn clean package
```

Una vez finalizado, busca el archivo ejecutable `JettraPluginStore-1.0-SNAPSHOT.jar` en la carpeta `target/`.

---

## Integración y Uso como Dependencia o Plugin

La suite JettraPluginStore puede usarse de dos formas:

### 1. Como Herramienta CLI Independiente (Recomendado)

La forma principal de uso es invocar el archivo JAR directamente desde el directorio de **tu proyecto JettraWeb destino** (y no desde la carpeta de JettraPluginStore).

1. Abre una terminal y navega hasta el directorio de tu proyecto Jettra (ej. `mi-proyecto-web`).
2. Llama a la herramienta apuntando al archivo JAR generado:

```bash
java -jar /ruta/a/JettraPluginStore/target/JettraPluginStore-1.0-SNAPSHOT.jar -c prepareplugin
```

> **Consejo:** Puedes crear un alias en tu sistema operativo (`~/.bashrc` o `~/.zshrc`) para hacer que el comando sea global:
> `alias jettrapluginstore="java -jar /ruta/a/JettraPluginStore/target/JettraPluginStore-1.0-SNAPSHOT.jar"`
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

### 3. Modo Shell Interactivo

JettraPluginStore incluye ahora un modo de "Shell Interactivo" para facilitar la ejecución rápida de varios comandos de manera consecutiva sin necesidad de cargar la máquina virtual de Java (JVM) y picocli en cada invocación.

**Uso desde la CLI Independiente:**
Simplemente ejecuta el JAR sin proporcionar ningún parámetro:
```bash
java -jar /ruta/a/JettraPluginStore/target/JettraPluginStore-1.0-SNAPSHOT.jar
```

Una vez dentro, el prompt cambiará a `jettrapluginstore>`. A partir de aquí, puedes ingresar tus comandos omitiendo el llamado a la herramienta:
```bash
jettrapluginstore> -c createplugin -p com.mi.paquete
jettrapluginstore> list
jettrapluginstore> exit
```
Usa el comando `exit` o `quit` para salir del shell interactivo.

**Uso cuando ha sido añadido como dependencia en un pom.xml:**
Si el plugin store está como dependencia de tu proyecto (como se indica en la sección 2), puedes ejecutar su Shell Interactivo utilizando el plugin `exec-maven-plugin`. En la terminal de tu proyecto, corre el siguiente comando:
```bash
mvn exec:java -Dexec.mainClass="com.jettrapluginstore.Main"
```
Esto inicializará el Shell Interactivo directamente desde la terminal del proyecto en el cual estás trabajando.

### Ejemplo Completo de Flujo de Trabajo en el Shell

Imagina que deseas verificar los plugins disponibles y luego crear un nuevo plugin en tu proyecto basándote en un paquete específico. En lugar de invocar `java -jar` múltiples veces, abres el shell:

```bash
$ java -jar target/JettraPluginStore-1.0-SNAPSHOT.jar
Jettra Plugin Store Interactive Shell
Type 'exit' or 'quit' to close the shell.
jettrapluginstore> 
```

**Paso 1: Listar plugins existentes en la tienda.**
```bash
jettrapluginstore> list
Conectando a JettraAppStore...
[Lista de plugins disponibles mostrada aquí]
```

**Paso 2: Preparar el plugin local.**
Genera el `plugin-descriptor.md` en tu directorio local para configurar la información del nuevo plugin.
```bash
jettrapluginstore> prepareplugin
[El sistema genera plugin-descriptor.md, lo editas en otra pestaña si es necesario]
```

**Paso 3: Convertir y subir tu plugin.**
Utilizando el parámetro `-p`, le indicas qué paquete específico se va a extraer y subir como un plugin.
```bash
jettrapluginstore> createplugin -p com.miproyecto.facturacion
Refactoring packages...
Plugin created and uploaded successfully.
```

**Paso 4: Salir del sistema.**
```bash
jettrapluginstore> exit
$
```
Gracias a este modo, ahorras el tiempo de inicialización de la herramienta entre cada comando sucesivo.

---

## 💻 Panel de Control Gráfico Futurista (JavaFX)

JettraPluginStore ahora incorpora una impresionante interfaz gráfica de usuario (**JavaFX GUI**) diseñada con una estética **cyberpunk y glassmorphism** premium. Está optimizada con paneles translúcidos de fondo, bordes brillantes con neones en tonos cian y morado, animaciones de hover y una terminal HUD integrada que reporta el flujo de compilación y subida en tiempo real.

### Características Clave de la Interfaz Gráfica:
- **Workspace Dashboard**: Permite seleccionar interactivamente cualquier directorio en disco mediante un explorador de archivos nativo, detectando automáticamente si contiene un proyecto Jettra-Maven válido y parseando su información del `pom.xml`.
- **Formulario Descriptor**: Permite editar de forma visual los campos clave de tu plugin (`Autor`, `Nombre`, `Sitio Web`, `Descripción`, `Dependencias`) para generar con un clic tu archivo `plugin-descriptor.md`.
- **Package & Upload**: Analiza dinámicamente tu código y ofrece un selector desplegable con todos los paquetes Java detectados en tu proyecto para aislarlos, refactorizarlos e iniciar la subida cifrada.
- **Tienda JettraAppStore**: Clona la tienda de forma interna, analiza los plugins y expone un catálogo de tarjetas interactivas donde puedes instalar cualquier plugin en **un solo clic**, advirtiéndote además de conflictos de rutas y sugiriéndote cambios de nombre antes de integrarlos.
- **Gestión Local**: Visualiza reportes detallados de instalación (`plugin-result.md`) y remueve cualquier plugin devolviendo el proyecto base a su estado original (removiendo menús, dependencias del pom, handlers de rutas y borrando los paquetes físicos).
- **Locker de Credenciales**: Guarda tus tokens y usuarios de GitHub encriptados de forma segura mediante un formulario protegido con tu clave maestra AES.

### Ejecución Directa de la GUI
Para lanzar el panel de control gráfico, simplemente añade el parámetro `--gui` (o `-gui`) al invocar el JAR:

```bash
java -jar target/JettraPluginStore-1.0-SNAPSHOT.jar --gui
```

---

## 🛠️ Script de Automatización `build.sh`

Para agilizar el desarrollo, hemos diseñado el script de automatización `build.sh` en la raíz del proyecto. Este script realiza dos tareas clave en un único paso:
1. Limpia y compila el proyecto generando el fat ejecutable "shaded jar".
2. Inicia de manera inmediata el Panel de Control Gráfico de JavaFX.

Para ejecutarlo, simplemente corre:

```bash
chmod +x build.sh
./build.sh
```

---

## 🐳 Empaquetamiento y Distribución con Docker

Hemos creado un archivo `Dockerfile` optimizado en la raíz del proyecto para empaquetar de manera robusta la herramienta en una imagen Docker ligera y autónoma, incluyendo las dependencias nativas de gráficos GTK y X11 necesarias para inicializar JavaFX.

### Paso 1: Construcción de la Imagen Docker
Ejecuta la construcción en la raíz del proyecto donde se encuentra el `Dockerfile`:

```bash
docker build -t jettrapluginstore .
```

### Paso 2: Ejecución del Contenedor

#### A) Modo Shell Interactivo o CLI (Sin GUI)
Para ejecutar la herramienta en modo consola interactiva montando tu proyecto de trabajo actual (`$(pwd)`) en el directorio `/app` del contenedor:

```bash
docker run -it -v $(pwd):/app jettrapluginstore
```

#### B) Modo Panel de Control Gráfico (Con JavaFX GUI)
Para reenviar la interfaz gráfica del contenedor hacia el servidor de visualización X11 de tu máquina anfitriona (Linux):

1. **Permitir conexiones locales al servidor X11:**
   ```bash
   xhost +local:docker
   ```

2. **Ejecutar el contenedor con reenvío de pantalla y montando el volumen:**
   ```bash
   docker run -it \
     --net=host \
     -v /tmp/.X11-unix:/tmp/.X11-unix \
     -e DISPLAY=$DISPLAY \
     -v $(pwd):/app \
     jettrapluginstore --gui
   ```

3. **Restablecer la seguridad de tu servidor X11 una vez que cierres la aplicación:**
   ```bash
   xhost -local:docker
   ```

Gracias a esta configuración de Docker, cualquier desarrollador puede compilar, distribuir y utilizar la herramienta JettraPluginStore de manera consistente sin preocuparse de configurar variables de entorno o librerías nativas JavaFX localmente.