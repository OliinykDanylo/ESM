package App;

import Controller.Controller;
import Models.BaseModel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class MainGUI extends JFrame {
    private Controller controller;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private JButton runModelButton;
    private final JButton runScriptFromFileButton;
    private final JButton createAndRunAdHocScriptButton;

    public MainGUI() {
        setTitle("MCS");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());

        // panel for model and data selection
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel modelLabel = new JLabel("Select model and data:");
        leftPanel.add(modelLabel);

        // model List
        DefaultListModel<String> modelListModel = new DefaultListModel<>();
        populateModelList(modelListModel);
        JList<String> modelList = new JList<>(modelListModel);
        JScrollPane modelScrollPane = new JScrollPane(modelList);
        leftPanel.add(modelScrollPane);

        // data List
        DefaultListModel<String> dataListModel = new DefaultListModel<>();
        populateDataList(dataListModel);
        JList<String> dataList = new JList<>(dataListModel);
        JScrollPane dataScrollPane = new JScrollPane(dataList);
        leftPanel.add(dataScrollPane);

        runModelButton = new JButton("Run model");
        runModelButton.addActionListener(e -> runModel(modelList, dataList));
        leftPanel.add(runModelButton);

        add(leftPanel, BorderLayout.WEST);

        // results table and buttons
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());

        // results table
        tableModel = new DefaultTableModel();
        resultsTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(resultsTable);
        centerPanel.add(tableScrollPane, BorderLayout.CENTER);

        // panel for buttons below the table
        JPanel tableButtonsPanel = new JPanel();
        tableButtonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        runScriptFromFileButton = new JButton("Run script from file");
        runScriptFromFileButton.setVisible(false); // hidden from start
        runScriptFromFileButton.addActionListener(this::runScriptFromFile);
        tableButtonsPanel.add(runScriptFromFileButton);

        createAndRunAdHocScriptButton = new JButton("Create and run ad hoc script");
        createAndRunAdHocScriptButton.setVisible(false); // also hidden from start
        createAndRunAdHocScriptButton.addActionListener(this::createAndRunAdHocScript);
        tableButtonsPanel.add(createAndRunAdHocScriptButton);

        centerPanel.add(tableButtonsPanel, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);
    }

    private void populateModelList(DefaultListModel<String> modelListModel) {
        try {
            // all model files
            File[] modelFiles = getModelFiles();
            if (modelFiles == null) return;

            // valid model class names
            List<String> modelNames = getValidModelNames(modelFiles);

            // to sort and add to list model
            addModelNamesToList(modelListModel, modelNames);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File[] getModelFiles() {
        File modelsDirectory = new File("/Users/danylooliinyk/programming/UTP/Projects/MCS/src/main/java/Models");
        if (modelsDirectory.exists() && modelsDirectory.isDirectory()) {
            return modelsDirectory.listFiles((dir, name) -> name.endsWith(".java"));
        }
        return null;
    }

    private List<String> getValidModelNames(File[] modelFiles) {
        List<String> modelNames = new ArrayList<>();
        for (File modelFile : modelFiles) {
            try {
                String className = modelFile.getName().replace(".java", "");
                Class<?> clazz = Class.forName("Models." + className);

                // the class must extend BaseModel but also is not BaseModel
                if (BaseModel.class.isAssignableFrom(clazz) && !clazz.equals(BaseModel.class)) {
                    modelNames.add(className);
                }
            } catch (ClassNotFoundException e) {
                System.err.println("Class not found: " + modelFile.getName());
            }
        }
        return modelNames;
    }

    private void addModelNamesToList(DefaultListModel<String> modelListModel, List<String> modelNames) {
        Collections.sort(modelNames); // to sort alphabetically
        for (String modelName : modelNames) {
            modelListModel.addElement(modelName);
        }
    }

    private void populateDataList(DefaultListModel<String> dataListModel) {
        try {
            File dataDirectory = new File("/Users/danylooliinyk/programming/UTP/Projects/MCS/src/main/data");
            if (dataDirectory.exists() && dataDirectory.isDirectory()) {
                File[] dataFiles = dataDirectory.listFiles((dir, name) -> name.endsWith(".txt"));
                if (dataFiles != null) {
                    for (File dataFile : dataFiles) {
                        dataListModel.addElement(dataFile.getName()); // to add data file name
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runModel(JList<String> modelList, JList<String> dataList) {
        String selectedModel = modelList.getSelectedValue();
        String selectedData = dataList.getSelectedValue();

        if (selectedModel == null || selectedData == null) {
            JOptionPane.showMessageDialog(this, "Please select a model and data file.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        controller = new Controller(selectedModel);
        controller.readDataFrom("/Users/danylooliinyk/programming/UTP/Projects/MCS/src/main/data/" + selectedData).runModel();

        updateResultsTable(controller.getResultsAsTsv());

        // the additional buttons appear after the model is run
        runScriptFromFileButton.setVisible(true);
        createAndRunAdHocScriptButton.setVisible(true);
    }

    private void runScriptFromFile(ActionEvent e) {
        if (controller == null) {
            JOptionPane.showMessageDialog(this, "Please run a model first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File scriptFile = fileChooser.getSelectedFile();
            controller.runScriptFromFile(scriptFile.getAbsolutePath());
            updateResultsTable(controller.getResultsAsTsv());
        }
    }

    private void createAndRunAdHocScript(ActionEvent e) {
        if (controller == null) {
            JOptionPane.showMessageDialog(this, "Please run a model first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // JTextArea for input
        JTextArea scriptArea = new JTextArea(10, 40);
        JScrollPane scrollPane = new JScrollPane(scriptArea); // a scroll pane for larger scripts

        // the input dialog with the text area
        int result = JOptionPane.showConfirmDialog(
                this,
                scrollPane,
                "Enter your script code",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String script = scriptArea.getText();
            if (script != null && !script.trim().isEmpty()) {
                controller.runScript(script);
                updateResultsTable(controller.getResultsAsTsv());
            }
        }
    }

    private void updateResultsTable(String tsvResults) {
        // to clear the existing table model
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);

        // put results
        String[] lines = tsvResults.split("\n");
        if (lines.length > 0) {
            // row with column names
            String[] columns = lines[0].split("\t");
            for (String column : columns) {
                tableModel.addColumn(column);
            }

            // data rows
            for (int i = 1; i < lines.length; i++) {
                String[] row = lines[i].split("\t");
                tableModel.addRow(row);
            }
        }

        // number formatting
        NumberFormatting numberRenderer = new NumberFormatting();

        for (int col = 1; col < tableModel.getColumnCount(); col++) {
            resultsTable.getColumnModel().getColumn(col).setCellRenderer(numberRenderer);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainGUI app = new MainGUI();
            app.setVisible(true);
        });
    }
}