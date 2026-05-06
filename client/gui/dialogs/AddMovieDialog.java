package gui.dialogs;

import connection.ServerConnection;
import movies.MovieOuterClass;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AddMovieDialog {

    private final JFrame           parent;
    private final ServerConnection conn;

    public AddMovieDialog(JFrame parent, ServerConnection conn) {
        this.parent = parent;
        this.conn   = conn;
    }

    public void show() {
        JDialog dialog = new JDialog(parent, "add new movie", true);
        dialog.setSize(520, 480);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout(0, 0));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(16, 20, 8, 20));
        GridBagConstraints c = new GridBagConstraints();
        c.fill    = GridBagConstraints.HORIZONTAL;
        c.insets  = new Insets(5, 4, 5, 4);
        c.weightx = 0;

        JTextField titleField     = new JTextField();
        JTextField yearField      = new JTextField();
        JTextField runtimeField   = new JTextField();
        JTextField ratedField     = new JTextField();
        JTextField typeField      = new JTextField("movie");
        JTextField genresField    = new JTextField();
        JTextField castField      = new JTextField();
        JTextField directorsField = new JTextField();
        JTextArea  plotArea       = new JTextArea(3, 20);
        plotArea.setLineWrap(true);
        plotArea.setWrapStyleWord(true);

        c.gridx = 0; c.gridy = 0; c.weightx = 0;
        form.add(labelRequired("title:"), c);
        c.gridx = 1; c.weightx = 1.0; c.gridwidth = 3;
        form.add(titleField, c);

        c.gridx = 0; c.gridy = 1; c.weightx = 0; c.gridwidth = 1;
        form.add(labelRequired("year:"), c);
        c.gridx = 1; c.weightx = 0.3;
        form.add(yearField, c);
        c.gridx = 2; c.weightx = 0;
        form.add(new JLabel("runtime (min):"), c);
        c.gridx = 3; c.weightx = 0.3;
        form.add(runtimeField, c);

        c.gridx = 0; c.gridy = 2; c.weightx = 0; c.gridwidth = 1;
        form.add(new JLabel("rated:"), c);
        c.gridx = 1; c.weightx = 0.3;
        form.add(ratedField, c);
        c.gridx = 2; c.weightx = 0;
        form.add(new JLabel("type:"), c);
        c.gridx = 3; c.weightx = 0.3;
        form.add(typeField, c);

        c.gridx = 0; c.gridy = 3; c.weightx = 0; c.gridwidth = 1;
        form.add(new JLabel("genres:"), c);
        c.gridx = 1; c.weightx = 1.0; c.gridwidth = 3;
        form.add(genresField, c);

        c.gridx = 0; c.gridy = 4; c.weightx = 0; c.gridwidth = 1;
        form.add(new JLabel("cast:"), c);
        c.gridx = 1; c.weightx = 1.0; c.gridwidth = 3;
        form.add(castField, c);

        c.gridx = 0; c.gridy = 5; c.weightx = 0; c.gridwidth = 1;
        form.add(new JLabel("directors:"), c);
        c.gridx = 1; c.weightx = 1.0; c.gridwidth = 3;
        form.add(directorsField, c);

        c.gridx = 0; c.gridy = 6; c.weightx = 0; c.gridwidth = 1;
        c.anchor = GridBagConstraints.NORTH;
        form.add(new JLabel("plot:"), c);
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 1; c.weightx = 1.0; c.gridwidth = 3;
        form.add(new JScrollPane(plotArea), c);

        c.gridx = 0; c.gridy = 7; c.gridwidth = 4; c.weightx = 1.0;
        JLabel hint = new JLabel("  * required   —  separate multiple values with commas");
        hint.setForeground(Color.GRAY);
        hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 11f));
        form.add(hint, c);

        dialog.add(form, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setBorder(new EmptyBorder(0, 12, 12, 12));
        JButton submitBtn = new JButton("submit");
        submitBtn.setPreferredSize(new Dimension(100, 30));
        south.add(submitBtn);
        dialog.add(south, BorderLayout.SOUTH);

        submitBtn.addActionListener(e -> {
            String title   = titleField.getText().trim();
            String yearStr = yearField.getText().trim();

            if (title.isEmpty() || yearStr.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "title and year are required!", "validation error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int year, runtime;
            try {
                year = Integer.parseInt(yearStr);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "year must be a valid number!", "validation error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                String rt = runtimeField.getText().trim();
                runtime = rt.isEmpty() ? 0 : Integer.parseInt(rt);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "runtime must be a valid number!", "validation error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            boolean success = sendCreateRequest(
                    dialog, title, year, runtime,
                    ratedField.getText().trim(),
                    typeField.getText().trim(),
                    plotArea.getText().trim(),
                    splitList(genresField.getText()),
                    splitList(castField.getText()),
                    splitList(directorsField.getText())
            );

            if (success) dialog.dispose();
            else JOptionPane.showMessageDialog(dialog, "server error! check the console.", "error", JOptionPane.ERROR_MESSAGE);
        });

        dialog.setVisible(true);
    }

    private JLabel labelRequired(String text) {
        return new JLabel("<html>" + text + " <font color='red'>*</font></html>");
    }

    private List<String> splitList(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private boolean sendCreateRequest(JDialog dialog, String title, int year, int runtime,
                                      String rated, String type, String plot,
                                      List<String> genres, List<String> cast, List<String> directors) {
        try {
            System.out.println("creating movie: " + title + " (" + year + ")");

            MovieOuterClass.Movie movie = MovieOuterClass.Movie.newBuilder()
                    .setTitle(title)
                    .setYear(year)
                    .setRuntime(runtime)
                    .setRated(rated)
                    .setType(type.isEmpty() ? "movie" : type)
                    .setPlot(plot)
                    .addAllGenres(genres)
                    .addAllCast(cast)
                    .addAllDirectors(directors)
                    .build();

            MovieOuterClass.Request request = MovieOuterClass.Request.newBuilder()
                    .setOperation(MovieOuterClass.Request.Operation.CREATE)
                    .setMovie(movie)
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