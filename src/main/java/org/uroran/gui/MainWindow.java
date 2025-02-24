package org.uroran.gui;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.uroran.models.SessionData;
import org.uroran.models.SftpEntry;
import org.uroran.models.TemperatureData;
import org.uroran.service.*;
import org.uroran.util.ColorUtils;
import org.uroran.util.PointParser;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Класс главного окна
 */
public class MainWindow extends JFrame {
    private static final SftpEntry EMPTY_ENTRY = new SftpEntry("..", SftpEntry.EntryType.DIRECTORY, "");
    private static final Path TEMP_FILES_DIR = Paths.get(System.getProperty("user.home"), "tempfiles/");

    private final List<String> currentFiles = new ArrayList<>();
    private int currentFilesIndex = 0;

    private JTextField commandInputField;
    private StyledDocument document;
    private DefaultTableModel tableModel;

    private final SessionManager sessionManager;
    private final SshService sshService;
    private final SftpService sftpService;

    private final SettingsManager settingsManager;
    private boolean sync;

    private final TaskManager taskManager;

    public MainWindow(SessionData sessionData) {
        sessionManager = new SessionManager(sessionData);

        try {
            sessionManager.connect();
        } catch (JSchException e) {
            throw new RuntimeException("Не получилось создать сессию", e);
        }

        try {
            Channel channelSsh = sessionManager.openChannel("shell");
            Channel channelSftp = sessionManager.openChannel("sftp");

            this.sshService = new SshService(channelSsh);
            this.sftpService = new SftpService(channelSftp);

            sshService.connect();
            sftpService.connect();
        } catch (IOException | JSchException | SftpException e) {
            throw new RuntimeException("Не получилось открыть каналы", e);
        }

        settingsManager = new SettingsManager();
        applySettings();

        taskManager = new TaskManager(sshService);
        Runtime.getRuntime().addShutdownHook(new Thread(taskManager::shutdown));

        try {
            if (!Files.exists(TEMP_FILES_DIR)) {
                Files.createDirectory(TEMP_FILES_DIR);
            }
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать директорию для временных файлов: " + TEMP_FILES_DIR, e);
        }

        initUI();
    }

    /**
     * Метод для применения настроек, взятых из settingsManager.
     */
    private void applySettings() {
        sync = Boolean.parseBoolean(settingsManager.getSetting("syncDirectories"));
    }

