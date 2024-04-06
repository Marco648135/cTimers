package com.advancedraidtracker.ui.exportraids;

import com.advancedraidtracker.ui.BaseFrame;
import com.advancedraidtracker.ui.filters.ConfirmationDialog;
import com.advancedraidtracker.utility.datautility.RaidsManager;
import com.advancedraidtracker.utility.datautility.datapoints.Raid;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

import static com.advancedraidtracker.utility.UISwingUtility.*;

public class SaveRaids extends BaseFrame
{
    private final JTextField field;

    public SaveRaids(ArrayList<Raid> raids)
    {
        getContentPane().removeAll();
        setTitle("Save Raids");
        JPanel borderPanel = getTitledPanel("Save Raids");

        JPanel subPanel = getThemedPanel();
        subPanel.setLayout(new GridLayout(1, 4));
        field = getThemedTextField();
        subPanel.add(getThemedLabel("Raids Name: "));
        subPanel.add(field);
        JButton saveButton = getSaveButton(raids);
        subPanel.add(saveButton);
        borderPanel.add(subPanel);
        add(borderPanel);
        pack();
        setLocationRelativeTo(null);
        repaint();
    }

    private JButton getSaveButton(ArrayList<Raid> raids)
    {
        JButton saveButton = getThemedButton("Save");
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
