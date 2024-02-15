package com.TheatreTracker.ui.exportraids;

import com.TheatreTracker.RoomData;
import com.TheatreTracker.ui.BaseFrame;
import com.TheatreTracker.ui.filters.ConfirmationDialog;
import com.TheatreTracker.utility.datautility.RaidsManager;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class SaveRaids extends BaseFrame
{
    private final JTextField field;

    public SaveRaids(ArrayList<RoomData> raids)
    {
        getContentPane().removeAll();
        setTitle("Save Raids");
        JPanel borderPanel = new JPanel(new BorderLayout());
        borderPanel.setBorder(BorderFactory.createTitledBorder("Save Raids"));
        JPanel subPanel = new JPanel();
        subPanel.setLayout(new GridLayout(1, 4));
        field = new JTextField();
        subPanel.add(new JLabel("Raids Name: "));
        subPanel.add(field);
        JButton saveButton = getSaveButton(raids);
        subPanel.add(saveButton);
        borderPanel.add(subPanel);
        add(borderPanel);
        pack();
        setLocationRelativeTo(null);
        repaint();
    }

    private JButton getSaveButton(ArrayList<RoomData> raids)
    {
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e ->
        {
            if (RaidsManager.doesRaidExist(field.getText()))
            {
                ConfirmationDialog dialog = new ConfirmationDialog(field.getText(), raids, (JFrame) (SwingUtilities.getRoot((Component) e.getSource())), 1);
                dialog.open();
            } else
            {
                RaidsManager.saveRaids(field.getText(), raids);
                close();
            }
        });
        return saveButton;
    }
}
