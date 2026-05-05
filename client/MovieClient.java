import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import movies.MovieOuterClass;

class MovieClientGUI {
    private JFrame frame;
    private OutputStream output;
    private InputStream input;

    public MovieClientGUI(OutputStream output, InputStream input) {
        this.output = output;
        this.input = input;

        frame = new JFrame("mflix client");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null); 
        frame.setLayout(new BorderLayout());

        JLabel label = new JLabel("connected to server!", SwingConstants.CENTER);
        frame.add(label, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();

        JButton btnCreate = new JButton("add movie");
        JButton btnSearchGenre = new JButton("search by genre");
        
        buttonPanel.add(btnCreate);
        buttonPanel.add(btnSearchGenre);
        
        frame.add(buttonPanel, BorderLayout.SOUTH);

        btnCreate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAddMovieModal();
            }
        });

        btnSearchGenre.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showSearchGenreModal();
            }
        });
    }

    private void showSearchGenreModal() {
        JDialog dialog = new JDialog(frame, "search by genre", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(frame);
        dialog.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        JLabel genreLabel = new JLabel("genre:");
        JTextField genreField = new JTextField(15);
        JButton searchBtn = new JButton("search");

        topPanel.add(genreLabel);
        topPanel.add(genreField);
        topPanel.add(searchBtn);
        dialog.add(topPanel, BorderLayout.NORTH);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> resultList = new JList<>(listModel);
        dialog.add(new JScrollPane(resultList), BorderLayout.CENTER);

        searchBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String genre = genreField.getText().trim();

                if (genre.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "genre cannot be empty!", "validation error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                listModel.clear();
                listModel.addElement("searching...");

                fetchMoviesByGenre(genre, listModel);
            }
        });

        dialog.setVisible(true);
    }

    private void fetchMoviesByGenre(String genre, DefaultListModel<String> listModel) {
        try {
            System.out.println("searching by genre: " + genre);
            
            MovieOuterClass.GenreRequest genreReq = MovieOuterClass.GenreRequest.newBuilder()
                    .setGenre(genre)
                    .build();

            MovieOuterClass.Request request = MovieOuterClass.Request.newBuilder()
                    .setOperation(MovieOuterClass.Request.Operation.LIST_BY_GENRE)
                    .setByGenre(genreReq)
                    .build();

            output.write(request.toByteArray());
            output.flush();

            byte[] buffer = new byte[4096];
            int bytesRead = input.read(buffer);

            listModel.clear();

            if (bytesRead > 0) {
                byte[] responseData = new byte[bytesRead];
                System.arraycopy(buffer, 0, responseData, 0, bytesRead);
                
                MovieOuterClass.Response response = MovieOuterClass.Response.parseFrom(responseData);
                
                if (response.getSuccess()) {
                    System.out.println("success! received movies.");
                    
                    if (response.getMoviesCount() == 0) {
                        listModel.addElement("no movies found for this genre.");
                    } else {
                        // Popula a interface com os filmes recebidos
                        for (MovieOuterClass.Movie m : response.getMoviesList()) {
                            listModel.addElement(m.getTitle() + " (" + m.getYear() + ")");
                        }
                    }
                } else {
                    System.out.println("server error: " + response.getError());
                    listModel.addElement("server error: " + response.getError());
                }
            } else {
                listModel.addElement("no response from server.");
            }
        } catch (Exception ex) {
            System.err.println("error during search operation: " + ex.getMessage());
            listModel.addElement("connection error.");
        }
    }

    private void showAddMovieModal() {
        JDialog dialog = new JDialog(frame, "add new movie", true);
        dialog.setSize(300, 200);
        dialog.setLocationRelativeTo(frame);
        dialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        
        JLabel titleLabel = new JLabel(" title:");
        JTextField titleField = new JTextField();
        
        JLabel yearLabel = new JLabel(" year:");
        JTextField yearField = new JTextField();

        formPanel.add(titleLabel);
        formPanel.add(titleField);
        formPanel.add(yearLabel);
        formPanel.add(yearField);

        dialog.add(formPanel, BorderLayout.CENTER);

        JButton submitBtn = new JButton("submit");
        dialog.add(submitBtn, BorderLayout.SOUTH);

        submitBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String title = titleField.getText().trim();
                String yearStr = yearField.getText().trim();

                if (title.isEmpty() || yearStr.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "fields cannot be empty!", "validation error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int year;
                try {
                    year = Integer.parseInt(yearStr);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, "year must be a valid number!", "validation error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                boolean success = sendCreateRequest(title, year);
                
                if (success) {
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "server error! check the console.", "error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        dialog.setVisible(true);
    }

    private boolean sendCreateRequest(String title, int year) {
        try {
            System.out.println("creating movie data...");
            MovieOuterClass.Movie movie = MovieOuterClass.Movie.newBuilder()
                    .setTitle(title)
                    .setYear(year)
                    .build();

            MovieOuterClass.Request request = MovieOuterClass.Request.newBuilder()
                    .setOperation(MovieOuterClass.Request.Operation.CREATE)
                    .setMovie(movie)
                    .build();

            System.out.println("sending request...");
            output.write(request.toByteArray());
            output.flush();

            byte[] buffer = new byte[4096];
            int bytesRead = input.read(buffer);

            if (bytesRead > 0) {
                byte[] responseData = new byte[bytesRead];
                System.arraycopy(buffer, 0, responseData, 0, bytesRead);
                
                MovieOuterClass.Response response = MovieOuterClass.Response.parseFrom(responseData);
                if (response.getSuccess()) {
                    System.out.println("success! movie inserted with id: " + response.getMovie().getId());
                    return true;
                } else {
                    System.out.println("server error: " + response.getError());
                    return false;
                }
            }
        } catch (Exception ex) {
            System.err.println("error during create operation: " + ex.getMessage());
        }
        return false;
    }

    public void show() {
        frame.setVisible(true);
    }
}

public class MovieClient {
    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 5000;

        try {
            Socket socket = new Socket(host, port);
            System.out.println("connected to " + host + ":" + port);

            OutputStream output = socket.getOutputStream();
            InputStream input = socket.getInputStream();

            System.out.println("starting GUI...");
            MovieClientGUI gui = new MovieClientGUI(output, input);
            gui.show();

        } catch (Exception e) {
            System.err.println("error connecting to server: " + e.getMessage());
        }
    }
}