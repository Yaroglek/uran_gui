package org.uroran.service;

import com.jcraft.jsch.*;
import org.uroran.models.SftpEntry;

import java.nio.file.Path;
import java.util.*;

/**
 * Класс для работы по Sftp
 */
public class SftpService {
    private final ChannelSftp channelSftp;

    public SftpService(Channel channel) {
        this.channelSftp = (ChannelSftp) channel;
    }

    public void connect() throws JSchException, SftpException {
        channelSftp.connect();
    }

    public void disconnect() {
        if (channelSftp != null && channelSftp.isConnected()) {
            channelSftp.disconnect();
        }
    }

    public void uploadFile(Path localFile) throws SftpException {
        channelSftp.put(localFile.toString(), localFile.getFileName().toString());
    }

    public void downloadFile(Path remoteFilePath, Path localFilePath) throws SftpException {
        channelSftp.get(remoteFilePath.toString(), localFilePath.toString());
    }

    public void deleteFile(Path remotePath) throws SftpException {
        channelSftp.rm(remotePath.toString());
    }

    public void changeCurrentRemoteDir(Path remotePath) throws SftpException {
        channelSftp.cd(remotePath.toString());
    }

    public String getCurrentRemoteDir() throws SftpException {
        return channelSftp.pwd();
    }

    public List<SftpEntry> listFiles() throws SftpException {
        Vector<ChannelSftp.LsEntry> entries = channelSftp.ls(channelSftp.pwd());
        List<SftpEntry> files = new ArrayList<>();

        for (var entry : entries) {
            String filename = entry.getFilename();
            if (filename.startsWith(".")) continue;

            var fileAttrs = entry.getAttrs();
            String mTime = fileAttrs.getMtimeString();
            

            if (fileAttrs.isDir()) {
                files.add(new SftpEntry(filename, SftpEntry.EntryType.DIRECTORY, mTime));
            } else if (fileAttrs.isLink()) {
                files.add(new SftpEntry(filename, SftpEntry.EntryType.LINK, mTime));
            } else {
                files.add(new SftpEntry(filename, SftpEntry.EntryType.FILE, mTime));
            }
        }

        files.sort(Comparator.comparing(SftpEntry::getEntryType).thenComparing(SftpEntry::getName));

        return files;
    }
}
