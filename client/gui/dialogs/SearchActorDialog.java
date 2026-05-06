package gui.dialogs;

import connection.ServerConnection;
import movies.MovieOuterClass;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class SearchActorDialog {

    private final JFrame           parent;
    private final ServerConnection conn;

    public SearchActorDialog(JFrame parent, ServerConnection conn) {
        this.parent = parent;
        this.conn   = conn;
    }

    public void show() {
        JDialog dialog = new JDialog(parent, "search by actor", true);
        dialog.setSize(850, 600);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        JTextField actorField = new JTextField(15);
        JButton    searchBtn  = new JButton("search");

        topPanel.add(new JLabel("actor:"));
        topPanel.add(actorField);
        topPanel.add(searchBtn);
        dialog.add(topPanel, BorderLayout.NORTH);

        // Configuração da JTable
        String[] columns = { "id", "title", "year", "runtime", "rated", "genres" };
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Desativa a edição das células
            }
        };
        JTable table = new JTable(tableModel);
        
        // Ajusta a largura das colunas
        table.getColumnModel().getColumn(0).setPreferredWidth(180); // id
        table.getColumnModel().getColumn(1).setPreferredWidth(200); // title
        table.getColumnModel().getColumn(2).setPreferredWidth(50);  // year
        table.getColumnModel().getColumn(3).setPreferredWidth(60);  // runtime
        table.getColumnModel().getColumn(4).setPreferredWidth(50);  // rated
        table.getColumnModel().getColumn(5).setPreferredWidth(150); // genres
        
        dialog.add(new JScrollPane(table), BorderLayout.CENTER);

        searchBtn.addActionListener(e -> {
            String actor = actorField.getText().trim();

            if (actor.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "actor cannot be empty!", "validation error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            tableModel.setRowCount(0);
            tableModel.addRow(new Object[]{"searching...", "", "", "", "", ""});
            fetchMoviesByActor(actor, tableModel);
        });

        dialog.setVisible(true);
    }

    private void fetchMoviesByActor(String actor, DefaultTableModel tableModel) {
        try {
            System.out.println("searching by actor: " + actor);

            MovieOuterClass.Request request = MovieOuterClass.Request.newBuilder()
                    .setOperation(MovieOuterClass.Request.Operation.LIST_BY_ACTOR)
                    .setByActor(MovieOuterClass.ActorRequest.newBuilder().setActor(actor).build())
                    .build();

            MovieOuterClass.Response response = conn.send(request);

            tableModel.setRowCount(0); // Limpa a tabela antes de popular

            if (response.getSuccess()) {
                    if (response.getMoviesCount() == 0) {
                        tableModel.addRow(new Object[]{"no movies found.", "", "", "", "", ""});
                    } else {
                        for (MovieOuterClass.Movie m : response.getMoviesList()) {
                            tableModel.addRow(new Object[]{ 
                                m.getId(), 
                                m.getTitle(), 
                                m.getYear(),
                                m.getRuntime() > 0 ? m.getRuntime() + " min" : "n/a",
                                m.getRated().isEmpty() ? "unrated" : m.getRated(),
                                String.join(", ", m.getGenresList())
                            });
                        }
                    }
                } else {
                    System.out.println("server error: " + response.getError());
                    tableModel.addRow(new Object[]{"server error: " + response.getError(), "", "", "", "", ""});
                }

            } catch (Exception ex) {
                System.err.println("error during search: " + ex.getMessage());
                tableModel.setRowCount(0);
                tableModel.addRow(new Object[]{"connection error.", "", "", "", "", ""});
            }
    }
}