package org.isf.medicalstock.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.ListIterator;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.apache.log4j.PropertyConfigurator;
import org.isf.generaldata.GeneralData;
import org.isf.generaldata.MessageBundle;
import org.isf.medicals.manager.MedicalBrowsingManager;
import org.isf.medicals.model.Medical;
import org.isf.medicalstock.manager.MovStockInsertingManager;
import org.isf.medicalstock.model.Lot;
import org.isf.medicalstock.model.Movement;
import org.isf.medstockmovtype.manager.MedicaldsrstockmovTypeBrowserManager;
import org.isf.medstockmovtype.model.MovementType;
import org.isf.utils.db.NormalizeString;
import org.isf.utils.exception.OHServiceException;
import org.isf.utils.exception.gui.OHServiceExceptionUtil;
import org.isf.utils.jobjects.BusyState;
import org.isf.utils.jobjects.RequestFocusListener;
import org.isf.utils.jobjects.TextPrompt;
import org.isf.utils.jobjects.TextPrompt.Show;
import org.isf.ward.manager.WardBrowserManager;
import org.isf.ward.model.Ward;
import org.isf.xmpp.gui.CommunicationFrame;
import org.isf.xmpp.manager.Interaction;

import com.toedter.calendar.JDateChooser;

public class MovStockMultipleDischarging extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String DATE_FORMAT_DD_MM_YYYY_HH_MM_SS = "dd/MM/yyyy HH:mm:ss"; //$NON-NLS-1$
	private static final String DATE_FORMAT_DD_MM_YYYY = "dd/MM/yyyy"; //$NON-NLS-1$
	private static final int CODE_COLUMN_WIDTH = 100;
	
	private JPanel mainPanel;
	private JTextField jTextFieldReference;
	private JTextField jTextFieldSearch;
	private JComboBox jComboBoxDischargeType;
	private JDateChooser jDateChooser;
	private JComboBox jComboBoxDestination;
	private JTable jTableMovements;
	private final String[] columnNames = { MessageBundle.getMessage("angal.medicalstock.multipledischarging.code"), MessageBundle.getMessage("angal.medicalstock.multipledischarging.description"), MessageBundle.getMessage("angal.medicalstock.multipledischarging.unitpack"), MessageBundle.getMessage("angal.medicalstock.multipledischarging.qty"), MessageBundle.getMessage("angal.medicalstock.multipledischarging.unitpack"), MessageBundle.getMessage("angal.medicalstock.multipledischarging.total"), MessageBundle.getMessage("angal.medicalstock.multipledischarging.lotnumberabb"), MessageBundle.getMessage("angal.medicalstock.multipledischarging.expiringdate")}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
	private final Class[] columnClasses = { String.class, String.class, Integer.class, Integer.class, String.class, Integer.class, String.class, String.class};
	private boolean[] columnEditable = { false, false, false, false, true, false, false, false};
	private int[] columnWidth = { 50, 100, 70, 50, 70, 50, 100, 80};
	private boolean[] columnResizable = { false, true, false, false, false, false, false, false};
	private boolean[] columnVisible = { true, true, true, true, true, true, !GeneralData.AUTOMATICLOT, !GeneralData.AUTOMATICLOT };
	private int[] columnAlignment = { SwingConstants.LEFT, SwingConstants.LEFT, SwingConstants.CENTER, SwingConstants.CENTER, SwingConstants.CENTER, SwingConstants.CENTER, SwingConstants.CENTER,
			SwingConstants.CENTER};
	private boolean[] columnBold = { false, false, false, false, false, true, false, false};
	private HashMap<String, Medical> medicalMap;
	private ArrayList<Integer> units;
	private JTableModel model;
	private String[] qtyOption = new String[] { MessageBundle.getMessage("angal.medicalstock.multipledischarging.units"), MessageBundle.getMessage("angal.medicalstock.multipledischarging.packet") }; //$NON-NLS-1$ //$NON-NLS-2$
	private final int UNITS = 0;
	private final int PACKETS = 1;
	private int optionSelected = UNITS;
	private JComboBox comboBoxUnits = new JComboBox(qtyOption);
	private JComboBox shareWith = null;
	private Interaction share;
	private ArrayList<Medical> pool = new ArrayList<Medical>();

	/**
	 * Launch the application.
	 * TODO: externalize strings
	 */
	public static void main(String[] args) {
		try {
			PropertyConfigurator.configure(new File("./rsc/log4j.properties").getAbsolutePath()); //$NON-NLS-1$
			GeneralData.getGeneralData();
			new MovStockMultipleDischarging(new JFrame());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean isAutomaticLot() {
		return GeneralData.AUTOMATICLOT;
	}

	private boolean isXmpp() {
		return GeneralData.XMPPMODULEENABLED;
	}
	
	/**
	 * Create the dialog.
	 */
	public MovStockMultipleDischarging(JFrame owner) {
		super(owner, true);
		initialize();
		initcomponents();
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setVisible(true);
		setLocationRelativeTo(null);
	}

	private void initialize() {
		MedicalBrowsingManager medMan = new MedicalBrowsingManager();
		
		ArrayList<Medical> medicals;
		try {
			medicals = medMan.getMedicals();
		} catch (OHServiceException e) {
			medicals = null;
			OHServiceExceptionUtil.showMessages(e);
		}

		medicalMap = new HashMap<String, Medical>();
		if (null != medicals) {
			for (Medical med : medicals) {
				String key = med.getProd_code();
				if (key.equals("")) key = med.getCode().toString(); //$NON-NLS-1$
				medicalMap.put(key, med);
			}
		}

		units = new ArrayList<Integer>();
	}
	
	private void initcomponents() {
		setTitle(MessageBundle.getMessage("angal.medicalstock.stockmovementinserting")); //$NON-NLS-1$
		add(getJPanelHeader(), BorderLayout.NORTH);
		add(getJMainPanel(), BorderLayout.CENTER);
		add(getJButtonPane(), BorderLayout.SOUTH);
		setPreferredSize(new Dimension(800, 600));
		pack();
		setLocationRelativeTo(null);
	}

	private JPanel getJButtonPane() {

		JPanel buttonPane = new JPanel();
		{
			JButton saveButton = new JButton(MessageBundle.getMessage("angal.common.save")); //$NON-NLS-1$
			saveButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					BusyState.setBusyState(MovStockMultipleDischarging.this, true);
					if (!checkAndPrepareMovements()) {
						BusyState.setBusyState(MovStockMultipleDischarging.this, false);
						return;
					}
					if (!save()) {
						BusyState.setBusyState(MovStockMultipleDischarging.this, false);
						return;
					}
					dispose();
				}
			});
			buttonPane.add(saveButton);
		}
		{
			JButton cancelButton = new JButton(MessageBundle.getMessage("angal.common.cancel")); //$NON-NLS-1$
			cancelButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					dispose();
				}
			});
			buttonPane.add(cancelButton);
		}
		{
			if(isXmpp())
			{
				shareWith=getShareUser();
				shareWith.setEnabled(false);
				buttonPane.add(shareWith);
			}
		}
	
		return buttonPane;
	}

	private JPanel getJMainPanel() {
		if (mainPanel == null) {
			mainPanel = new JPanel(new BorderLayout());
			mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
			mainPanel.add(getJTextFieldSearch(), BorderLayout.NORTH);
			mainPanel.add(getJScrollPane(), BorderLayout.CENTER);
		}
		return mainPanel;
	}

	private JTextField getJTextFieldSearch() {
		if (jTextFieldSearch == null) {
			jTextFieldSearch = new JTextField();
			jTextFieldSearch.setPreferredSize(new Dimension(300, 30));
			jTextFieldSearch.setHorizontalAlignment(SwingConstants.LEFT);
			jTextFieldSearch.setColumns(10);
			TextPrompt suggestion = new TextPrompt(MessageBundle.getMessage("angal.medicalstock.multipledischarging.typeacodeoradescriptionandpressenter"), jTextFieldSearch, Show.FOCUS_LOST); //$NON-NLS-1$
			{
				suggestion.setFont(new Font("Tahoma", Font.PLAIN, 14)); //$NON-NLS-1$
				suggestion.setForeground(Color.GRAY);
				suggestion.setHorizontalAlignment(JLabel.CENTER);
				suggestion.changeAlpha(0.5f);
				suggestion.changeStyle(Font.BOLD + Font.ITALIC);
			}
			jTextFieldSearch.addActionListener(new ActionListener() {
	
				@Override
				public void actionPerformed(ActionEvent e) {
					String text = jTextFieldSearch.getText();
					Medical med = null;
					if (medicalMap.containsKey(text)) {
						// Medical found
						med = medicalMap.get(text);
					} else {
						
						med = chooseMedical(text);
					}
					
					if (med != null) {
						
						if (isAutomaticLot() && isMedicalPresent(med))
							return;
						
						if (!isAvailable(med))
							return;
						
						// Quantity
						int qty = askQuantity(med);
						if (qty == 0) return;
						if (!checkQuantity(med, qty)) return;
	
						// Lot (PreparationDate && ExpiringDate)
						MovStockInsertingManager movBrowser = new MovStockInsertingManager();
						ArrayList<Lot> lots;
						try {
							lots = movBrowser.getLotByMedical(med);
						} catch (OHServiceException e1) {
							lots = null;
							OHServiceExceptionUtil.showMessages(e1);
						}
						Lot lot = null;
						if (!isAutomaticLot()) {
							lot = chooseLot(lots);
							if (lot == null) return;
							double lotQty = lot.getQuantity();
							if (qty > lotQty) {
								JOptionPane.showMessageDialog(MovStockMultipleDischarging.this, MessageBundle.getMessage("angal.medicalstock.movementquantityisgreaterthanthequantityof")); //$NON-NLS-1$
								return;
							}
						} else {
							lot = new Lot("", null, null); //$NON-NLS-1$
						}

						// Date
						GregorianCalendar date = new GregorianCalendar();
						date.setTime(jDateChooser.getDate());
						
						// RefNo
						String refNo = jTextFieldReference.getText();
						
						Movement movement = new Movement(med, (MovementType) jComboBoxDischargeType.getSelectedItem(), null, lot, date, qty, null, refNo);
						model.addItem(movement);
						units.add(PACKETS);
	
						jTextFieldSearch.setText(""); //$NON-NLS-1$
						jTextFieldSearch.requestFocus();
					}
				}
			});
		}
		return jTextFieldSearch;
	}

	protected boolean isAvailable(Medical med) {
		if (med.getTotalQuantity() == 0) {
			StringBuilder message = new StringBuilder();
			message.append(MessageBundle.getMessage("angal.medicalstock.multipledischarging.outofstock")); //$NON-NLS-1$
			message.append("\n").append(med.getDescription()); //$NON-NLS-1$
			JOptionPane.showMessageDialog(MovStockMultipleDischarging.this, message.toString());
			return false;
		}
		return true;
	}

	private boolean isMedicalPresent(Medical med) {
		ArrayList<Movement> movements = model.getMovements();
		for (Movement mov : movements) {
			if (mov.getMedical() == med) {
				StringBuilder message = new StringBuilder();
				message.append(MessageBundle.getMessage("angal.medicalstock.multipledischarging.alreadyinthisform")); //$NON-NLS-1$
				message.append("\n").append(med.getDescription()); //$NON-NLS-1$
				JOptionPane.showMessageDialog(MovStockMultipleDischarging.this, message.toString());
				return true;
			}
		}
		return false;
	}

	private JScrollPane getJScrollPane() {
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(getJTable());
		scrollPane.setPreferredSize(new Dimension(400, 450));
		return scrollPane;
	}

	private JTable getJTable() {
		if (jTableMovements == null) {
			model = new JTableModel();
			jTableMovements = new JTable(model);
			jTableMovements.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			jTableMovements.setRowHeight(24);
			jTableMovements.addKeyListener(new KeyListener() {
	
				@Override
				public void keyTyped(KeyEvent e) {
				}
	
				@Override
				public void keyReleased(KeyEvent e) {
				}
	
				@Override
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_DELETE) {
						int row = jTableMovements.getSelectedRow();
						model.removeItem(row);
					}
				}
			});
	
			for (int i = 0; i < columnNames.length; i++) {
				jTableMovements.getColumnModel().getColumn(i).setCellRenderer(new EnabledTableCellRenderer());
				jTableMovements.getColumnModel().getColumn(i).setMinWidth(columnWidth[i]);
				if (!columnResizable[i]) {
					jTableMovements.getColumnModel().getColumn(i).setResizable(columnResizable[i]);
					jTableMovements.getColumnModel().getColumn(i).setMaxWidth(columnWidth[i]);
				}
				if (!columnVisible[i]) {
					jTableMovements.getColumnModel().getColumn(i).setMinWidth(0);
					jTableMovements.getColumnModel().getColumn(i).setMaxWidth(0);
					jTableMovements.getColumnModel().getColumn(i).setWidth(0);
				}
			}
	
			TableColumn qtyOptionColumn = jTableMovements.getColumnModel().getColumn(4);
			qtyOptionColumn.setCellEditor(new DefaultCellEditor(comboBoxUnits));
			comboBoxUnits.setSelectedIndex(optionSelected);
		}
		return jTableMovements;
	}

	private JPanel getJPanelHeader() {
		JPanel headerPanel = new JPanel();
		GridBagLayout gbl_headerPanel = new GridBagLayout();
		gbl_headerPanel.columnWidths = new int[] { 0, 0, 0, 0, 0 };
		gbl_headerPanel.rowHeights = new int[] { 0, 0, 0, 0, 0 };
		gbl_headerPanel.columnWeights = new double[] { 0.0, 1.0, 0.0, 1.0, Double.MIN_VALUE };
		gbl_headerPanel.rowWeights = new double[] { 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE };
		headerPanel.setLayout(gbl_headerPanel);
		{
			JLabel jLabelDate = new JLabel(MessageBundle.getMessage("angal.common.date")); //$NON-NLS-1$
			GridBagConstraints gbc_jLabelDate = new GridBagConstraints();
			gbc_jLabelDate.anchor = GridBagConstraints.WEST;
			gbc_jLabelDate.insets = new Insets(5, 5, 5, 5);
			gbc_jLabelDate.gridx = 0;
			gbc_jLabelDate.gridy = 0;
			headerPanel.add(jLabelDate, gbc_jLabelDate);
		}
		{
			GridBagConstraints gbc_dateChooser = new GridBagConstraints();
			gbc_dateChooser.anchor = GridBagConstraints.WEST;
			gbc_dateChooser.insets = new Insets(5, 0, 5, 5);
			gbc_dateChooser.fill = GridBagConstraints.VERTICAL;
			gbc_dateChooser.gridx = 1;
			gbc_dateChooser.gridy = 0;
			headerPanel.add(getJDateChooser(), gbc_dateChooser);
		}
		{
			JLabel jLabelReferenceNo = new JLabel(MessageBundle.getMessage("angal.medicalstock.multipledischarging.referencenumber")); //$NON-NLS-1$
			GridBagConstraints gbc_jLabelReferenceNo = new GridBagConstraints();
			gbc_jLabelReferenceNo.anchor = GridBagConstraints.EAST;
			gbc_jLabelReferenceNo.insets = new Insets(5, 0, 5, 5);
			gbc_jLabelReferenceNo.gridx = 2;
			gbc_jLabelReferenceNo.gridy = 0;
			headerPanel.add(jLabelReferenceNo, gbc_jLabelReferenceNo);
		}
		{
			jTextFieldReference = new JTextField();
			GridBagConstraints gbc_jTextFieldReference = new GridBagConstraints();
			gbc_jTextFieldReference.insets = new Insets(5, 0, 5, 0);
			gbc_jTextFieldReference.fill = GridBagConstraints.HORIZONTAL;
			gbc_jTextFieldReference.gridx = 3;
			gbc_jTextFieldReference.gridy = 0;
			headerPanel.add(jTextFieldReference, gbc_jTextFieldReference);
			jTextFieldReference.setColumns(10);
		}
		{
			JLabel jLabelChargeType = new JLabel(MessageBundle.getMessage("angal.medicalstock.multipledischarging.dischargetype")); //$NON-NLS-1$
			GridBagConstraints gbc_jLabelChargeType = new GridBagConstraints();
			gbc_jLabelChargeType.anchor = GridBagConstraints.EAST;
			gbc_jLabelChargeType.insets = new Insets(0, 5, 5, 5);
			gbc_jLabelChargeType.gridx = 0;
			gbc_jLabelChargeType.gridy = 1;
			headerPanel.add(jLabelChargeType, gbc_jLabelChargeType);
		}
		{
			GridBagConstraints gbc_jComboBoxChargeType = new GridBagConstraints();
			gbc_jComboBoxChargeType.anchor = GridBagConstraints.WEST;
			gbc_jComboBoxChargeType.insets = new Insets(0, 0, 5, 5);
			gbc_jComboBoxChargeType.gridx = 1;
			gbc_jComboBoxChargeType.gridy = 1;
			headerPanel.add(getJComboBoxChargeType(), gbc_jComboBoxChargeType);
		}
		{
			JLabel jLabelSupplier = new JLabel(MessageBundle.getMessage("angal.medicalstock.multipledischarging.destination")); //$NON-NLS-1$
			GridBagConstraints gbc_jLabelSupplier = new GridBagConstraints();
			gbc_jLabelSupplier.anchor = GridBagConstraints.WEST;
			gbc_jLabelSupplier.insets = new Insets(0, 5, 0, 5);
			gbc_jLabelSupplier.gridx = 0;
			gbc_jLabelSupplier.gridy = 3;
			headerPanel.add(jLabelSupplier, gbc_jLabelSupplier);
		}
		{
			GridBagConstraints gbc_jComboBoxSupplier = new GridBagConstraints();
			gbc_jComboBoxSupplier.anchor = GridBagConstraints.WEST;
			gbc_jComboBoxSupplier.insets = new Insets(0, 0, 0, 5);
			gbc_jComboBoxSupplier.gridx = 1;
			gbc_jComboBoxSupplier.gridy = 3;
			headerPanel.add(getJComboBoxDestination(), gbc_jComboBoxSupplier);
		}
		return headerPanel;
	}

	private JComboBox getShareUser(){

		share= new Interaction();
		Collection<String> contacts = share.getContactOnline();
		contacts.add(MessageBundle.getMessage("angal.medicalstock.multipledischarging.sharealertwithnobody")); //$NON-NLS-1$
		shareWith= new JComboBox(contacts.toArray());
		shareWith.setSelectedItem(MessageBundle.getMessage("angal.medicalstock.multipledischarging.sharealertwithnobody")); //$NON-NLS-1$

		return shareWith;
	}
	
	private JDateChooser getJDateChooser() {
		if (jDateChooser == null) {
			jDateChooser = new JDateChooser(new Date());
			jDateChooser.setDateFormatString(DATE_FORMAT_DD_MM_YYYY_HH_MM_SS);
			jDateChooser.setPreferredSize(new Dimension(150, 24));
		}
		return jDateChooser;
	}

	private JComboBox getJComboBoxChargeType() {
		if (jComboBoxDischargeType == null) {
			jComboBoxDischargeType = new JComboBox();
			MedicaldsrstockmovTypeBrowserManager movMan = new MedicaldsrstockmovTypeBrowserManager();
			ArrayList<MovementType> movTypes;
			try {
				movTypes = movMan.getMedicaldsrstockmovType();
			} catch (OHServiceException e) {
				movTypes = null;
				OHServiceExceptionUtil.showMessages(e);
			}
			if (null != movTypes) {
				for (MovementType movType : movTypes) {
					if (movType.getType().equals("-")) //$NON-NLS-1$
						jComboBoxDischargeType.addItem(movType);
				}
			}
		}
		return jComboBoxDischargeType;
	}

	protected double askCost() {
		String input = JOptionPane.showInputDialog(MovStockMultipleDischarging.this, MessageBundle.getMessage("angal.medicalstock.multipledischarging.unitcost"), 0.); //$NON-NLS-1$
		double cost = 0.;
		if (input != null) {
			try {
				cost = Double.parseDouble(input);
				if (cost < 0)
					throw new NumberFormatException();
			} catch (NumberFormatException nfe) {
				JOptionPane.showMessageDialog(MovStockMultipleDischarging.this, MessageBundle.getMessage("angal.medicalstock.multipledischarging.pleaseinsertavalidvalue")); //$NON-NLS-1$
			}
		}
		return cost;
	}

	protected Lot askLot() {
		GregorianCalendar preparationDate = new GregorianCalendar();
		GregorianCalendar expiringDate = new GregorianCalendar();
		Lot lot = null;

		JTextField lotNameTextField = new JTextField(15);
		lotNameTextField.addAncestorListener(new RequestFocusListener());
		if (isAutomaticLot())
			lotNameTextField.setEnabled(false);
		TextPrompt suggestion = new TextPrompt(MessageBundle.getMessage("angal.medicalstock.multipledischarging.lotid"), lotNameTextField); //$NON-NLS-1$
		{
			suggestion.setFont(new Font("Tahoma", Font.PLAIN, 14)); //$NON-NLS-1$
			suggestion.setForeground(Color.GRAY);
			suggestion.setHorizontalAlignment(JLabel.CENTER);
			suggestion.changeAlpha(0.5f);
			suggestion.changeStyle(Font.BOLD + Font.ITALIC);
		}
		JDateChooser preparationDateChooser = new JDateChooser(new Date());
		{
			preparationDateChooser.setDateFormatString(DATE_FORMAT_DD_MM_YYYY);
		}
		JDateChooser expireDateChooser = new JDateChooser(new Date());
		{
			expireDateChooser.setDateFormatString(DATE_FORMAT_DD_MM_YYYY);
		}
		JPanel panel = new JPanel(new GridLayout(3, 2));
		panel.add(new JLabel(MessageBundle.getMessage("angal.medicalstock.multipledischarging.preparationdate"))); //$NON-NLS-1$
		panel.add(preparationDateChooser);
		panel.add(new JLabel(MessageBundle.getMessage("angal.medicalstock.multipledischarging.expiringdate"))); //$NON-NLS-1$
		panel.add(expireDateChooser);
		panel.add(new JLabel(MessageBundle.getMessage("angal.medicalstock.multipledischarging.lotnumberabb"))); //$NON-NLS-1$
		panel.add(lotNameTextField);

		int ok = JOptionPane.showConfirmDialog(MovStockMultipleDischarging.this, panel, MessageBundle.getMessage("angal.medicalstock.multipledischarging.lotinformations"), JOptionPane.OK_CANCEL_OPTION); //$NON-NLS-1$

		if (ok == JOptionPane.OK_OPTION) {
			String lotName = lotNameTextField.getText();
			expiringDate.setTime(expireDateChooser.getDate());
			preparationDate.setTime(preparationDateChooser.getDate());
			lot = new Lot(lotName, preparationDate, expiringDate);
		}
		return lot;
	}
	
	protected Medical chooseMedical(String text) {
		ArrayList<Medical> medList = new ArrayList<Medical>();
		for (Medical aMed : medicalMap.values()) {
			if (NormalizeString.normalizeContains(aMed.getDescription().toLowerCase(), text.toLowerCase()))
				medList.add(aMed);
		}
		Collections.sort(medList);
		Medical med = null;
		
		if (!medList.isEmpty()) {
			JTable medTable = new JTable(new StockMedModel(medList));
			medTable.getColumnModel().getColumn(0).setMaxWidth(CODE_COLUMN_WIDTH);
			medTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			JPanel panel = new JPanel();
			panel.add(new JScrollPane(medTable));
			
			int ok = JOptionPane.showConfirmDialog(MovStockMultipleDischarging.this, panel, MessageBundle.getMessage("angal.medicalstock.multipledischarging.chooseamedical"), JOptionPane.YES_NO_OPTION); //$NON-NLS-1$
			
			if (ok == JOptionPane.OK_OPTION) {
				int row = medTable.getSelectedRow();
				med = medList.get(row);
			}
			return med;
		}
		return null;
	}

	protected Lot chooseLot(ArrayList<Lot> lots) {
		Lot lot = null;
		if (!lots.isEmpty()) {
			stripeLots(lots);
			
			JTable lotTable = new JTable(new StockMovModel(lots));
			lotTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			JPanel panel = new JPanel(new BorderLayout());
			panel.add(new JLabel(MessageBundle.getMessage("angal.medicalstock.multipledischarging.selectalot")), BorderLayout.NORTH); //$NON-NLS-1$
			panel.add(new JScrollPane(lotTable), BorderLayout.CENTER);
			
			int ok = JOptionPane.showConfirmDialog(MovStockMultipleDischarging.this, panel, MessageBundle.getMessage("angal.medicalstock.multipledischarging.lotinformations"), JOptionPane.OK_CANCEL_OPTION); //$NON-NLS-1$

			if (ok == JOptionPane.OK_OPTION) {
				int row = lotTable.getSelectedRow();
				lot = lots.get(row);
			}
			return lot;
		} 
		return lot;
	}

	private void stripeLots(ArrayList<Lot> lots) {
		if (!lots.isEmpty()) {
			ArrayList<Movement> movements = model.getMovements();
			ListIterator<Lot> lotIterator = lots.listIterator();
			while (lotIterator.hasNext()) {
				Lot aLot = (Lot) lotIterator.next();
				for (Movement mov : movements) {
					if (aLot.getCode().equals(mov.getLot().getCode())) {
						int aLotQty = aLot.getQuantity();
						int newQty = aLotQty - mov.getQuantity();
						if (newQty == 0) lotIterator.remove();
						else {
							aLot.setQuantity(newQty);
							lotIterator.set(aLot);
						}
					}
				}
			}
		}
	}

	protected GregorianCalendar askExpiringDate() {
		GregorianCalendar date = new GregorianCalendar();
		JDateChooser expireDateChooser = new JDateChooser(new Date());
		{
			expireDateChooser.setDateFormatString(DATE_FORMAT_DD_MM_YYYY);
		}
		JPanel panel = new JPanel(new GridLayout(1, 2));
		panel.add(new JLabel(MessageBundle.getMessage("angal.medicalstock.multipledischarging.expiringdate"))); //$NON-NLS-1$
		panel.add(expireDateChooser);

		int ok = JOptionPane.showConfirmDialog(MovStockMultipleDischarging.this, panel, MessageBundle.getMessage("angal.medicalstock.multipledischarging.expiringdate"), JOptionPane.OK_CANCEL_OPTION); //$NON-NLS-1$

		if (ok == JOptionPane.OK_OPTION) {
			date.setTime(expireDateChooser.getDate());
		}
		return date;
	}

	private boolean checkQuantity(Medical med, double qty) {
		double totalQty = med.getTotalQuantity();
		double criticalLevel = med.getMinqty();
		
		// update remaining quantity with already inserted movements
		ArrayList<Movement> movements = model.getMovements();
		double usedQty = 0;
		for (Movement mov : movements) {
			if (mov.getMedical() == med) {
				usedQty+=mov.getQuantity();
			}
		}
		totalQty = totalQty - usedQty;
		
		if (qty > totalQty) {
			StringBuilder message = new StringBuilder();
			message.append(MessageBundle.getMessage("angal.medicalstock.multipledischarging.thequantityisnotavailable")); //$NON-NLS-1$
			message.append("\n").append(MessageBundle.getMessage("angal.medicalstock.multipledischarging.lyinginstock")).append(totalQty); //$NON-NLS-1$ //$NON-NLS-2$
			JOptionPane.showMessageDialog(MovStockMultipleDischarging.this, message.toString());
			return false;
		}
		
		if (totalQty - qty < criticalLevel) {
			StringBuilder message = new StringBuilder();
			message.append(MessageBundle.getMessage("angal.medicalstock.multipledischarging.youaregoingundercriticalevel")); //$NON-NLS-1$
			message.append(" (").append(criticalLevel).append(") "); //$NON-NLS-1$ //$NON-NLS-2$
			message.append(MessageBundle.getMessage("angal.medicalstock.multipledischarging.procedere")); //$NON-NLS-1$
			int ok = JOptionPane.showConfirmDialog(MovStockMultipleDischarging.this, message.toString());
			
			if (ok != JOptionPane.OK_OPTION) {
				return false;
			} else {
				if(isXmpp()) {
					shareWith.setEnabled(true);
					pool.add(med);
				}
				return true;
			}
		}
		return true;
	}
	
	protected int askQuantity(Medical med) {
		double totalQty = med.getTotalQuantity();
		
		// update remaining quantity with already inserted movements
		ArrayList<Movement> movements = model.getMovements();
		double usedQty = 0;
		for (Movement mov : movements) {
			if (mov.getMedical() == med) {
				usedQty+=mov.getQuantity();
			}
		}
		totalQty = totalQty - usedQty;
		
		StringBuilder message = new StringBuilder();
		message.append(med.toString());
		message.append("\n").append(MessageBundle.getMessage("angal.medicalstock.multipledischarging.lyinginstock")).append(totalQty); //$NON-NLS-1$ //$NON-NLS-2$
		
		String quantity = JOptionPane.showInputDialog(MovStockMultipleDischarging.this, message.toString(), 0);
		int qty = 0;
		if (quantity != null) {
			try {
				qty = Integer.parseInt(quantity);
				if (qty == 0)
					return 0;
				if (qty < 0)
					throw new NumberFormatException();
			} catch (NumberFormatException nfe) {
				JOptionPane.showMessageDialog(MovStockMultipleDischarging.this, MessageBundle.getMessage("angal.medicalstock.multipledischarging.pleaseinsertavalidvalue")); //$NON-NLS-1$
			}
		}
		
		return qty;
	}

	private JComboBox getJComboBoxDestination() {
		if (jComboBoxDestination == null) {
			jComboBoxDestination = new JComboBox();
			jComboBoxDestination.addItem(""); //$NON-NLS-1$
			WardBrowserManager wardMan = new WardBrowserManager();
			ArrayList<Ward> wards;
			try {
				wards = wardMan.getWards();
			}catch(OHServiceException e){
				wards = new ArrayList<Ward>();
				OHServiceExceptionUtil.showMessages(e);
			}
			for (Ward ward : wards) {
				if (GeneralData.INTERNALPHARMACIES) {
					if (ward.isPharmacy())
						jComboBoxDestination.addItem(ward);
				} else {
					jComboBoxDestination.addItem(ward);
				}
			}
		}
		return jComboBoxDestination;
	}

	public class JTableModel extends AbstractTableModel {

		private ArrayList<Movement> movements;
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public JTableModel() {
			movements = new ArrayList<Movement>();
		}
		
		public ArrayList<Movement> getMovements() {
			return movements;
		}

		public void removeItem(int row) {
			pool.remove(movements.get(row).getMedical());
			movements.remove(row);
			units.remove(row);
			fireTableDataChanged();
		}

		public void addItem(Movement movement) {
			movements.add(movement);
			fireTableDataChanged();
		}

		@Override
		public int getRowCount() {
			return movements.size();
		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public String getColumnName(int columnIndex) {
			return columnNames[columnIndex];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return columnClasses[columnIndex];
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnEditable[columnIndex];
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.table.TableModel#getValueAt(int, int)
		 */
		@Override
		public Object getValueAt(int r, int c) {
			Movement movement = movements.get(r);
			Medical medical = movement.getMedical();
			Lot lot = movement.getLot();
			String lotName = lot.getCode();
			int qty = movement.getQuantity();
			int ppp = medical.getPcsperpck().intValue();
			int option = units.get(r);
			int total = option == UNITS ? qty : (ppp == 0 ? qty : ppp * qty);
			if (c == -1) {
				return movement;
			} else if (c == 0) {
				return medical.getProd_code();
			} else if (c == 1) {
				return medical.getDescription();
			} else if (c == 2) {
				return ppp;
			} else if (c == 3) {
				return qty;
			} else if (c == 4) {
				return qtyOption[option];
			} else if (c == 5) {
				return total;
			} else if (c == 6) {
				return lotName.equals("") ? "AUTO" : lotName; //$NON-NLS-1$ //$NON-NLS-2$
			} else if (c == 7) {
				if (lot.getDueDate() != null)
					return format(lot.getDueDate());
				else 
					return "AUTO"; //$NON-NLS-1$
			} 
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.table.TableModel#setValueAt(java.lang.Object, int,
		 * int)
		 */
		@Override
		public void setValueAt(Object value, int r, int c) {
			Movement movement = movements.get(r);
			if (c == 0) {
				String key = String.valueOf(value);
				if (medicalMap.containsKey(key)) {
					movement.setMedical(medicalMap.get(key));
					movements.set(r, movement);
				}
			} else if (c == 3) {
				int qty = (Integer) value;
				if (checkQuantity(movement.getMedical(), qty));
					movement.setQuantity(qty);
			} else if (c == 4) {
				units.set(r, comboBoxUnits.getSelectedIndex());
			} else if (c == 7) {
				Lot lot = movement.getLot();
				try {
					lot.setDueDate(convertToDate((String) value));
				} catch (ParseException e) {
				}
			} 
			movements.set(r, movement);
			fireTableDataChanged();
		}
	}
	
	private boolean checkAndPrepareMovements() {
		boolean ok = true;
		MovStockInsertingManager manager = new MovStockInsertingManager();
		
		// Check the Date
		GregorianCalendar thisDate = new GregorianCalendar();
		thisDate.setTime(jDateChooser.getDate());
		GregorianCalendar lastDate;
		try {
			lastDate = manager.getLastMovementDate();
		} catch (OHServiceException e) {
			lastDate = null;
			OHServiceExceptionUtil.showMessages(e);
		}
		if (lastDate != null && thisDate.compareTo(lastDate) < 0) {
			JOptionPane.showMessageDialog(MovStockMultipleDischarging.this, MessageBundle.getMessage("angal.medicalstock.multipledischarging.datebeforelastmovement") + format(lastDate) + MessageBundle.getMessage("angal.medicalstock.multipledischarging.notallowed")); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}
		
		// Check the RefNo
		String refNo = jTextFieldReference.getText();
		if (refNo.equals("")) { //$NON-NLS-1$
			JOptionPane.showMessageDialog(MovStockMultipleDischarging.this, MessageBundle.getMessage("angal.medicalstock.multipledischarging.pleaseinsertareferencenumber")); //$NON-NLS-1$
			return false;
		} else {
			try {
				if (manager.refNoExists(refNo)) {
					JOptionPane.showMessageDialog(MovStockMultipleDischarging.this, MessageBundle.getMessage("angal.medicalstock.multipledischarging.theinsertedreferencenumberalreadyexists")); //$NON-NLS-1$
					return false;
				}
			} catch (OHServiceException e) {
				OHServiceExceptionUtil.showMessages(e);
				return false;
			}
		}
		
		// Check destination
		Object ward = jComboBoxDestination.getSelectedItem();
		if (ward instanceof String) {
			JOptionPane.showMessageDialog(MovStockMultipleDischarging.this, MessageBundle.getMessage("angal.medicalstock.multipledischarging.pleaseselectaward")); //$NON-NLS-1$
			return false;
		}
		
		// Check destination
		ArrayList<Movement> movements = model.getMovements();
		if (movements.isEmpty()) {
			JOptionPane.showMessageDialog(MovStockMultipleDischarging.this, MessageBundle.getMessage("angal.medicalstock.multipledischarging.noelementtosave")); //$NON-NLS-1$
			return false;
		}
		
		// Check and set all movements
		for (int i = 0; i < movements.size(); i++) {
			Movement mov = movements.get(i);
			mov.setWard((Ward) jComboBoxDestination.getSelectedItem());
			mov.setDate(thisDate);
			mov.setRefNo(refNo);
			mov.setType((MovementType) jComboBoxDischargeType.getSelectedItem());
			mov.getLot().setPreparationDate(thisDate);
		}
		return ok;
	}
	
	private boolean save() {
		boolean ok = true;
		ArrayList<Movement> movements = model.getMovements();
		int movSize = movements.size();
		MovStockInsertingManager movManager = new MovStockInsertingManager();
		int index;
		try {
			index = movManager.newMultipleDischargingMovements(movements);
			
			if (index < movSize) {
				jTableMovements.getSelectionModel().setSelectionInterval(index, index);
				ok = false;
			} else {
				if (isXmpp()) {
					if(shareWith.isEnabled()&& (!(((String)shareWith.getSelectedItem())==MessageBundle.getMessage("angal.medicalstock.multipledischarging.sharealertwithnobody")))){ //$NON-NLS-1$
						CommunicationFrame frame= (CommunicationFrame)CommunicationFrame.getFrame();
						for (Medical med : pool) {
							frame.sendMessage(MessageBundle.getMessage("angal.medicalstock.multipledischarging.alert") + med.getDescription() + MessageBundle.getMessage("angal.medicalstock.multipledischarging.isabouttoend"), (String)shareWith.getSelectedItem(), false); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
				}
			}
		} catch (OHServiceException e) {
			ok = false;
			OHServiceExceptionUtil.showMessages(e);
		}
		
		return ok;
	}

	public String format(GregorianCalendar gc) {
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_DD_MM_YYYY);
		return sdf.format(gc.getTime());
	}

	public GregorianCalendar convertToDate(String string) throws ParseException {
		GregorianCalendar date = new GregorianCalendar();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_DD_MM_YYYY);
		date.setTime(sdf.parse(string));
		return date;
	}

	class EnabledTableCellRenderer extends DefaultTableCellRenderer {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			setHorizontalAlignment(columnAlignment[column]);
			if (!columnEditable[column]) {
				cell.setBackground(Color.LIGHT_GRAY);
			}
			if (columnBold[column]) { 
				cell.setFont(new Font(null, Font.BOLD, 12));
			}
			return cell;
		}
	}
	
	class StockMovModel extends DefaultTableModel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private ArrayList<Lot> lotList;

		public StockMovModel(ArrayList<Lot> lots) {
			lotList = lots;
		}

		public int getRowCount() {
			if (lotList == null)
				return 0;
			return lotList.size();
		}

		public String getColumnName(int c) {
			if (c == 0) {
				return MessageBundle.getMessage("angal.medicalstock.lotid"); //$NON-NLS-1$
			}
			if (c == 1) {
				return MessageBundle.getMessage("angal.medicalstock.prepdate"); //$NON-NLS-1$
			}
			if (c == 2) {
				return MessageBundle.getMessage("angal.medicalstock.duedate"); //$NON-NLS-1$
			}
			if (c == 3) {
				return MessageBundle.getMessage("angal.medicalstock.quantity"); //$NON-NLS-1$
			}
			return ""; //$NON-NLS-1$
		}

		public int getColumnCount() {
			return 4;
		}

		public Object getValueAt(int r, int c) {
			if (c == -1) {
				return lotList.get(r);
			} else if (c == 0) {
				return lotList.get(r).getCode();
			} else if (c == 1) {
				return format(lotList.get(r).getPreparationDate());
			} else if (c == 2) {
				return format(lotList.get(r).getDueDate());
			} else if (c == 3) {
				return lotList.get(r).getQuantity();
			}
			return null;
		}

		@Override
		public boolean isCellEditable(int arg0, int arg1) {
			return false;
		}
	}
	
	class StockMedModel extends DefaultTableModel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private ArrayList<Medical> medList;

		public StockMedModel(ArrayList<Medical> meds) {
			medList = meds;
		}

		public int getRowCount() {
			if (medList == null)
				return 0;
			return medList.size();
		}

		public String getColumnName(int c) {
			if (c == 0) {
				return MessageBundle.getMessage("angal.medicals.code"); //$NON-NLS-1$
			}
			if (c == 1) {
				return MessageBundle.getMessage("angal.medicals.description"); //$NON-NLS-1$
			}
			return ""; //$NON-NLS-1$
		}

		public int getColumnCount() {
			return 2;
		}

		public Object getValueAt(int r, int c) {
			Medical med = medList.get(r);
			if (c == -1) {
				return med;
			} else if (c == 0) {
				return med.getProd_code();
			} else if (c == 1) {
				return med.getDescription();
			}
			return null;
		}

		@Override
		public boolean isCellEditable(int arg0, int arg1) {
			return false;
		}
	}
}
