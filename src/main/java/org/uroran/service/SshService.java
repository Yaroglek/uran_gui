package org.uroran.service;

import com.jcraft.jsch.*;
import org.apache.poi.xssf.binary.XSSFBHyperlinksTable;
import org.uroran.models.SessionData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс для работы по SSH
 */
public class SshService {
    private final ChannelShell channelShell;
    private InputStream inputStream;
    private OutputStream outputStream;

    public SshService(Channel channel) {
        this.channelShell = (ChannelShell) channel;;
    }

    public void connect() throws JSchException, IOException {
        channelShell.connect();
        inputStream = channelShell.getInputStream();
        outputStream = channelShell.getOutputStream();
    }

    public void disconnect() {
        if (channelShell != null && channelShell.isConnected()) {
            channelShell.disconnect();
        }
    }

    /**
     * Метод для отправки команды и получения ответа
     */
    public String sendCommand(String command) throws Exception {
        if (channelShell == null || !channelShell.isConnected()) {
            throw new IllegalStateException("Shell channel is not connected.");
        }

        outputStream.write((command + "\n").getBytes());
        outputStream.flush();

        Thread.sleep(500);
        StringBuilder output = new StringBuilder();
        byte[] buffer = new byte[2048];
        while (inputStream.available() > 0) {
            int bytesRead = inputStream.read(buffer);
            output.append(new String(buffer, 0, bytesRead));
        }

        return output.toString();
    }
}
