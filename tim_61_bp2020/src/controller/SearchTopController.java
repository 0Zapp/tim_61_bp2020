package controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import gui.MainFrame;
import gui.SearchDialog;

public class SearchTopController implements ActionListener {
	@Override
	public void actionPerformed(ActionEvent e) {
		if(MainFrame.getInstance().getAppCore().getMainTable() != null) {
			SearchDialog dialog = new SearchDialog(MainFrame.getInstance(), "Search", true);
			dialog.setVisible(true);
		}
	}
}
