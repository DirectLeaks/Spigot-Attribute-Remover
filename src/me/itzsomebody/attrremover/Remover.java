package me.itzsomebody.attrremover;

import java.awt.event.*;
import javax.swing.*;
import java.awt.*;
import java.util.zip.*;
import java.io.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

@SuppressWarnings("serial")
public class Remover extends JFrame {
	private static int count = 0;
    private JTextField field;
    
    public static void main(String[] args) {
        createGUI();
    }
    
    private static void createGUI() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                }
                catch (Exception ex) {}
                Remover remover = new Remover();
                remover.setTitle("Spigot Attribute Remover");
                remover.setResizable(false);
                remover.setSize(400, 100);
                remover.setLocationRelativeTo(null);
                remover.setDefaultCloseOperation(3);
                remover.getContentPane().setLayout(new FlowLayout());
                JLabel label = new JLabel("Select File:");
                remover.field = new JTextField();
                remover.field.setEditable(false);
                remover.field.setColumns(18);
                JButton selectButton = new JButton("Select");
                selectButton.setToolTipText("Select jar file");
                selectButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        JFileChooser chooser = new JFileChooser();
                        if (remover.field.getText() != null && !remover.field.getText().isEmpty()) {
                            chooser.setSelectedFile(new File(remover.field.getText()));
                        }
                        chooser.setMultiSelectionEnabled(false);
                        chooser.setFileSelectionMode(0);
                        int result = chooser.showOpenDialog(remover);
                        if (result == 0) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    remover.field.setText(chooser.getSelectedFile().getAbsolutePath());
                                }
                            });
                        }
                    }
                });
                JButton startButton = new JButton("Start");
                startButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (remover.field.getText() == null || remover.field.getText().isEmpty() || !remover.field.getText().endsWith(".jar")) {
                            JOptionPane.showMessageDialog(null, "You must select a valid jar file!", "Error", 0);
                            return;
                        }
                        File output = null;
                        try {
                            File input = new File(remover.field.getText());
                            if (!input.getName().endsWith(".jar")) {
                                throw new IllegalArgumentException("File must be a jar.");
                            }
                            if (!input.exists()) {
                                throw new FileNotFoundException("The file " + input.getName() + " doesn't exist.");
                            }
                            output = new File(String.format("%s-Output.jar", input.getAbsolutePath().substring(0, input.getAbsolutePath().lastIndexOf("."))));
                            if (output.exists()) {
                                output.delete();
                            }
                            process(input, output, 1);
                            checkFile(output);
                            if (count == 1) {
                            	JOptionPane.showMessageDialog(null, "Removed Spigot CompileVersion attribute.", "Success", 1);
                            } else {
                            	JOptionPane.showMessageDialog(null, "Removed Spigot " + String.valueOf(count) + " CompileVersion attributes.", "Success", 1);
                            }
                            
                        }
                        catch (Throwable t) {
                            JOptionPane.showMessageDialog(null, t, "Error", 0);
                            t.printStackTrace();
                            if (output != null) {
                                output.delete();
                            }
                        }
                        finally {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    remover.field.setText("");
                                }
                            });
                        }
                    }
                });
                JPanel panel = new JPanel(new FlowLayout());
                panel.add(label);
                panel.add(remover.field);
                panel.add(selectButton);
                JPanel panel2 = new JPanel(new FlowLayout());
                panel2.add(startButton);
                JPanel border = new JPanel(new BorderLayout());
                border.add(panel, "North");
                border.add(panel2, "South");
                remover.getContentPane().add(border);
                remover.setVisible(true);
            }
        });
    }
    
    private static void process(File jarFile, File outputFile, int mode) throws Throwable {
        ZipFile zipFile = new ZipFile(jarFile);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        ZipOutputStream out = (mode == 1) ? new ZipOutputStream(new FileOutputStream(outputFile)) : null;
        try {
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry)entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    try (InputStream in = zipFile.getInputStream(entry)) {
                        ClassReader cr = new ClassReader(in);
                        ClassNode classNode = new ClassNode();
                        cr.accept(classNode, 0);
                        
                        removeAttr(classNode);
                        
                        ClassWriter cw = new ClassWriter(0);
                        classNode.accept(cw);
                        ZipEntry newEntry = new ZipEntry(entry.getName());
                        newEntry.setTime(System.currentTimeMillis());
                        out.putNextEntry(newEntry);
                        writeToFile(out, new ByteArrayInputStream(cw.toByteArray()));
                    }
                } else {
                    entry.setTime(System.currentTimeMillis());
                    out.putNextEntry(entry);
                    writeToFile(out, zipFile.getInputStream(entry));
                }
            }
        }
        finally {
            zipFile.close();
            if (out != null) {
                out.close();
            }
        }
    }
    
    private static void writeToFile(ZipOutputStream outputStream, InputStream inputStream) throws Throwable {
        byte[] buffer = new byte[4096];
        try {
            while (inputStream.available() > 0) {
                int data = inputStream.read(buffer);
                outputStream.write(buffer, 0, data);
            }
        }
        finally {
            inputStream.close();
            outputStream.closeEntry();
        }
    }
    
    private static void checkFile(File jarFile) throws Throwable {
        if (!jarFile.exists()) {
            throw new IllegalStateException("Output file not found.");
        }
    }
    
    private static void removeAttr(ClassNode classNode) throws Throwable {
        if (classNode.attrs != null) {
        	for (int i = 0; i < classNode.attrs.size(); ++i) {
        		if (classNode.attrs.get(i).type.equals("CompileVersion")) {
        			classNode.attrs.remove(i);
        			++count;
        		}
        	}
        }
    }
}