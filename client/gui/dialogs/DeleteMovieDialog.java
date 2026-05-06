package gui.dialogs;

import connection.ServerConnection;
import movies.MovieOuterClass;

import javax.swing.*;
import java.awt.*;

public class DeleteMovieDialog {

    private final JFrame           parent;
    private final ServerConnection conn;

    public DeleteMovieDialog(JFrame parent, ServerConnection conn) {
        this.parent = parent;
        this.conn   = conn;
    }

    public void show() {
        JDialog dialog = new JDialog(parent, "delete movie", true);
        dialog.setSize(300, 150);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel();
        JTextField idField = new JTextField(20);

        formPanel.add(new JLabel("movie id:"));
        formPanel.add(idField);
        dialog.add(formPanel, BorderLayout.CENTER);

        JButton deleteBtn = new JButton("delete");
        dialog.add(deleteBtn, BorderLayout.SOUTH);

        deleteBtn.addActionListener(e -> {
            String id = idField.getText().trim();

            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "id cannot be empty!", "validation error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(dialog,
                    "are you sure you want to delete movie " + id + "?",
                    "confirm delete",
                    JOptionPane.YES_NO_OPTION);

            if (confirm != JOptionPane.YES_OPTION) return;

            try {
                MovieOuterClass.Request request = MovieOuterClass.Request.newBuilder()
                        .setOperation(MovieOuterClass.Request.Operation.DELETE)
                        .setById(MovieOuterClass.MovieIdRequest.newBuilder().setId(id).build())
                        .build();

                MovieOuterClass.Response response = conn.send(request);

                if (response.getSuccess()) {
                    System.out.println("movie deleted: " + id);
                    JOptionPane.showMessageDialog(dialog, "movie deleted!", "success", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                } else {
                    System.out.println("server error: " + response.getError());
                    JOptionPane.showMessageDialog(dialog, "error: " + response.getError(), "error", JOptionPane.ERROR_MESSAGE);
                }

            } catch (Exception ex) {
                System.err.println("error during delete: " + ex.getMessage());
                JOptionPane.showMessageDialog(dialog, "connection error.", "error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setVisible(true);
    }
}