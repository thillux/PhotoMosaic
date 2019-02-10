package de.thillux;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PhotoMosaicGUI extends JFrame {
    JPanel contentPane;
    JPanel gridPanel;

    JLabel imageFolderLabel;
    File imageFolder;

    JLabel outputFileLabel;
    File outputFile;

    JTextField widthField;
    JTextField heightField;
    JTextField dpiField;

    JProgressBar progressBar;
    JButton startButton;

    JScrollPane outputPane;
    JTextArea outputArea;

    JPanel statusPanel;
    JLabel statusLabel;

    File getInputPath() {
        String chooserTitle = "Select Input Path";
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle(chooserTitle);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.showDialog(this, "Ok");

        return chooser.getSelectedFile();
    }

    File getOutputFile() {
        String chooserTitle = "Select Output Path";
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle(chooserTitle);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.showDialog(this, "Ok");

        return chooser.getSelectedFile();
    }

    void selectPaths() {
        imageFolder = getInputPath();
        if(imageFolder == null) {
            setStatus("Invalid input path!");
            disableInputs();
            return;
        }
        imageFolderLabel.setText(imageFolder.getName());

        outputFile = getOutputFile();
        if(outputFile == null) {
            setStatus("Invalid output path!");
            disableInputs();
            return;
        }
        outputFileLabel.setText(outputFile.getName());

        pack();
    }

    void disableInputs() {
        widthField.setEnabled(false);
        heightField.setEnabled(false);
        dpiField.setEnabled(false);
    }

    void setStatus(String status) {
        statusLabel.setText(status);
    }

    public PhotoMosaicGUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setTitle("Photo Mosaic");

        contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        gridPanel = new JPanel();
        gridPanel.setLayout(new GridLayout(6,2));
        contentPane.add(gridPanel);

        imageFolderLabel = new JLabel();
        outputFileLabel = new JLabel();

        gridPanel.add(new JLabel("Image Folder: "));
        gridPanel.add(imageFolderLabel);

        gridPanel.add(new JLabel("Output File: "));
        gridPanel.add(outputFileLabel);

        gridPanel.add(new JLabel("Width (cm)"));
        gridPanel.add(new JLabel("Height (cm)"));
        widthField = new JTextField();
        widthField.setText("60");
        gridPanel.add(widthField);

        heightField = new JTextField();
        heightField.setText("90");
        gridPanel.add(heightField);

        gridPanel.add(new JLabel("DPI: "));
        dpiField = new JTextField();
        dpiField.setText("300.0");
        gridPanel.add(dpiField);

        startButton = new JButton("Start");

        startButton.addActionListener((ActionEvent ev) -> {
            final long width_cm = Long.parseLong(widthField.getText());
            final long height_cm = Long.parseLong(heightField.getText());
            final double dpi = Double.parseDouble(dpiField.getText());

            if(width_cm <= 0) {
                setStatus("Invalid width given");
                return;
            }

            if(height_cm <= 0) {
                setStatus("Invalid height given");
                return;
            }

            if(dpi <= 0.0) {
                setStatus("Invalid DPI set");
                return;
            }

            disableInputs();
            progressBar.setValue(0);
            progressBar.setStringPainted(true);

            setStatus("Conversion in Progress");

            new Thread(() -> {
                final long conversionStartTime = System.nanoTime();

                List<ImageFile> list = new ArrayList<>();
                for(File f : imageFolder.listFiles()) {
                    if(f.getName().endsWith(".jpeg") || f.getName().endsWith(".jpg") || f.getName().endsWith(".JPG") || f.getName().endsWith(".JPEG")) {
                        list.add(new ImageFile(f));
                    } else{
                        System.out.println("Cannot use non-image file: " + f.getName());
                    }
                }
                Collections.shuffle(list);

                GraphicsConfiguration gfxConf = GraphicsEnvironment.getLocalGraphicsEnvironment().
                        getDefaultScreenDevice().getDefaultConfiguration();

                final double cm_to_inch = 2.54;
                int width_px = (int) Math.round(width_cm / cm_to_inch * dpi);
                int height_px = (int) Math.round(height_cm / cm_to_inch * dpi);
                BufferedImage outputImage = gfxConf.createCompatibleImage(width_px, height_px);
                Graphics2D g2d = outputImage.createGraphics();
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, outputImage.getWidth(), outputImage.getHeight());
                g2d.setBackground(Color.WHITE);

                int numFiles = list.size();
                int currentNum = 0;
                int currentX = 0;
                int currentY = 0;
                int lineHeight = outputImage.getHeight() / 11;
                for(ImageFile imgFile : list) {
                    final int currentProgress = (int) (currentNum * 100.0 / numFiles);
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(currentProgress);
                    });

                    final long imageStartTime = System.nanoTime();

                    BufferedImage img = imgFile.getImage();
                    int width = (int) ((double) img.getWidth() * lineHeight / img.getHeight());
                    if(currentX + width > outputImage.getWidth()) {
                        currentX = 0;
                        currentY += lineHeight;
                    }
                    if(currentY >= outputImage.getHeight())
                        break;
                    g2d.drawImage(img, currentX, currentY, width, lineHeight, null);
                    currentX += width;

                    final long imageEndTime = System.nanoTime();
                    final long imageTimeDuration = imageEndTime - imageStartTime;

                    SwingUtilities.invokeLater(() -> {
                        outputArea.append("Processed: " + imgFile.getFilename() + " in " + imageTimeDuration / 1E6 + " ms\n");
                    });

                    ++currentNum;
                }
                try {
                    ImageIO.write(outputImage, "jpeg", outputFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(100);
                });

                final long conversionEndTime = System.nanoTime();
                final long conversionTimeDuration = conversionEndTime - conversionStartTime;
                SwingUtilities.invokeLater(() -> {
                    outputArea.append("\n");
                    outputArea.append("Whole processing took: " + conversionTimeDuration / 1E9 + " s\n");
                    setStatus("Conversion Finished");
                });
            }).start();
        });

        gridPanel.add(startButton);

        progressBar = new JProgressBar(0, 100);
        gridPanel.add(progressBar);

        outputPane = new JScrollPane();
        outputArea = new JTextArea(20, 80);
        outputArea.setEditable(false);
        outputPane.add(outputArea);
        outputPane.setViewportView(outputArea);
        contentPane.add(outputPane);

        DefaultCaret caret = (DefaultCaret) outputArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.OUT_BOTTOM);

        statusPanel = new JPanel();
        statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusLabel = new JLabel("status");
        statusPanel.add(statusLabel);
        contentPane.add(statusPanel);

        pack();
        setVisible(true);

        selectPaths();
    }
}
