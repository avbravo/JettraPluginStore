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
    echo "El archivo ejecutable JAR se encuentra en:"
    echo "target/JettraPluginStore-1.0-SNAPSHOT-shaded.jar"
    echo "========================================="
else
    echo "========================================="
    echo "❌ Error durante la compilación."
    echo "========================================="
    exit 1
fi
