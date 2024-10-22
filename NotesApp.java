import java.awt.*;
import java.io.*;
import javax.swing.*;

public class NotesApp extends JFrame {
    private JTextField titleField;      // Field for the note title (file name)
    private JTextArea textArea;         // Field for writing the note content
    private JFileChooser fileChooser;
    private DefaultListModel<String> notesListModel;
    private JList<String> notesList;
    private File notesDirectory;
    private static final String CONFIG_FILE = System.getProperty("user.home") + File.separator + "notes_config.txt";

    public NotesApp() {
        setTitle("Notes Application");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Initialize the file chooser before it's used
        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        // Set Nimbus Look and Feel (a modern UI theme)
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize the notes list model to avoid NullPointerException
        notesListModel = new DefaultListModel<>();
        notesList = new JList<>(notesListModel);

        // Check if path is saved, otherwise ask the user to set the save path
        loadOrAskSavePath();

        // Main layout
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Create title field for the note (file name)
        titleField = new JTextField();
        titleField.setBorder(BorderFactory.createTitledBorder("Note Title (Filename)"));
        mainPanel.add(titleField, BorderLayout.NORTH);

        // Create text area for writing notes
        textArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Create toolbar with buttons for Save, New, Delete, and Change Path
        JToolBar toolBar = new JToolBar();
        JButton newNoteButton = new JButton("New");
        newNoteButton.addActionListener(e -> newNoteAction());

        JButton saveNoteButton = new JButton("Save");
        saveNoteButton.addActionListener(e -> saveNoteAction());

        JButton deleteNoteButton = new JButton("Delete");
        deleteNoteButton.addActionListener(e -> deleteNoteAction());

        JButton changePathButton = new JButton("Change Path");
        changePathButton.addActionListener(e -> setSavePath());

        toolBar.add(newNoteButton);
        toolBar.add(saveNoteButton);
        toolBar.add(deleteNoteButton);
        toolBar.add(changePathButton);
        mainPanel.add(toolBar, BorderLayout.SOUTH);

        // Create saved notes list
        notesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        notesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedIndex = notesList.getSelectedIndex();
                if (selectedIndex != -1) {
                    openNoteFromList(new File(notesDirectory, notesListModel.get(selectedIndex)));
                }
            }
        });

        // Panel for saved notes list
        JPanel notesListPanel = new JPanel(new BorderLayout());
        notesListPanel.setBorder(BorderFactory.createTitledBorder("Saved Notes"));
        JScrollPane listScrollPane = new JScrollPane(notesList);
        notesListPanel.add(listScrollPane, BorderLayout.CENTER);

        // Add notes list panel to the main panel
        mainPanel.add(notesListPanel, BorderLayout.WEST);

        // Add the main panel to the frame
        add(mainPanel);

        // Load saved notes
        loadSavedNotes();
    }

    // Method to load the saved path or prompt the user to select it
    private void loadOrAskSavePath() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                String path = reader.readLine();
                notesDirectory = new File(path);
                
                if (!notesDirectory.exists()) {
                    JOptionPane.showMessageDialog(this, "Saved path doesn't exist. Please choose a new path.");
                    setSavePath();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            setSavePath(); // Prompt user to choose path if config file doesn't exist
        }
    }

    // Method to set the save path and save it in the config file
    private void setSavePath() {
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            notesDirectory = fileChooser.getSelectedFile();
            saveConfig(notesDirectory.getAbsolutePath());
            loadSavedNotes();
        } else {
            JOptionPane.showMessageDialog(this, "No path selected. The application will close.");
            System.exit(0);
        }
    }

    // Method to save the config file with the selected path
    private void saveConfig(String path) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CONFIG_FILE))) {
            writer.write(path);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving config file: " + e.getMessage());
        }
    }

    // Method to load saved notes from the directory
    private void loadSavedNotes() {
        notesListModel.clear();
        if (notesDirectory != null && notesDirectory.exists()) {
            File[] files = notesDirectory.listFiles((dir, name) -> name.endsWith(".txt"));
            if (files != null) {
                for (File file : files) {
                    notesListModel.addElement(file.getName());
                }
            }
        }
    }

    // Method to check if notes directory exists before performing actions
    private boolean checkDirectoryExists() {
        if (!notesDirectory.exists()) {
            JOptionPane.showMessageDialog(this, "The save path has been deleted. Please choose a new path.");
            setSavePath();
            return false;
        }
        return true;
    }

    // Action method for saving a note
    private void saveNoteAction() {
        if (!checkDirectoryExists()) return;

        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title cannot be empty.");
            return;
        }

        File file = new File(notesDirectory, title + ".txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(textArea.getText());
            JOptionPane.showMessageDialog(this, "Note saved successfully.");

            // Add saved note to the list if not already present
            if (!notesListModel.contains(file.getName())) {
                notesListModel.addElement(file.getName());
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving note: " + e.getMessage());
        }
    }

    // Action method for opening a note from the list
    private void openNoteFromList(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            titleField.setText(file.getName().replace(".txt", ""));
            textArea.setText("");
            String line;
            while ((line = reader.readLine()) != null) {
                textArea.append(line + "\n");
            }
            JOptionPane.showMessageDialog(this, "Note loaded: " + file.getName());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error opening note: " + e.getMessage());
        }
    }

    // Action method for creating a new note
    private void newNoteAction() {
        titleField.setText("");
        textArea.setText("");
    }

    // Action method for deleting a selected note
    private void deleteNoteAction() {
        int selectedIndex = notesList.getSelectedIndex();
        if (selectedIndex == -1) {
            JOptionPane.showMessageDialog(this, "No note selected to delete.");
            return;
        }

        String noteTitle = notesListModel.get(selectedIndex);
        File fileToDelete = new File(notesDirectory, noteTitle);

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the note: " + noteTitle + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (fileToDelete.delete()) {
                notesListModel.remove(selectedIndex);
                titleField.setText("");
                textArea.setText("");
                JOptionPane.showMessageDialog(this, "Note deleted successfully.");
            } else {
                JOptionPane.showMessageDialog(this, "Error deleting note.");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new NotesApp().setVisible(true);
        });
    }
}
