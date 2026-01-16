package org.example.view;

import org.example.theme.ThemeManager;
import org.example.util.SoundManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public class SplashScreen extends JWindow {
    private static final int WIDTH = 500;
    private static final int HEIGHT = 300;
    private static final int DISPLAY_DURATION = 3000;

    private JProgressBar progressBar;
    private Timer timer;
    private int progress = 0;

    public SplashScreen() {
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);

        JPanel contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setColor(ThemeManager.getCurrentTheme().getBackgroundColor());
                g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 15, 15));

                g2d.setColor(ThemeManager.getCurrentTheme().getAccentColor());
                g2d.setStroke(new BasicStroke(2f));
                g2d.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 15, 15));

                g2d.dispose();
            }
        };
        contentPanel.setLayout(new BorderLayout());
        setContentPane(contentPanel);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        mainPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("CPV Analysis Tool");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(ThemeManager.getCurrentTheme().getAccentColor());
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel versionLabel = new JLabel("Version 1.0");
        versionLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        versionLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel logoLabel = new JLabel(createAppIcon(100, 100));
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel loadingLabel = new JLabel("Loading...");
        loadingLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        loadingLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        loadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setForeground(ThemeManager.getCurrentTheme().getAccentColor());
        progressBar.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        progressBar.setMaximumSize(new Dimension(WIDTH - 80, 20));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(Box.createVerticalGlue());
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(versionLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(logoLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(loadingLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(progressBar);
        mainPanel.add(Box.createVerticalGlue());

        contentPanel.add(mainPanel, BorderLayout.CENTER);

        JLabel copyrightLabel = new JLabel("© 2025 Your Organization");
        copyrightLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        copyrightLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        copyrightLabel.setHorizontalAlignment(SwingConstants.CENTER);
        contentPanel.add(copyrightLabel, BorderLayout.SOUTH);
    }

    private ImageIcon createAppIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(ThemeManager.getCurrentTheme().getPanelColor());
        g2d.fillOval(0, 0, width, height);

        g2d.setColor(ThemeManager.getCurrentTheme().getAccentColor());
        g2d.setStroke(new BasicStroke(3f));
        g2d.drawOval(3, 3, width - 6, height - 6);

        g2d.setFont(new Font("Arial", Font.BOLD, 32));
        g2d.setColor(ThemeManager.getCurrentTheme().getAccentColor());

        FontMetrics metrics = g2d.getFontMetrics();
        String text = "CPV";
        int x = (width - metrics.stringWidth(text)) / 2;
        int y = ((height - metrics.getHeight()) / 2) + metrics.getAscent();

        g2d.drawString(text, x, y);

        g2d.dispose();
        return new ImageIcon(image);
    }

    public void showSplash() {
        setVisible(true);

        SoundManager.playSound(SoundManager.SOUND_STARTUP);

        timer = new Timer(30, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                progress += 1;
                progressBar.setValue(progress);

                if (progress >= 100) {
                    timer.stop();

                    Timer closeTimer = new Timer(500, event -> {
                        dispose();
                    });
                    closeTimer.setRepeats(false);
                    closeTimer.start();
                }
            }
        });

        timer.start();
    }
}