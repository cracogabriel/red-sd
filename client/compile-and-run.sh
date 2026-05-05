#!/bin/bash

# Define a versão do Protobuf que vamos usar
PROTO_VERSION="3.21.12"
PROTO_JAR="protobuf-java-$PROTO_VERSION.jar"
# Pasta onde os arquivos compilados (.class) serão guardados
OUT_DIR="bin"

# 1. Verifica se o .jar da biblioteca Protobuf já foi baixado. Se não, baixa.
if [ ! -f "$PROTO_JAR" ]; then
    echo "Baixando a biblioteca Protobuf ($PROTO_JAR)..."
    wget -q "https://repo1.maven.org/maven2/com/google/protobuf/protobuf-java/$PROTO_VERSION/$PROTO_JAR"
    echo "Download concluído!"
fi

# 2. Limpa a compilação anterior e cria a pasta de novo
echo "Limpando arquivos compilados antigos..."
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

echo "Compilando o projeto..."

# 3. Compila o projeto jogando os .class para dentro da pasta bin/ (flag -d)
javac -d "$OUT_DIR" -cp "$PROTO_JAR" movies/*.java *.java

# Verifica se a compilação deu certo
if [ $? -eq 0 ]; then
    echo "Compilação concluída com sucesso!"
    echo "Iniciando o MovieClient..."
    echo "---------------------------------------------------"
    
    # 4. Roda o cliente apontando o classpath para a pasta bin/ e o jar do Protobuf
    java -cp "$OUT_DIR:$PROTO_JAR" MovieClient
else
    echo "==================================================="
    echo "Erro na compilação! Verifique as mensagens acima."
fi