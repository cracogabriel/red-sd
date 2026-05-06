package gui.dialogs;

import connection.ServerConnection;
import movies.MovieOuterClass;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class GetMovieDialog {

    private final JFrame           parent;
    private final ServerConnection conn;

    public GetMovieDialog(JFrame parent, ServerConnection conn) {
        this.parent = parent;
        this.conn   = conn;
    }

    public void show() {
        JDialog dialog = new JDialog(parent, "get movie", true);
        dialog.setSize(650, 400);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        JTextField idField  = new JTextField(24);
        JButton    searchBtn = new JButton("search");

        topPanel.add(new JLabel("movie id:"));
        topPanel.add(idField);
        topPanel.add(searchBtn);
        dialog.add(topPanel, BorderLayout.NORTH);

        String[] columns = { "field", "value" };
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(1).setPreferredWidth(280);
        dialog.add(new JScrollPane(table), BorderLayout.CENTER);

        searchBtn.addActionListener(e -> {
            String id = idField.getText().trim();

            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "id cannot be empty!", "validation error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            fetchMovie(id, tableModel);
        });

        dialog.setVisible(true);
    }

    private void fetchMovie(String id, DefaultTableModel tableModel) {
        try {
            System.out.println("fetching movie with id: " + id);

            MovieOuterClass.Request request = MovieOuterClass.Request.newBuilder()
                    .setOperation(MovieOuterClass.Request.Operation.GET)
                    .setById(MovieOuterClass.MovieIdRequest.newBuilder().setId(id).build())
                    .build();

            MovieOuterClass.Response response = conn.send(request);

            tableModel.setRowCount(0); 

            if (response.getSuccess()) {
                MovieOuterClass.Movie m = response.getMovie();
                tableModel.addRow(new Object[]{ "id",         m.getId() });
                tableModel.addRow(new Object[]{ "title",      m.getTitle() });
                tableModel.addRow(new Object[]{ "year",       m.getYear() });
                tableModel.addRow(new Object[]{ "plot",       m.getPlot() });
                tableModel.addRow(new Object[]{ "rated",      m.getRated() });
                tableModel.addRow(new Object[]{ "runtime",    m.getRuntime() + " min" });
                tableModel.addRow(new Object[]{ "genres",     String.join(", ", m.getGenresList()) });
                tableModel.addRow(new Object[]{ "cast",       String.join(", ", m.getCastList()) });
                tableModel.addRow(new Object[]{ "directors",  String.join(", ", m.getDirectorsList()) });
                tableModel.addRow(new Object[]{ "countries",  String.join(", ", m.getCountriesList()) });
                tableModel.addRow(new Object[]{ "languages",  String.join(", ", m.getLanguagesList()) });
                tableModel.addRow(new Object[]{ "type",       m.getType() });
                tableModel.addRow(new Object[]{ "released",   m.getReleased() });
            } else {
                System.out.println("server error: " + response.getError());
                JOptionPane.showMessageDialog(null, "error: " + response.getError(), "error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            System.err.println("error during get: " + ex.getMessage());
            JOptionPane.showMessageDialog(null, "connection error.", "error", JOptionPane.ERROR_MESSAGE);
        }
    }
}