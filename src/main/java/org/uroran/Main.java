package org.uroran;

import org.uroran.gui.SessionManagerWindow;
import org.uroran.service.SessionDataService;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        System.out.println(System.getProperty("java.home"));

        SwingUtilities.invokeLater(() -> new SessionManagerWindow(SessionDataService.getInstance()).setVisible(true));
    }
}