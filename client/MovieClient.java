import connection.ServerConnection;
import gui.MainWindow;

public class MovieClient {
    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 5000;

        try {
            ServerConnection conn = new ServerConnection(host, port);
            System.out.println("connected to " + host + ":" + port);

            System.out.println("starting GUI...");
            MainWindow window = new MainWindow(conn);
            window.show();

        } catch (Exception e) {
            System.err.println("error connecting to server: " + e.getMessage());
        }
    }
}