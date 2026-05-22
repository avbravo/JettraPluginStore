#!/bin/bash

echo "========================================="
echo "Compilando JettraPluginStore..."
echo "========================================="

# Navegar a la ruta del proyecto por si se ejecuta desde otro directorio
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"

# Ejecutar maven para limpiar y empaquetar
mvn clean package

if [ $? -eq 0 ]; then
    echo "========================================="
    echo "✅ Construcción exitosa."
    echo "Iniciando la interfaz gráfica futurista de JavaFX..."
    echo "========================================="
    java -jar target/JettraPluginStore-1.0-SNAPSHOT.jar --gui
else
    echo "========================================="
    echo "❌ Error durante la compilación."
    echo "========================================="
    exit 1
fi
