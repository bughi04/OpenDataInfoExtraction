package org.example;

import org.example.controller.DataController;
import org.example.controller.MainController;
import org.example.theme.ThemeManager;
import org.example.util.SoundManager;
import org.example.view.ModernMainView;
import org.example.view.SplashScreen;

import javax.swing.*;
import java.awt.*;

public class ModernMain {

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("sun.java2d.opengl", "true");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set system look and feel: " + e.getMessage());
        }

        System.out.println("Initializing CPV Analysis Tool Professional Edition...");
        SoundManager.initialize();

        SwingUtilities.invokeLater(() -> {
            SplashScreen splashScreen = new SplashScreen();
            splashScreen.showSplash();
        });

        SwingUtilities.invokeLater(() -> {
            try {
                ThemeManager.applyTheme(ThemeManager.LIGHT_THEME);

                System.out.println("Creating application components...");

                ModernMainView view = new ModernMainView();
                DataController dataController = new DataController();
                MainController mainController = new MainController(view, dataController);

                view.setDataController(dataController);
                view.setMainController(mainController);

                configureApplication(view);

                System.out.println("Starting CPV Analysis Tool...");
                view.show();

                SoundManager.playSound(SoundManager.SOUND_STARTUP);

                System.out.println("CPV Analysis Tool Professional Edition started successfully!");

            } catch (Exception e) {
                System.err.println("Error starting application: " + e.getMessage());
                e.printStackTrace();

                JOptionPane.showMessageDialog(null,
                        "Failed to start CPV Analysis Tool:\n" + e.getMessage(),
                        "Startup Error",
                        JOptionPane.ERROR_MESSAGE);

                System.exit(1);
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down CPV Analysis Tool...");
            SoundManager.cleanup();
            System.out.println("Cleanup complete. Goodbye!");
        }));
    }

    private static void configureApplication(ModernMainView view) {
        JFrame frame = view.getFrame();

        try {
            Image icon = Toolkit.getDefaultToolkit().getImage(
                    ModernMain.class.getResource("/icons/app-icon.png"));
            if (icon != null) {
                frame.setIconImage(icon);
            }
        } catch (Exception e) {
            System.out.println("Application icon not found, using default");
        }

        frame.setMinimumSize(new Dimension(1200, 800));
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        if (frame.getExtendedState() != JFrame.MAXIMIZED_BOTH) {
            frame.setLocationRelativeTo(null);
        }

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                int option = JOptionPane.showConfirmDialog(
                        frame,
                        "Are you sure you want to exit CPV Analysis Tool?",
                        "Confirm Exit",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

                if (option == JOptionPane.YES_OPTION) {
                    System.out.println("User requested application exit");
                    System.exit(0);
                }
            }
        });

        printApplicationInfo();
    }

    private static void printApplicationInfo() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("CPV ANALYSIS TOOL - PROFESSIONAL EDITION");
        System.out.println("Version: 1.0.0");
        System.out.println("Build Date: 2025");
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("Operating System: " + System.getProperty("os.name"));
        System.out.println("=".repeat(60));
        System.out.println("\nFeatures:");
        System.out.println("• 📊 Interactive Executive Dashboard");
        System.out.println("• 🔍 Advanced Search & Filtering");
        System.out.println("• 📈 Deep Analytics & Insights");
        System.out.println("• ⚠️  Risk Assessment Analysis");
        System.out.println("• 📋 Maturity Assessment");
        System.out.println("• 🤖 AI-Powered Assistant");
        System.out.println("• 🎨 Multiple Theme Support");
        System.out.println("• 📄 Comprehensive Reporting");
        System.out.println("• 🔊 Audio Feedback");
        System.out.println("\nSupported File Types:");
        System.out.println("• PAAP Excel files (.xlsx, .xls)");
        System.out.println("• CPV Codes Excel files (.xlsx, .xls)");
        System.out.println("\nReady for professional procurement analysis!");
        System.out.println("=".repeat(60) + "\n");
    }
}