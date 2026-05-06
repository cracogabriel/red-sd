package connection;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import movies.MovieOuterClass;

public class ServerConnection {

    private final String       host;
    private final int          port; 
    private final Socket       socket;
    private final OutputStream output;
    private final InputStream  input;

    public ServerConnection(String host, int port) throws Exception {
        this.host   = host;           
        this.port   = port;           
        this.socket = new Socket(host, port);
        this.output = socket.getOutputStream();
        this.input  = socket.getInputStream();
    }

    public String getHost() { return host; }  
    public int    getPort() { return port; } 

    /**
     * Envia um Request serializado e retorna o Response deserializado.
     * Toda a lógica de bytes fica aqui — os dialogs só lidam com objetos proto.
     */
    public MovieOuterClass.Response send(MovieOuterClass.Request request) throws Exception {
        output.write(request.toByteArray());
        output.flush();

        byte[] buffer    = new byte[65536];
        int    bytesRead = input.read(buffer);

        if (bytesRead <= 0) {
            throw new Exception("no response from server");
        }

        byte[] responseData = new byte[bytesRead];
        System.arraycopy(buffer, 0, responseData, 0, bytesRead);
        return MovieOuterClass.Response.parseFrom(responseData);
    }

    public void close() {
        try { 
            socket.close(); 
        } catch (Exception ignored) {
            
        }
    }
}