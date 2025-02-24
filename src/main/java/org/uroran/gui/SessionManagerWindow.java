package org.uroran.gui;

import org.uroran.models.SessionData;
import org.uroran.service.SessionDataService;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Класс окна-менеджера сессий.
 */
public class SessionManagerWindow extends JFrame {
    private static final String HOST = "umt.imm.uran.ru";
    private static final int PORT = 22;

    private JTextField sessionNameField;  // Новое поле
    private JTextField usernameField;
    private JTextField privateKeyField;
    private JTextField passphraseField;
    private JList<String> sessionList;

    private final SessionDataService sessionDataService;

    public SessionManagerWindow(SessionDataService sessionDataService) {
        this.sessionDataService = sessionDataService;

        //Создание окна выбора сессии
        setTitle("УРО РАН - Управление сессиями");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());

        JPanel createSessionPanel = createSessionPanel();
        JPanel existingSessionsPanel = createExistingSessionPanel();

        add(createSessionPanel);
        add(existingSessionsPanel);

        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Метод для создания панели создания сессии.
     *
     * @return - панель создания сессии
     */
    public JPanel createSessionPanel() {
        JPanel createSessionPanel = new JPanel(new BorderLayout());
        createSessionPanel.setBorder(BorderFactory.createTitledBorder("Создать новую сессию"));

        JPanel fieldsPanel = new JPanel(new GridLayout(6, 1, 0, 10));

        sessionNameField = new JTextField(3);
        usernameField = new JTextField(3);
        privateKeyField = new JTextField(3);
        passphraseField = new JPasswordField(3);

        JButton browseButton = new JButton("Обзор...");
        browseButton.addActionListener(_ -> choosePrivateKeyFile());

        JPanel privateKeyPanel = new JPanel(new BorderLayout());
        privateKeyPanel.add(privateKeyField, BorderLayout.CENTER);
        privateKeyPanel.add(browseButton, BorderLayout.EAST);

        fieldsPanel.add(new JLabel("Имя сессии:  "));
        fieldsPanel.add(sessionNameField);
        fieldsPanel.add(new JLabel("Имя пользователя:  "));
        fieldsPanel.add(usernameField);
        fieldsPanel.add(new JLabel("Приватный ключ (OpenSSH):  "));
        fieldsPanel.add(privateKeyPanel);
        fieldsPanel.add(new JLabel("Контрольное слово:  "));
        fieldsPanel.add(passphraseField);
        createSessionPanel.add(fieldsPanel, BorderLayout.PAGE_START);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        JButton addButton = new JButton("Добавить");
        addButton.addActionListener(_ -> addNewSession());
        buttonPanel.add(addButton);
        createSessionPanel.add(buttonPanel, BorderLayout.PAGE_END);

        return createSessionPanel;
    }

    /**
     * Метод для создания панели со списком сохраненных сессий.
     *
     * @return - панель со списком сессий
     */
    public JPanel createExistingSessionPanel() {
        JPanel existingSessionsPanel = new JPanel(new BorderLayout(0, 50));
        existingSessionsPanel.setBorder(BorderFactory.createTitledBorder("Сохраненные сессии"));

        String[] savedSessions = sessionDataService.loadSessionDataList().toArray(new String[0]);
        sessionList = new JList<>(savedSessions);
        JScrollPane sessionScrollPane = new JScrollPane(sessionList);

        JButton connectButton = new JButton("Подключить");
        connectButton.addActionListener(_ -> connectToSelectedSession());

        JButton deleteButton = new JButton("Удалить");
        deleteButton.addActionListener(_ -> deleteSession());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(connectButton);
        buttonPanel.add(deleteButton);

        existingSessionsPanel.add(sessionScrollPane, BorderLayout.CENTER);
        existingSessionsPanel.add(buttonPanel, BorderLayout.SOUTH);

        return existingSessionsPanel;
    }

    /**
     * Выбор пути до приватного ключа
     */
    private void choosePrivateKeyFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            privateKeyField.setText(selectedFile.getAbsolutePath());
        }
    }

    /**
     * Добавление новой сессии в список сессий
     */
    private void addNewSession() {
        String sessionName = sessionNameField.getText();
        String username = usernameField.getText();
        String privateKeyPath = privateKeyField.getText();
        String passphrase = passphraseField.getText();

        SessionData newSession = SessionData.builder()
                .name(sessionName)
                .host(HOST)
                .port(PORT)
                .user(username)
                .pathToKey(privateKeyPath)
                .passPhrase(passphrase)
                .build();

        try {
            sessionDataService.saveSessionData(newSession);
            refreshSessionList();
            JOptionPane.showMessageDialog(this, "Сессия успешно добавлена.", "Успех", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException | IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Ошибка при добавлении", JOptionPane.ERROR_MESSAGE);
        }

        sessionNameField.setText("");
        usernameField.setText("");
        privateKeyField.setText("");
        passphraseField.setText("");
    }

    /**
     * Подключение к выбранной сессии. Закрывается текущее окно и открывается следующее.
     */
    private void connectToSelectedSession() {
        String selectedSession = sessionList.getSelectedValue();

        if (selectedSession == null) {
            JOptionPane.showMessageDialog(this, "Выберите сессию для подключения.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SessionData chosenSession;
        try {
             chosenSession = sessionDataService.loadSessionData(selectedSession);
        } catch (IOException | RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Ошибка при загрузке", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingUtilities.invokeLater(() -> new MainWindow(chosenSession).setVisible(true));

        this.dispose();
    }

    /**
     * Удаление сессии из списка сессий
     */
    private void deleteSession() {
        String selectedSession = sessionList.getSelectedValue();

        if (selectedSession == null) {
            JOptionPane.showMessageDialog(this, "Выберите сессию для удаления.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Вы уверены, что хотите удалить сессию \"" + selectedSession + "\"?",
                "Подтверждение удаления",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                sessionDataService.deleteSession(selectedSession);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Ошибка при удалении", JOptionPane.ERROR_MESSAGE);
                return;
            }

            refreshSessionList();
            JOptionPane.showMessageDialog(this, "Сессия успешно удалена.", "Успех", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Обновление списка сессий
     */
    private void refreshSessionList() {
        String[] updatedSessions = sessionDataService.loadSessionDataList().toArray(new String[0]);
        DefaultListModel<String> model = new DefaultListModel<>();
        for (String session : updatedSessions) {
            model.addElement(session);
        }
        sessionList.setModel(model);
    }
}
