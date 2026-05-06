package gui.dialogs;

import connection.ServerConnection;
import movies.MovieOuterClass;

import javax.swing.*;
import java.awt.*;

public class AddMovieDialog {

    private final JFrame           parent;
    private final ServerConnection conn;

    public AddMovieDialog(JFrame parent, ServerConnection conn) {
        this.parent = parent;
        this.conn   = conn;
    }

    public void show() {
        JDialog dialog = new JDialog(parent, "add new movie", true);
        dialog.setSize(300, 200);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(2, 2, 10, 10));

        JTextField titleField = new JTextField();
        JTextField yearField  = new JTextField();

        formPanel.add(new JLabel(" title:"));
        formPanel.add(titleField);
        formPanel.add(new JLabel(" year:"));
        formPanel.add(yearField);

        dialog.add(formPanel, BorderLayout.CENTER);

        JButton submitBtn = new JButton("submit");
        dialog.add(submitBtn, BorderLayout.SOUTH);

        submitBtn.addActionListener(e -> {
            String title  = titleField.getText().trim();
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

            boolean success = sendCreateRequest(dialog, title, year);
            if (success) dialog.dispose();
            else JOptionPane.showMessageDialog(dialog, "server error! check the console.", "error", JOptionPane.ERROR_MESSAGE);
        });

        dialog.setVisible(true);
    }

    private boolean sendCreateRequest(JDialog dialog, String title, int year) {
        try {
            System.out.println("creating movie: " + title + " (" + year + ")");

            MovieOuterClass.Request request = MovieOuterClass.Request.newBuilder()
                    .setOperation(MovieOuterClass.Request.Operation.CREATE)
                    .setMovie(MovieOuterClass.Movie.newBuilder()
                            .setTitle(title)
                            .setYear(year)
                            .build())
                    .build();

            MovieOuterClass.Response response = conn.send(request);

            if (response.getSuccess()) {
                System.out.println("success! movie inserted with id: " + response.getMovie().getId());
                return true;
            } else {
                System.out.println("server error: " + response.getError());
                return false;
            }
        } catch (Exception ex) {
            System.err.println("error during create: " + ex.getMessage());
            return false;
        }
    }
}