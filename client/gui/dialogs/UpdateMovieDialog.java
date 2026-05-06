package gui.dialogs;

import connection.ServerConnection;
import movies.MovieOuterClass;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class UpdateMovieDialog {

    private final JFrame           parent;
    private final ServerConnection conn;

    public UpdateMovieDialog(JFrame parent, ServerConnection conn) {
        this.parent = parent;
        this.conn   = conn;
    }

    public void show() {
        JDialog dialog = new JDialog(parent, "update movie", true);
        dialog.setSize(650, 400);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        JTextField idField  = new JTextField(20);
        JButton    loadBtn   = new JButton("load");
        JButton    updateBtn = new JButton("update");
        updateBtn.setEnabled(false);
        
        topPanel.add(new JLabel("movie id:"));
        topPanel.add(idField);
        topPanel.add(loadBtn);
        topPanel.add(updateBtn);
        dialog.add(topPanel, BorderLayout.NORTH);

        String[] columns = { "field", "value" };
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1; 
            }
        };
        JTable table = new JTable(tableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(1).setPreferredWidth(280);
        dialog.add(new JScrollPane(table), BorderLayout.CENTER);

        loadBtn.addActionListener(e -> {
            String id = idField.getText().trim();

            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "id cannot be empty!", "validation error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            boolean loaded = loadMovie(id, tableModel);
            updateBtn.setEnabled(loaded);
        });

        updateBtn.addActionListener(e -> {
            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }

            boolean success = sendUpdateRequest(idField.getText().trim(), tableModel);
            if (success) dialog.dispose();
        });

        dialog.setVisible(true);
    }

    private boolean loadMovie(String id, DefaultTableModel tableModel) {
        try {
            System.out.println("loading movie with id: " + id);

            MovieOuterClass.Request request = MovieOuterClass.Request.newBuilder()
                    .setOperation(MovieOuterClass.Request.Operation.GET)
                    .setById(MovieOuterClass.MovieIdRequest.newBuilder().setId(id).build())
                    .build();

            MovieOuterClass.Response response = conn.send(request);

            tableModel.setRowCount(0);

            if (response.getSuccess()) {
                MovieOuterClass.Movie m = response.getMovie();
                tableModel.addRow(new Object[]{ "title",      m.getTitle() });
                tableModel.addRow(new Object[]{ "year",       m.getYear() });
                tableModel.addRow(new Object[]{ "plot",       m.getPlot() });
                tableModel.addRow(new Object[]{ "rated",      m.getRated() });
                tableModel.addRow(new Object[]{ "runtime",    m.getRuntime() });
                tableModel.addRow(new Object[]{ "genres",     String.join(", ", m.getGenresList()) });
                tableModel.addRow(new Object[]{ "cast",       String.join(", ", m.getCastList()) });
                tableModel.addRow(new Object[]{ "directors",  String.join(", ", m.getDirectorsList()) });
                tableModel.addRow(new Object[]{ "countries",  String.join(", ", m.getCountriesList()) });
                tableModel.addRow(new Object[]{ "languages",  String.join(", ", m.getLanguagesList()) });
                tableModel.addRow(new Object[]{ "type",       m.getType() });
                tableModel.addRow(new Object[]{ "released",   m.getReleased() });
                return true;
            } else {
                JOptionPane.showMessageDialog(null, "error: " + response.getError(), "error", JOptionPane.ERROR_MESSAGE);
                return false;
            }

        } catch (Exception ex) {
            System.err.println("error loading movie: " + ex.getMessage());
            JOptionPane.showMessageDialog(null, "connection error.", "error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private boolean sendUpdateRequest(String id, DefaultTableModel tableModel) {
        try {
            MovieOuterClass.Movie.Builder movieBuilder = MovieOuterClass.Movie.newBuilder().setId(id);

            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String field = (String) tableModel.getValueAt(i, 0);
                String value = tableModel.getValueAt(i, 1).toString();

                switch (field) {
                    case "title"     -> movieBuilder.setTitle(value);
                    case "year"      -> movieBuilder.setYear(Integer.parseInt(value));
                    case "plot"      -> movieBuilder.setPlot(value);
                    case "rated"     -> movieBuilder.setRated(value);
                    case "runtime"   -> movieBuilder.setRuntime(Integer.parseInt(value));
                    case "genres"    -> { movieBuilder.clearGenres();    for (String s : value.split(",")) movieBuilder.addGenres(s.trim()); }
                    case "cast"      -> { movieBuilder.clearCast();      for (String s : value.split(",")) movieBuilder.addCast(s.trim()); }
                    case "directors" -> { movieBuilder.clearDirectors(); for (String s : value.split(",")) movieBuilder.addDirectors(s.trim()); }
                    case "countries" -> { movieBuilder.clearCountries(); for (String s : value.split(",")) movieBuilder.addCountries(s.trim()); }
                    case "languages" -> { movieBuilder.clearLanguages(); for (String s : value.split(",")) movieBuilder.addLanguages(s.trim()); }
                    case "type"      -> movieBuilder.setType(value);
                    case "released"  -> movieBuilder.setReleased(value);
                }
            }

            MovieOuterClass.Request request = MovieOuterClass.Request.newBuilder()
                    .setOperation(MovieOuterClass.Request.Operation.UPDATE)
                    .setMovie(movieBuilder.build())
                    .build();

            MovieOuterClass.Response response = conn.send(request);

            if (response.getSuccess()) {
                System.out.println("movie updated successfully: " + id);
                JOptionPane.showMessageDialog(null, "movie updated!", "success", JOptionPane.INFORMATION_MESSAGE);
                return true;
            } else {
                System.out.println("server error: " + response.getError());
                JOptionPane.showMessageDialog(null, "error: " + response.getError(), "error", JOptionPane.ERROR_MESSAGE);
                return false;
            }

        } catch (Exception ex) {
            System.err.println("error during update: " + ex.getMessage());
            JOptionPane.showMessageDialog(null, "connection error.", "error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}