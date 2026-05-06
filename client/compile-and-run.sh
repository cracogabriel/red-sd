#!/bin/bash

PROTO_VERSION="3.21.12"
PROTO_JAR="protobuf-java-$PROTO_VERSION.jar"
OUT_DIR="bin"

# 1. Baixa o .jar do Protobuf se não existir
if [ ! -f "$PROTO_JAR" ]; then
    echo "Baixando a biblioteca Protobuf ($PROTO_JAR)..."
    wget -q "https://repo1.maven.org/maven2/com/google/protobuf/protobuf-java/$PROTO_VERSION/$PROTO_JAR"
    echo "Download concluído!"
fi

# 2. Limpa e recria a pasta de saída
echo "Limpando arquivos compilados antigos..."
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

echo "Compilando o projeto..."

# 3. Compila tudo: movies/ (proto), connection/, gui/, gui/dialogs/ e raiz
javac -d "$OUT_DIR" -cp "$PROTO_JAR" \
    movies/*.java \
    connection/*.java \
    gui/dialogs/*.java \
    gui/*.java \
    *.java

if [ $? -eq 0 ]; then
    echo "Compilação concluída com sucesso!"
    echo "Iniciando o MovieClient..."
    echo "---------------------------------------------------"
    java -cp "$OUT_DIR:$PROTO_JAR" MovieClient
else
    echo "==================================================="
    echo "Erro na compilação! Verifique as mensagens acima."
fi