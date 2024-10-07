import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class TwoPassAssemblerGUI {

    private static Map<String, Integer> symbolTable = new HashMap<>();
    private static Map<String, String> opcodeTable = new HashMap<>();
    private static int startingAddress = 0;
    private static int programLength = 0;

    // UI Components
    private static JTextArea intermediateArea;
    private static JTextArea symtabArea;
    private static JTextArea lengthArea;
    private static JTextArea outputArea;
    private static JTextArea objectCodeArea;

    private static File inputFile;
    private static File optabFile;

    public static void main(String[] args) {
        // Create JFrame
        JFrame frame = new JFrame("Two-Pass Assembler");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLayout(new BorderLayout(10, 10));
        frame.getContentPane().setBackground(new Color(230, 230, 250)); // Light lavender background

        // Set modern look and feel
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create text areas for displaying results
        intermediateArea = createTextArea("Intermediate");
        symtabArea = createTextArea("Symbol Table");
        lengthArea = createTextArea("Program Length");
        outputArea = createTextArea("Output");
        objectCodeArea = createTextArea("Object Code");

        // Create a panel for the text areas
        JPanel textAreaPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        textAreaPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        textAreaPanel.add(createLeftColumn());
        textAreaPanel.add(createRightColumn());

        // Create button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(new Color(230, 230, 250)); // Same as frame background

        // Create buttons to load files
        JButton loadInputButton = createButton("Load Input File", e -> loadInputFile(frame));
        JButton loadOptabButton = createButton("Load Optab File", e -> loadOptabFile(frame));

        // Create Run button
        JButton runButton = createButton("Run Assembler", e -> runAssembler(frame));

        // Add buttons to the button panel
        buttonPanel.add(loadInputButton);
        buttonPanel.add(loadOptabButton);
        buttonPanel.add(runButton);

        // Add components to the frame
        frame.add(buttonPanel, BorderLayout.NORTH);
        frame.add(textAreaPanel, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    private static JTextArea createTextArea(String title) {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        textArea.setBackground(new Color(255, 255, 255)); // White background
        textArea.setForeground(new Color(0, 0, 0)); // Black text
        textArea.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), title, 0, 0, new Font("Arial", Font.BOLD, 12)));
        return textArea;
    }

    private static JPanel createLeftColumn() {
        JPanel leftPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        leftPanel.add(new JScrollPane(intermediateArea));
        leftPanel.add(new JScrollPane(symtabArea));
        leftPanel.add(new JScrollPane(lengthArea));
        return leftPanel;
    }

    private static JPanel createRightColumn() {
        JPanel rightPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        rightPanel.add(new JScrollPane(outputArea));
        rightPanel.add(new JScrollPane(objectCodeArea));
        return rightPanel;
    }

    private static JButton createButton(String text, ActionListener action) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(160, 35));
        button.setBackground(new Color(100, 149, 237)); // Cornflower blue
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBorder(BorderFactory.createLineBorder(new Color(70, 130, 180), 2));
        button.addActionListener(action);
        button.setToolTipText(text);
        return button;
    }

    private static void loadInputFile(JFrame frame) {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(frame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            inputFile = fileChooser.getSelectedFile();
        }
    }

    private static void loadOptabFile(JFrame frame) {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(frame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            optabFile = fileChooser.getSelectedFile();
            try {
                loadOptabFromFile(optabFile);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Error loading Optab file: " + ex.getMessage());
            }
        }
    }

    private static void runAssembler(JFrame frame) {
        try {
            if (inputFile != null) {
                pass1(inputFile.getAbsolutePath());
                pass2();
                loadSymtab(); // Update the symbol table display
            } else {
                JOptionPane.showMessageDialog(frame, "Please load an input file first.");
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Error during assembly: " + ex.getMessage());
        }
    }

    // Pass 1 implementation
    private static void pass1(String inputFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        StringBuilder intermediateContent = new StringBuilder();
        StringBuilder lengthContent = new StringBuilder();

        String line;
        int locationCounter = 0;
        boolean isStartFound = false;

        while ((line = reader.readLine()) != null) {
            String[] parts = line.trim().split("\\s+");

            String label = "-";
            String opcode = "-";
            String operand = "-";

            if (parts.length > 0) {
                label = parts[0].equals("-") ? "-" : parts[0];
                opcode = (parts.length > 1 && !parts[1].equals("-")) ? parts[1] : "-";
                operand = (parts.length > 2 && !parts[2].equals("-")) ? parts[2] : "-";

                if (opcode.equals("START") && !isStartFound) {
                    startingAddress = Integer.parseInt(operand, 16);
                    locationCounter = startingAddress;
                    isStartFound = true;
                    intermediateContent.append("-\t").append(label).append("\t").append(opcode).append("\t").append(operand).append("\n");
                    continue;
                }

                if (!label.equals("-")) {
                    if (symbolTable.containsKey(label)) {
                        System.out.println("Error: Duplicate label - " + label);
                    } else {
                        symbolTable.put(label, locationCounter);
                    }
                }

                intermediateContent.append(Integer.toHexString(locationCounter).toUpperCase())
                        .append("\t")
                        .append(label)
                        .append("\t")
                        .append(opcode)
                        .append("\t")
                        .append(operand)
                        .append("\n");

                if (opcode.equals("WORD")) {
                    locationCounter += 3;
                } else if (opcode.equals("RESW")) {
                    locationCounter += 3 * Integer.parseInt(operand);
                } else if (opcode.equals("RESB")) {
                    locationCounter += Integer.parseInt(operand);
                } else if (opcode.equals("BYTE")) {
                    locationCounter += operand.length() - 3;
                } else if (opcodeTable.containsKey(opcode)) {
                    locationCounter += 3;
                }
            }
        }

        programLength = locationCounter - startingAddress;
        lengthContent.append("Program Length: ").append(Integer.toHexString(programLength).toUpperCase()).append("\n");

        intermediateArea.setText(intermediateContent.toString());
        lengthArea.setText(lengthContent.toString());

        reader.close();
    }

    // Pass 2 implementation
    private static void pass2() throws IOException {
        StringBuilder outputContent = new StringBuilder();
        StringBuilder objectCodeContent = new StringBuilder();
        StringBuilder textRecord = new StringBuilder();
        StringBuilder currentTextRecord = new StringBuilder();
        String currentTextStartAddress = "";
        int textRecordLength = 0;

        BufferedReader intermediateReader = new BufferedReader(new StringReader(intermediateArea.getText()));
        String line;

        // Initialize variables to store program name and start address
        String programName = "";
        String startAddressHex = Integer.toHexString(startingAddress).toUpperCase();

        // Read through the intermediate file to get the program name from the START directive
        while ((line = intermediateReader.readLine()) != null) {
            String[] parts = line.trim().split("\\s+");

            if (parts.length < 4) continue;

            String address = parts[0];
            String label = parts[1];
            String opcode = parts[2];
            String operand = parts[3];

            if (opcode.equals("START")) {
                // Set the program name from the label of the START line
                programName = label;
                // Ensure it's 6 characters (padded with spaces if needed)
                programName = String.format("%-6s", programName);
                break;
            }
        }

        // Header record: Program name, starting address, program length
        objectCodeContent.append(String.format("H^%s^%06X^%06X\n", programName, startingAddress, programLength));

        // Reset the reader to go through the file again for processing
        intermediateReader.close();
        intermediateReader = new BufferedReader(new StringReader(intermediateArea.getText()));

        while ((line = intermediateReader.readLine()) != null) {
            String[] parts = line.trim().split("\\s+");

            if (parts.length < 4) continue;

            String address = parts[0];
            String label = parts[1];
            String opcode = parts[2];
            String operand = parts[3];
            String objectCode = "";

            if (opcodeTable.containsKey(opcode)) {
                String opCodeValue = opcodeTable.get(opcode);
                int operandAddress = 0;

                if (!operand.equals("-") && symbolTable.containsKey(operand)) {
                    operandAddress = symbolTable.get(operand);
                }

                objectCode = String.format("%s%04X", opCodeValue, operandAddress);
            } else if (opcode.equals("WORD")) {
                objectCode = String.format("%06X", Integer.parseInt(operand));
            } else if (opcode.equals("BYTE")) {
                if (operand.startsWith("C'")) {
                    StringBuilder asciiHex = new StringBuilder();
                    for (char c : operand.substring(2, operand.length() - 1).toCharArray()) {
                        asciiHex.append(String.format("%02X", (int) c));
                    }
                    objectCode = asciiHex.toString();
                } else if (operand.startsWith("X'")) {
                    objectCode = operand.substring(2, operand.length() - 1);
                }
            }

            outputContent.append(address).append("\t").append(label).append("\t").append(opcode).append("\t").append(operand).append("\t").append(objectCode).append("\n");

            if (!objectCode.isEmpty()) {
                if (currentTextRecord.length() == 0) {
                    currentTextStartAddress = address;
                }
                currentTextRecord.append("^").append(objectCode);
                textRecordLength += objectCode.length() / 2;
            }

            if (opcode.equals("END")) {
                break;
            }
        }

        // Add any remaining text record
        if (currentTextRecord.length() > 0) {
            textRecord.append(String.format("T^%06X^%02X%s\n", Integer.parseInt(currentTextStartAddress, 16), textRecordLength, currentTextRecord.toString()));
        }

        objectCodeContent.append(textRecord.toString());
        objectCodeContent.append("E^").append(startAddressHex).append("\n");

        outputArea.setText(outputContent.toString());
        objectCodeArea.setText(objectCodeContent.toString());

        intermediateReader.close();
    }

    // Load the Opcode Table from optab file
    private static void loadOptabFromFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        opcodeTable.clear();
        String line;

        while ((line = reader.readLine()) != null) {
            String[] parts = line.split("\\s+");
            if (parts.length == 2) {
                opcodeTable.put(parts[0], parts[1]);
            }
        }

        reader.close();
        System.out.println("Loaded Opcode Table: " + opcodeTable); // Debug output
    }

    // Load the Symbol Table from symtab
    private static void loadSymtab() {
        StringBuilder symtabContent = new StringBuilder();
        for (Map.Entry<String, Integer> entry : symbolTable.entrySet()) {
            symtabContent.append(entry.getKey())
                    .append("\t")
                    .append(Integer.toHexString(entry.getValue()).toUpperCase())
                    .append("\n");
        }
        symtabArea.setText(symtabContent.toString());
    }
}
