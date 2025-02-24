package org.uroran.service;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.Getter;
import org.uroran.models.SessionData;

/**
 * Класс для создания сессии и каналов.
 */
public class SessionManager {
    @Getter
    private final SessionData sessionData;
    private Session session;

    public SessionManager(SessionData sessionData) {
        this.sessionData = sessionData;
    }

    public synchronized void connect() throws JSchException {
        if (session == null || !session.isConnected()) {
            JSch jsch = new JSch();
            jsch.addIdentity(sessionData.getPathToKey(), sessionData.getPassPhrase());

            session = jsch.getSession(sessionData.getUser(), sessionData.getHost(), sessionData.getPort());
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
        }
    }

    public synchronized Channel openChannel(String type) throws JSchException {
        if (session == null || !session.isConnected()) {
            throw new IllegalStateException("Session is not connected.");
        }
        return session.openChannel(type);
    }

    public synchronized void disconnect() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }
}
