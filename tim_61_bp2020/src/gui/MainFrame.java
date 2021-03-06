package gui;

import app.AppCore;
import controller.DeleteTopController;
import controller.FaSController;
import controller.InsertTopController;
import controller.ReportTopController;
import controller.SearchTopController;
import controller.UpdateTopController;
import listener.MyListSelectionListener;
import observer.Notification;
import observer.Subscriber;
import observer.enums.NotificationCode;
import resource.DBNode;
import resource.DBNodeComposite;
import resource.enums.ConstraintType;
import resource.implementation.Attribute;
import resource.implementation.AttributeConstraint;
import resource.implementation.Entity;
import resource.implementation.InformationResource;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeNode;

import IRTree.IRTree;
import IRTree.IRTreeModel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame implements Subscriber {

	private static MainFrame instance = null;

	private AppCore appCore;
	private JTable jTableTop;

	private JScrollPane scroll;
	private JSplitPane split;
	private JSplitPane splitTable;
	private JScrollPane scrollTableTop;

	private JPanel topPanel;
	private JPanel bottomPanel;
	private JPanel buttonPanelTop;

	private JButton DeleteTop;
	private JButton InsertTop;
	private JButton UpdateTop;
	private JButton FaS;
	private JButton SearchTop;
	private JButton ReportTop;

	private IRTreeModel IRTreeModel;
	private IRTree IRTree;
	private String topTableName;

	private JTabbedPane tpane;
	private InformationResource ir;
	private String tabName;

	public String getTabName() {
		return tabName;
	}

	private MainFrame() {

	}

	public static MainFrame getInstance() {
		if (instance == null) {
			instance = new MainFrame();
			instance.initialise();
		}
		return instance;
	}

	private void initialise() {
		initialiseWorkspaceTree();
		initialiseGui();
		SwingUtilities.updateComponentTreeUI(this);
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		this.setVisible(true);
	}

	private void initialiseWorkspaceTree() {
		IRTree = new IRTree();
		IRTreeModel = new IRTreeModel(new InformationResource("loading...."));
		IRTree.setModel(IRTreeModel);
		SwingUtilities.updateComponentTreeUI(this);

	}

	private void initialiseGui() {

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		setTitle("Baze Projekat");

		scroll = new JScrollPane(IRTree);
		scroll.setPreferredSize(new Dimension(250, 250));
		scroll.setMinimumSize((new Dimension(200, 200)));
		scroll.setMaximumSize(new Dimension(300, 300));

		jTableTop = new JTable();
		jTableTop.setPreferredScrollableViewportSize(new Dimension(500, 300));
		jTableTop.setFillsViewportHeight(true);
		scrollTableTop = new JScrollPane(jTableTop);

		DeleteTop = new JButton("Delete");
		DeleteTop.addActionListener(new DeleteTopController());
		UpdateTop = new JButton("Update");
		UpdateTop.addActionListener(new UpdateTopController());
		InsertTop = new JButton("Insert");
		InsertTop.addActionListener(new InsertTopController());
		FaS = new JButton("Filter & Sort");
		FaS.addActionListener(new FaSController());
		SearchTop = new JButton("Search");
		SearchTop.addActionListener(new SearchTopController());
		ReportTop = new JButton("Report");
		ReportTop.addActionListener(new ReportTopController());

		buttonPanelTop = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		buttonPanelTop.add(SearchTop);
		buttonPanelTop.add(DeleteTop);
		buttonPanelTop.add(InsertTop);
		buttonPanelTop.add(UpdateTop);
		buttonPanelTop.add(FaS);
		buttonPanelTop.add(ReportTop);

		topPanel = new JPanel(new BorderLayout());
		topPanel.add(scrollTableTop, BorderLayout.CENTER);
		topPanel.add(buttonPanelTop, BorderLayout.SOUTH);

		tpane = new JTabbedPane(JTabbedPane.TOP);
		tpane.setPreferredSize(new Dimension(500, 300));

		bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.add(tpane, BorderLayout.CENTER);

		splitTable = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, bottomPanel);

		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll, splitTable);
		add(split, BorderLayout.CENTER);
		split.setDividerLocation(250);
		split.setOneTouchExpandable(true);

		this.pack();
		this.setLocationRelativeTo(null);
		topTableName = "loading..";

		jTableTop.getSelectionModel().addListSelectionListener(new MyListSelectionListener());

	}

	public void setAppCore(AppCore appCore) {
		this.appCore = appCore;
		this.appCore.addSubscriber(this);
		this.jTableTop.setModel(appCore.getTableModel());
		// this.jTableBottom.setModel(appCore.getTableModel());
	}

	@Override
	public void update(Notification notification) {

		if (notification.getCode() == NotificationCode.RESOURCE_LOADED) {
			IRTreeModel.setRoot((TreeNode) notification.getData());
			ir = (InformationResource) notification.getData();
			appCore.readDataFromTable((Entity) IRTreeModel.getChild(IRTreeModel.getRoot(), 0));

		} else if (notification.getCode() == NotificationCode.TABLE_NAME_CHANGE) {
			topTableName = (String) notification.getData();
		}

		else if (notification.getCode() == NotificationCode.DATA_UPDATED) {
			jTableTop.setModel((TableModel) notification.getData());
			// jTableBottom.setModel(appCore.getTableModel());

		} else if (notification.getCode() == NotificationCode.ROW_CHANGE) {
			// appCore.readDataFromTable(topTableName);
			appCore.readDataFromTable(null);
		} else if (notification.getCode() == NotificationCode.ADDED_TABLE) {
			JTable jTableBottom1 = new JTable();
			jTableBottom1.setPreferredScrollableViewportSize(new Dimension(500, 300));
			jTableBottom1.setFillsViewportHeight(true);
			jTableBottom1.setModel((TableModel) notification.getData());
			JScrollPane scrollTableBottom1 = new JScrollPane(jTableBottom1);
			tpane.add(tabName, scrollTableBottom1);
		}
		try {
			// jTableTop.setRowSelectionInterval(0, 0);
		} catch (Exception e) {
			System.out.println("loading...");
		}

	}

	public void addTables() {

		tpane.removeAll();
		List<DBNode> tables = ir.getChildren();

		Entity selected = null;
		for (DBNode d : tables) {
			if (d.getName() == topTableName) {
				selected = (Entity) d;
			}
		}

		if (selected != null) {

			for (DBNode a : selected.getChildren()) {
				if (((Attribute) a).getInRelationWith() != null) {
					Attribute related = ((Attribute) a).getInRelationWith();
					Entity table = (Entity) related.getParent();
					tabName = table.getName();
					appCore.addTable(table, null, null);
				}
			}

		}

	}

	public InformationResource getIr() {
		return ir;
	}

	public void setIr(InformationResource ir) {
		this.ir = ir;
	}

	public JTable getjTableTop() {
		return jTableTop;
	}

	public AppCore getAppCore() {
		return appCore;
	}

	public String[] getSelectedTop() {
		int row = jTableTop.getSelectedRow();
		if (row < 0) {
			jTableTop.setRowSelectionInterval(0, 0);
			row = jTableTop.getSelectedRow();
		}
		int columnCount = jTableTop.getColumnCount();
		String data[] = new String[1 + columnCount * 2];
		data[0] = topTableName;
		for (int i = 0; i < columnCount; i++) {
			data[i + 1] = jTableTop.getColumnName(i);
			data[i + 1 + columnCount] = (String) jTableTop.getValueAt(row, i);
		}

		return data;
	}

	public void removeAlltabs() {
		tpane.removeAll();

	}

	public String getTopTableName() {
		return topTableName;
	}

	public void setTopTableName(String topTableName) {
		this.topTableName = topTableName;
	}

	public void setjTableTop(JTable jTableTop) {
		this.jTableTop = jTableTop;
	}

	public void setTabName(String tabName) {
		this.tabName = tabName;
	}

}
