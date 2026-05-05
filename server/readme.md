# Servidor de Filmes - Mflix

Gerenciamento de filmes usando MongoDB Atlas + Protocol Buffers + TCP.

---

## Setup

1. Crie a virtual environment e ativa ela:

```bash
python3 -m venv venv
source venv/bin/activate
```

2. Instale as dependências:

```bash
pip install pymongo python-dotenv protobuf
```

3. Crie o arquivo `.env` na raiz do projeto com a senha do banco:

```
DB_PASSWORD=sua_senha_aqui
```

4. Instale o compilador do protobuf:

```bash
sudo apt install -y protobuf-compiler
```

5. Gere o código Python a partir do `movie.proto`:

```bash
protoc --proto_path=. --python_out=. movie.proto
```

Isso vai gerar o arquivo `movie_pb2.py` que é usado pelo servidor.

6. Rode o servidor:

```bash
python main.py
```
