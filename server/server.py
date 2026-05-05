import os
import socket
import threading
import movie_pb2

from bson import ObjectId
from dotenv import load_dotenv
from pymongo import MongoClient
from urllib.parse import quote_plus

# MongoDB connection
def connect_db():
    print("connecting to MongoDB...")
    load_dotenv()
    password = quote_plus(os.getenv("DB_PASSWORD"))
    client = MongoClient(f"mongodb+srv://sd-red-mongodb:{password}@cluster0.nyepcaz.mongodb.net/?appName=Cluster0")
    db = client["sample_mflix"]
    return db["movies"]

# Socket connection
HOST = '0.0.0.0'
PORT = 5000
 
def start_server(movies):
    print("starting TCP server...")
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((HOST, PORT))
    server.listen()
 
    print("")
    print(f"listening on {HOST}:{PORT}")
    print("")
    while True:
        conn, addr = server.accept()
        print(f"new connection from {addr}. thread created")
        thread = threading.Thread(target=handle_client, args=(conn, addr, movies))
        thread.daemon = True
        thread.start()

# Helpers
# retorna mensagem de erro se campos obrigatórios estiverem vazios
def validate_movie(movie: movie_pb2.Movie) -> str | None:
    if not movie.title.strip():
        return "field 'title' is required"
    if not movie.year:
        return "field 'year' is required"
    return None

def safe_parse_year(val):
    try:
        return int(val)
    except (ValueError, TypeError):
        cleaned = ''.join(filter(str.isdigit, str(val)))
        return int(cleaned) if cleaned else 0

# Converte um documento MongoDB
def doc_to_movie(doc):
    if doc is None:
        return None
    return movie_pb2.Movie(
        id              = str(doc.get("_id", "")),
        title           = doc.get("title", ""),
        plot            = doc.get("plot", ""),
        fullplot        = doc.get("fullplot", ""),
        genres          = doc.get("genres", []),
        cast            = doc.get("cast", []),
        directors       = doc.get("directors", []),
        countries       = doc.get("countries", []),
        languages       = doc.get("languages", []),
        released        = str(doc.get("released", "")),
        runtime         = doc.get("runtime", 0),
        rated           = doc.get("rated", ""),
        year            = safe_parse_year(doc.get("year", 0)),
        type            = doc.get("type", ""),
        poster          = doc.get("poster", ""),
        num_mflix_comments = doc.get("num_mflix_comments", 0),
        lastupdated     = doc.get("lastupdated", ""),
    )

# Converte um Movie protobuf para um documento MongoDB
def movie_to_doc(movie: movie_pb2.Movie) :
    return {
        "title":             movie.title,
        "plot":              movie.plot,
        "fullplot":          movie.fullplot,
        "genres":            list(movie.genres),
        "cast":              list(movie.cast),
        "directors":         list(movie.directors),
        "countries":         list(movie.countries),
        "languages":         list(movie.languages),
        "released":          movie.released,
        "runtime":           movie.runtime,
        "rated":             movie.rated,
        "year":              movie.year,
        "type":              movie.type,
        "poster":            movie.poster,
        "num_mflix_comments": movie.num_mflix_comments,
        "lastupdated":       movie.lastupdated,
    }

def build_error(msg: str) -> movie_pb2.Response:
    return movie_pb2.Response(success=False, error=msg)

# Client handler
def handle_op_create(request, movies):
    print("handling CREATE operation")
    try:
        err = validate_movie(request.movie)
        if err:
            return build_error(err)

        doc = movie_to_doc(request.movie)
        result = movies.insert_one(doc)
        created = movies.find_one({"_id": result.inserted_id})

        return movie_pb2.Response(success=True, movie=doc_to_movie(created))

    except Exception as e:
        return build_error(f"CREATE error: {e}")

def handle_op_get(request, movies):
    print("handling GET operation")
    try:
        oid = ObjectId(request.by_id.id)
        doc = movies.find_one({"_id": oid})

        if doc is None:
            return build_error("movie not found")

        return movie_pb2.Response(success=True, movie=doc_to_movie(doc))

    except Exception as e:
        return build_error(f"GET error: {e}")

def handle_op_update(request, movies):
    print("handling UPDATE operation")
    try:
        if not request.movie.id:
            return build_error("field 'id' is required for update")
        
        err = validate_movie(request.movie)
        if err:
            return build_error(err)

        oid = ObjectId(request.movie.id)

        # $set atualiza apenas os campos enviados, sem sobrescrever o documento inteiro
        update_fields = movie_to_doc(request.movie)
        result = movies.update_one({"_id": oid}, {"$set": update_fields})

        if result.matched_count == 0:
            return build_error("movie not found")

        updated = movies.find_one({"_id": oid})
        return movie_pb2.Response(success=True, movie=doc_to_movie(updated))

    except Exception as e:
        return build_error(f"UPDATE error: {e}")
    
def handle_op_delete(request, movies):
    print("handling DELETE operation")
    try:
        oid = ObjectId(request.by_id.id)
        result = movies.delete_one({"_id": oid})

        if result.deleted_count is None or result.deleted_count == 0:
            return build_error("movie not found")

        return movie_pb2.Response(success=True, message="movie deleted successfully")

    except Exception as e:
        return build_error(f"GET error: {e}")
    
def handle_op_list_by_actor(request, movies):
    print("handling LIST_BY_ACTOR operation")
    try:
        response = movies.find({"cast": request.by_actor.actor}).limit(4)
        movie_list = [doc_to_movie(doc) for doc in response]

        if not movie_list:
            return build_error("no movies found for this actor")

        return movie_pb2.Response(success=True, movies=movie_list)

    except Exception as e:
        return build_error(f"LIST_BY_ACTOR error: {e}")


def handle_op_list_by_genre(request, movies):
    print("handling LIST_BY_GENRE operation")
    try:
        response = movies.find({"genres": request.by_genre.genre}).limit(4)
        movie_list = [doc_to_movie(doc) for doc in response]

        if not movie_list:
            return build_error("no movies found for this genre")

        return movie_pb2.Response(success=True, movies=movie_list)

    except Exception as e:
        return build_error(f"LIST_BY_GENRE error: {e}")

def handle_client(conn, addr, movies):
    try:
        while True:
            data = conn.recv(4096) 
            if not data:
                print(f"client {addr} disconnected. returning...")
                break
            
            print(f"received request from {addr}")

            request = movie_pb2.Request()
            request.ParseFromString(data)

            match request.operation:
                case movie_pb2.Request.Operation.CREATE:
                    response = handle_op_create(request, movies)
                case movie_pb2.Request.Operation.GET:
                    response = handle_op_get(request, movies)
                case movie_pb2.Request.Operation.UPDATE:
                    response = handle_op_update(request, movies)
                case movie_pb2.Request.Operation.DELETE:
                    response = handle_op_delete(request, movies)
                case movie_pb2.Request.Operation.LIST_BY_ACTOR:
                    response = handle_op_list_by_actor(request, movies)
                case movie_pb2.Request.Operation.LIST_BY_GENRE:
                    response = handle_op_list_by_genre(request, movies)
                case _:
                    response = build_error(f"unknown operation: {request.operation}")

            conn.sendall(response.SerializeToString())

    except Exception as e:
        print(f"error handling client {addr}: {e}")
    finally:
        conn.close()
        print(f"connection closed for {addr}")
        print("")

def main():
    print('starting server...')
    movies = connect_db()
    print(f"connected with MongoDB! total moovies: {movies.count_documents({})}")
    start_server(movies)

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print('\nserver shutting down.')