    /**
     * Метод для инициализации всех главных панелей окна.
     */
    private void initUI() {
        setTitle("СК 'УРАН'");
        setSize(1000, 600);
        setLocationRelativeTo(null);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });

        // Основной контейнер
        Container container = getContentPane();
        container.setLayout(new BorderLayout());

        setJMenuBar(createMenuBar());

        // Разделитель
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setDividerLocation(550);
        container.add(mainSplitPane, BorderLayout.CENTER);

        JPanel sshPanel = createSshPanel();
        mainSplitPane.setLeftComponent(sshPanel);

        JPanel sftpPanel = createSftpPanel();
        mainSplitPane.setRightComponent(sftpPanel);

        sendCommand();
    }

    /**
     * Метод, вызывающийся перед закрытием окна и закрывающий сессию и каналы.
     */
    private void onClose() {
        sshService.disconnect();
        sftpService.disconnect();
        sessionManager.disconnect();
    }

    /**
     * Метод для создания левой панели - панели для работы по SSH.
     *
     * @return панель
     */
    private JPanel createSshPanel() {
        JPanel sshPanel = new JPanel(new BorderLayout());

        JTextPane terminalOutputPane = new JTextPane();
        terminalOutputPane.setEditable(false);
        terminalOutputPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
        document = terminalOutputPane.getStyledDocument();
        JScrollPane scrollPane = new JScrollPane(terminalOutputPane);
        sshPanel.add(scrollPane, BorderLayout.CENTER);

        commandInputField = new JTextField();
        commandInputField.addActionListener(_ -> sendCommand());
        commandInputField.setFocusTraversalKeysEnabled(false);

        // По нажатию на TAB пробегаемся по списку доступных в текущей директории файлов
        commandInputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    e.consume();
                    if (!currentFiles.isEmpty()) {
                        String currentText = commandInputField.getText();
                        if (!currentText.contains(" ")) {
                            currentText += "";
                        }

                        int lastIndexOfSpace = currentText.lastIndexOf(" ");
                        String lastWord = currentText.substring(lastIndexOfSpace).trim();
                        if (currentFiles.contains(lastWord)) {
                            currentText = currentText.substring(0, lastIndexOfSpace + 1);
                        }

                        commandInputField.setText(currentText + currentFiles.get(currentFilesIndex));
                        currentFilesIndex = (currentFilesIndex + 1) % currentFiles.size();
                    }
                }
            }
        });
        sshPanel.add(commandInputField, BorderLayout.SOUTH);

        return sshPanel;
    }

    /**
     * Метод для создания правой панели - панели для работы по SFTP.
     *
     * @return панель
     */
    private JPanel createSftpPanel() {
        JPanel sftpPanel = new JPanel(new BorderLayout());

        // Создаем таблицу для отображения файлов
        String[] columnNames = {"Тип", "Имя", "Время изменения"};
        tableModel = new DefaultTableModel(columnNames, 0);
        JTable fileTable = new JTable(tableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        fileTable.setShowGrid(false);

        // Прокрутка
        JScrollPane scrollPane = new JScrollPane(fileTable);
        sftpPanel.add(scrollPane, BorderLayout.CENTER);

        // Слушатель для обновления текущей директории по нажатию на ЛКМ или отображения PopUp меню при ПКМ
        fileTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                int row = fileTable.rowAtPoint(event.getPoint());
                if (row < 0) return;

                String fileName = (String) tableModel.getValueAt(row, 1);
                String fileType = (String) tableModel.getValueAt(row, 0);

                // ЛКМ
                if (event.getClickCount() == 2
                        && (fileType.equals(SftpEntry.EntryType.DIRECTORY.toString())
                        || fileType.equals(SftpEntry.EntryType.LINK.toString()))
                ) {
                    try {
                        if (sync) {
                            commandInputField.setText("cd " + fileName);
                            sendCommand();
                        } else {
                            sftpService.changeCurrentRemoteDir(Path.of(fileName));
                        }

                        updateFileList();
                    } catch (SftpException e) {
                        JOptionPane.showMessageDialog(sftpPanel, "Не удалось открыть папку: " + e.getMessage());
                    }
                }

                // ПКМ
                if (SwingUtilities.isRightMouseButton(event)) {
                    JPopupMenu contextMenu = createFileContextMenu(fileName);
                    contextMenu.show(fileTable, event.getX(), event.getY());
                }
            }
        });

        // Добавление стандартных иконок файлов и директорий.
        fileTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = new JLabel();
                label.setHorizontalAlignment(SwingConstants.CENTER);

                if (value.toString().equals(SftpEntry.EntryType.DIRECTORY.toString()) || value.toString().equals(SftpEntry.EntryType.LINK.toString())) {
                    label.setIcon(UIManager.getIcon("FileView.directoryIcon"));
                } else {
                    label.setIcon(UIManager.getIcon("FileView.fileIcon"));
                }

                if (isSelected) {
                    label.setBackground(table.getSelectionBackground());
                    label.setForeground(table.getSelectionForeground());
                    label.setOpaque(true);
                }
                return label;
            }
        });

        updateFileList();

        return sftpPanel;
    }

    /**
     * Метод для обновления списка файлов в таблице после перехода в новую директорию.
     * Также записывает в currentFiles все названия для их возможной вставки при нажатии на TAB в терминале.
     */
    private void updateFileList() {
        currentFiles.clear();
        currentFilesIndex = 0;

        tableModel.setRowCount(0);
        try {
            tableModel.addRow(new Object[]{EMPTY_ENTRY.getEntryType().name(), EMPTY_ENTRY.getName(), EMPTY_ENTRY.getMTime()});

            List<SftpEntry> files = sftpService.listFiles();
            for (SftpEntry entry : files) {
                tableModel.addRow(new Object[]{entry.getEntryType().name(), entry.getName(), entry.getMTime()});
                currentFiles.add(entry.getName());
            }
        } catch (SftpException e) {
            JOptionPane.showMessageDialog(this, "Не удалось загрузить список файлов: " + e.getMessage());
        }
    }

    /**
     * Метод для создания PopUp меню при нажатии ПКМ по файлу для работы по SFTP или построения графика.
     *
     * @param fileName - файл для работы
     * @return возвращает меню
     */
    private JPopupMenu createFileContextMenu(String fileName) {
        JPopupMenu contextMenu = new JPopupMenu();

        //Кнопка "Загрузить"
        JMenuItem uploadedItem = new JMenuItem("Загрузить");
        uploadedItem.addActionListener(_ -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                Path localPath = fileChooser.getSelectedFile().toPath();
                try {
                    sftpService.uploadFile(localPath);
                    updateFileList();
                    JOptionPane.showMessageDialog(this, "Файл успешно загружен!");
                } catch (SftpException e) {
                    JOptionPane.showMessageDialog(this, "Ошибка при загрузку: " + e.getMessage());
                }
            }
        });
        contextMenu.add(uploadedItem);

        // Кнопка "Скачать"
        JMenuItem downloadItem = new JMenuItem("Скачать");
        downloadItem.addActionListener(_ -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File(fileName));
            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                Path localPath = fileChooser.getSelectedFile().toPath();
                try {
                    sftpService.downloadFile(Path.of(fileName), localPath);
                    JOptionPane.showMessageDialog(this, "Файл успешно скачан!");
                } catch (SftpException e) {
                    JOptionPane.showMessageDialog(this, "Ошибка при скачивании: " + e.getMessage());
                }
            }
        });
        contextMenu.add(downloadItem);

        // Кнопка "Удалить"
        JMenuItem deleteItem = new JMenuItem("Удалить");
        deleteItem.addActionListener(_ -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Вы уверены, что хотите удалить файл?", "Подтверждение удаления", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    sftpService.deleteFile(Path.of(fileName));
                    updateFileList();
                    JOptionPane.showMessageDialog(this, "Файл успешно удален!");
                } catch (SftpException e) {
                    JOptionPane.showMessageDialog(this, "Ошибка при удалении: " + e.getMessage());
                }
            }
        });
        contextMenu.add(deleteItem);

        //Кнопка "Запустить продляющуюся задачу"
        JMenuItem taskItem = new JMenuItem("Запустить продляющуюся задачу");
        taskItem.addActionListener(_ -> {
            TaskCreationWindow taskCreationWindow = new TaskCreationWindow(this, fileName, taskManager);
            taskCreationWindow.setVisible(true);
        });
        contextMenu.add(taskItem);

        // Кнопка "График"
        JMenuItem graphItem = new JMenuItem("График");
        graphItem.addActionListener(_ -> {
            try {
                sftpService.downloadFile(Path.of(fileName), TEMP_FILES_DIR);
            } catch (SftpException e) {
                JOptionPane.showMessageDialog(this, "Ошибка при открытии файла для построения графиков " + e.getMessage());
                return;
            }

            Map<LocalDate, Map<Double, Double>> parsedData;
            try {
                parsedData = PointParser.parsePointFile(TEMP_FILES_DIR + "\\" + fileName);
                Files.deleteIfExists(Path.of(TEMP_FILES_DIR + "\\" + fileName));
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Ошибка при открытии файла для построения графиков " + e.getMessage());
                return;
            }

            TemperatureData data = new TemperatureData(15, parsedData);

            SwingUtilities.invokeLater(() -> {
                ChartWindow chartWindow = new ChartWindow(data);
                chartWindow.setVisible(true);
            });

        });
        contextMenu.add(graphItem);

        return contextMenu;
    }

    /**
     * Метод для создания меню-бара с кнопками "Настройки", "Справка" и "Закрыть сессию".
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        Dimension buttonsDimension = new Dimension(100, 25);

        // Кнопка "Настройки"
        JButton settingsButton = new JButton("Настройки");
        settingsButton.addActionListener(_ -> {
            SettingsDialog settingsDialog = new SettingsDialog(this, settingsManager, this::applySettings);
            settingsDialog.setVisible(true);
        });
        settingsButton.setPreferredSize(buttonsDimension);
        menuBar.add(settingsButton);

        // Кнопка "Задачи"
        JButton tasksButton = new JButton("Задачи");
        tasksButton.addActionListener(_ -> {
            TaskManagerDialog taskManagerDialog = new TaskManagerDialog(this, taskManager);
            taskManagerDialog.setVisible(true);
        });
        tasksButton.setPreferredSize(buttonsDimension);
        menuBar.add(tasksButton);

//        // Кнопка "Справка"
//        JButton helpButton = new JButton("Справка");
//        helpButton.addActionListener(_ -> {
//        });
//        menuBar.add(helpButton);

        // Кнопка "Закрыть сессию"
        JButton closeSessionButton = new JButton("Закрыть сессию");
        closeSessionButton.addActionListener(_ -> {
            onClose();
            new SessionManagerWindow(SessionDataService.getInstance()).setVisible(true);
            dispose();
        });
        closeSessionButton.setPreferredSize(buttonsDimension);

        menuBar.add(closeSessionButton);

        return menuBar;
    }

    /**
     * Метод для отправки команды по SSH и вывода ответа с раскрашиванием текста
     */
    private void sendCommand() {
        String command = commandInputField.getText();

        String output;
        try {
            // Проверка на перемещение - для синхронизации
            if (command.startsWith("cd") && sync) {
                String path = command.split(" ")[1];
                sftpService.changeCurrentRemoteDir(Path.of(path));
                updateFileList();
            }

            // Проверка на текстовый редактор
            if (command.startsWith("nano") || command.startsWith("vim")) {
                openTextEditor(command);
                commandInputField.setText("");
                return;
            }

            output = sshService.sendCommand(command);
            output = output.replaceFirst(command, "").trim();
            output = output.replaceFirst("\\[0m", "");
        } catch (Exception ignored) {
            output = "Failed\n";
        }

        try {
            //Раскрашиваем input и output
            ColorUtils.appendColoredText(document, ">>> " + command + "\n", Color.BLUE);
            ColorUtils.appendAnsiColoredText(document, output);
        } catch (BadLocationException e) {
            JOptionPane.showMessageDialog(this, "Ошибка при создании файла " + e.getMessage());
        }

        commandInputField.setText("");
    }

    /**
     * Метод, открывающий окно для редактирования файла при попытке открыть его через vim/nano
     *
     * @param command - команда для открытия файла через vim/nano
     */
    private void openTextEditor(String command) {
        String fileName = command.split(" ")[1];
        String filePath = TEMP_FILES_DIR + "\\" + fileName;

        // Скачиваем файл для временного хранения
        try {
            sftpService.downloadFile(Path.of(fileName), Path.of(filePath));
        } catch (SftpException ex) {
            if (ex.id == 2) { // No such file
                try {
                    Files.createFile(Path.of(filePath));
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Ошибка при создании файла " + e.getMessage());
                    return;
                }
            } else {
                JOptionPane.showMessageDialog(this, "Ошибка при открытии файла для редактирования " + ex.getMessage());
                return;
            }
        }

        TextEditorDialog textEditorDialog = new TextEditorDialog(this, filePath, () -> {
            Path path = Path.of(filePath);

            try {
                sftpService.uploadFile(path);
            } catch (SftpException e) {
                JOptionPane.showMessageDialog(this, "Ошибка при загрузке файла обратно на сервер: " + e.getMessage());
            }

            try {
                Files.delete(path);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Ошибка при удалении временного файла: " + e.getMessage());
            }

            updateFileList();
        });

        textEditorDialog.setVisible(true);

    }
}