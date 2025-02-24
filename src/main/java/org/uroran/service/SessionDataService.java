package org.uroran.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.uroran.models.SessionData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервисный класс для работы с сохраненными сессиями.
 */
public class SessionDataService {
    private static final Path SESSION_FILE = Paths.get(System.getProperty("user.home"), "sessions.yaml");
    private final YAMLMapper yamlMapper;

    private static final SessionDataService INSTANCE = new SessionDataService();

    public static SessionDataService getInstance() {
        return INSTANCE;
    }

    public SessionDataService() {
        this.yamlMapper = new YAMLMapper();
        try {
            if (!Files.exists(SESSION_FILE)) {
                Files.createFile(SESSION_FILE);
                yamlMapper.writeValue(SESSION_FILE.toFile(), new HashMap<String, SessionData>());
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при инициализации файла сессий", e);
        }
    }

    /**
     * Сохранение сессии.
     */
    public void saveSessionData(SessionData session) throws IOException {
        Map<String, SessionData> sessions = loadAllSessions();
        sessions.put(session.getName(), session);
        yamlMapper.writeValue(SESSION_FILE.toFile(), sessions);
    }

    /**
     * Получение сессии.
     */
    public SessionData loadSessionData(String sessionName) throws IOException {
        Map<String, SessionData> sessions = loadAllSessions();
        SessionData session = sessions.get(sessionName);
        if (session == null) {
            throw new RuntimeException("Сессия с именем '" + sessionName + "' не найдена");
        }
        return session;
    }

    /**
     * Удаление сессии.
     */
    public void deleteSession(String sessionName) throws IOException {
        Map<String, SessionData> sessions = loadAllSessions();
        if (sessions.remove(sessionName) != null) {
            yamlMapper.writeValue(SESSION_FILE.toFile(), sessions);
        } else {
            throw new RuntimeException("Сессия с именем '" + sessionName + "' не существует");
        }
    }

    /**
     * Получение имен всех сессий.
     */
    public List<String> loadSessionDataList() {
        return new ArrayList<>(loadAllSessions().keySet());
    }

    /**
     * Загрузка всех сессий из файла.
     */
    private Map<String, SessionData> loadAllSessions() {
        try {
            return yamlMapper.readValue(SESSION_FILE.toFile(), new TypeReference<>() {});
        } catch (IOException e) {
            return new HashMap<>();
        }
    }
}

