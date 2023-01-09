package editribe;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.*;
import javax.swing.table.*;

//*******************************************************************
//==================== EDITRIBE =====================================
//******************************************************************

public class Editribe extends JFrame
{
	private JButton convertButton = new JButton("Convert...");
	private JButton deleteAllButton = new JButton("Del All From");
	private JButton deleteButton = new JButton("Delete");
	private JButton exportAllButton = new JButton("Exp All");
	private JButton exportButton = new JButton("Export");
	private JButton loopButton = new JButton("Loop");
	private JButton mergeButton = new JButton("Merge...");
	private JButton playButton = new JButton("Play");
	private JButton quitButton = new JButton("Quit");
	private JButton saveButton = new JButton("Save");
	private JButton squinchButton = new JButton("Squinch");
	private JFrame frame = new JFrame();
	private JLabel findLabel = new JLabel(" Find: ");
	private JLabel freeSpaceLabel = new JLabel(" Seconds left: ");
	private JLabel freeSpaceLabel2 = new JLabel("---");
	private JLabel memoryLabel = new JLabel(" Memory left: ");
	private JLabel memoryLabel2 = new JLabel("---");
	private JLabel sampleLabel = new JLabel(" Total samples: ");
	private JLabel sampleLabel2 = new JLabel("---");
	private JScrollPane scrollPane;
	private JTextField findField = new JTextField();

	private Merge mergeWindow = null;
	private Convert convertWindow = null;
	private AllFile af = new AllFile();
	private Object currValue = "";

	final private static int MAX_COLUMNS = 12;
	final private static int MAX_ROWS = 902;

	final private static int sampleNumber = 0;
	final private static int sampleName = 1;
	final private static int category = 2;
	final private static int sampleLength = 3;
	final private static int sampleStart = 4;
	final private static int loopStart = 5;
	final private static int loopEnd = 6;
	final private static int looping = 7;
	final private static int stereo = 8;
	final private static int loudness = 9;
	final private static int sampleRate = 10;
	final private static int tuning = 11;

	private String[] categories = { "[Analog]", "[Audio In]", "Kick", "Snare", "Clap", "HiHat",
									"Cymbal", "Hits", "Shots", "Voice", "SE", "FX", "Tom", "Perc", "Phrase", "Loop",
									"PCM", "User"
								  };
	private String[] columnNames = { "#", "Sample Name", "Category", "Size", "Start", "Loop Start",
									 "Loop End", "Lp", "St", "Ld", "Bitrate", "Tun"
								   };
	private JComboBox<String> dropCatCombo = new JComboBox<String>(categories);

	private TableModel tm = new DefaultTableModel()
	{
		public boolean isCellEditable(int row, int column)
		{
			if ((af.sampleBank[row].kChunkStart != -1) && ((column == sampleName)
					|| (column > sampleLength))) return true;
			return false;
		}

		public String getColumnName(int column)
		{
			return columnNames[column];
		}

		public Object getValueAt(int row, int column)
		{
			if ((af.sampleBank[row].kChunkStart == -1) && (column != sampleNumber)) return null;
			switch (column)
			{
			case sampleNumber:
				return row2osc(row);
			case sampleName:
				return af.sampleBank[row].kSampleName;
			case category:
				return categories[af.sampleBank[row].kCategory];
			case sampleLength:
				return af.sampleBank[row].kSampleSize;
			case sampleStart:
				return af.sampleBank[row].kSampleStart;
			case loopStart:
				return af.sampleBank[row].kLoopStart;
			case loopEnd:
				return af.sampleBank[row].kLoopEnd;
			case looping:
				return af.sampleBank[row].kLooping;
			case stereo:
				return af.sampleBank[row].numChannels;
			case loudness:
				return af.sampleBank[row].kLoudness;
			case sampleRate:
				return af.sampleBank[row].sampleRate;
			case tuning:
				return af.sampleBank[row].kTuning;
			}
			return null;
		}

		public void setValueAt(Object value, int row, int column)
		{
			switch (column)
			{
			case sampleNumber:
				// do nothing
				break;
			case sampleName:
				af.sampleBank[row].kSampleName = (String)value;
				break;
			case category:
				af.sampleBank[row].kCategory = Arrays.asList(categories).indexOf((String)value);
				break;
			case sampleLength:
				af.sampleBank[row].kSampleSize = (Integer)value;
				break;
			case sampleStart:
				af.sampleBank[row].kSampleStart = (Integer)value;
				break;
			case loopStart:
				af.sampleBank[row].kLoopStart = (Integer)value;
				break;
			case loopEnd:
				af.sampleBank[row].kLoopEnd = (Integer)value;
				break;
			case looping:
				af.sampleBank[row].kLooping = (Integer)value;
				break;
			case stereo:
				af.sampleBank[row].numChannels = (Integer)value;
				break;
			case loudness:
				af.sampleBank[row].kLoudness = (Integer)value;
				break;
			case sampleRate:
				af.sampleBank[row].sampleRate = (Integer)value;
				break;
			case tuning:
				af.sampleBank[row].kTuning = (Integer)value;
				break;
			}
			fireTableCellUpdated(row, column);
		}

		public int getColumnCount()
		{
			return MAX_COLUMNS;
		}

		public int getRowCount()
		{
			return MAX_ROWS;
		}

		public Class getColumnClass(int column)
		{
			if ((column == sampleName) || (column == category)) return String.class;
			return Integer.class;
		}
	};

	private JTable table1 = new JTable()
	{
		public String getToolTipText(MouseEvent me)
		{
			String reply = null;
			switch(table1.columnAtPoint(me.getPoint()))
			{
			case tuning:
				reply = "<html>+/-14: 1 step<br>+/-17: 2 steps<br>+/-19: 3 steps(20#)<br>" +
						"+/-20: 4 steps(20b)<br>+/-22: 5 steps<br>+/-24: 6 steps<br>+/-28: " +
						"7 steps<br>+/-32: 8 steps<br>+/-36: 9 steps<br>+/-40: 10 steps<br>" +
						"+/-44: 11 steps<br>+/-48: 12 steps<br>+/-63: 24 steps</html>";
				break;
			case stereo:
				reply = "<html>0 = Mono<br>1 = Stereo</html>";
				break;
			case looping:
				reply = "<html>0 = Looping<br>1 = One-Shot</html>";
				break;
			case loudness:
				reply = "<html>0 = +0dB<br>1 = +12dB</html>";
				break;
			default:
				break;
			}
			return reply;
		}
	};

	public Editribe()
	{
		// TABLE
		table1.setModel(tm);
		table1.setPreferredScrollableViewportSize(new Dimension(705, 615));
		((DefaultTableCellRenderer)table1.getTableHeader().getDefaultRenderer())
		.setHorizontalAlignment(JLabel.CENTER);

		setWidth(sampleNumber, 35);
		setWidth(sampleName, 155);
		setWidth(category, 65);
		setWidth(sampleStart, 70);
		setWidth(sampleLength, 70);
		setWidth(loopStart, 70);
		setWidth(loopEnd, 70);
		setWidth(looping, 25);
		setWidth(stereo, 25);
		setWidth(loudness, 25);
		setWidth(sampleRate, 60);
		setWidth(tuning, 35);
		table1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table1.getTableHeader().setReorderingAllowed(false);
		table1.setShowGrid(true);
		table1.setGridColor(new Color(235, 235, 235));
		table1.setDragEnabled(true);

		table1.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent me)
			{
				int row2 = table1.rowAtPoint(me.getPoint());
				int col = table1.columnAtPoint(me.getPoint());
				if ((me.getClickCount() > 1) && (col == category)
						&& (af.sampleBank[row2].kChunkStart != -1))
				{
					af.sampleBank[row2].kCategory = dropCatCombo.getSelectedIndex();
					updateSelection();
				}
			}
		});

		ListSelectionListener lst = new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				updateSelection();
			}
		};
		table1.getSelectionModel().addListSelectionListener(lst);
		table1.getColumnModel().getSelectionModel().addListSelectionListener(lst);

		table1.getModel().addTableModelListener(new TableModelListener()
		{
			public void tableChanged(TableModelEvent e)
			{
				int row2 = e.getFirstRow();
				int col = e.getColumn();
				Object cv = table1.getValueAt(row2, col);
				switch (col)
				{
				case sampleName:
				{
					if (((String) cv).trim().length() == 0)
					{
						af.sampleBank[row2].kSampleName = (String)currValue;
						JOptionPane.showMessageDialog(null, "Name cannot be blank.");
					}
					else
					{
						int namelen = ((String) cv).length();
						if (namelen > 16)
						{
							cv = ((String) cv).substring(0, 16);
							JOptionPane.showMessageDialog(null, "Name has been trimmed.");
						}
						af.sampleBank[row2].kSampleName = (String) cv;
					}
					break;
				}
				case sampleStart:
				{
					if (((Integer)cv < 0) && ((Integer)cv > af.sampleBank[row2].kSampleSize))
					{
						af.sampleBank[row2].kSampleStart = (Integer)currValue;
						JOptionPane.showMessageDialog(null, "Sample Start must be in bounds.");
					}
					else
					{
						af.sampleBank[row2].kSampleStart = (Integer)cv;
					}
					break;
				}
				case loopStart:
				{
					if (((Integer)cv < 0) && ((Integer)cv > af.sampleBank[row2].kSampleSize))
					{
						af.sampleBank[row2].kLoopStart = (Integer)currValue;
						JOptionPane.showMessageDialog(null, "Loop Start must be in bounds.");
					}
					else
					{
						af.sampleBank[row2].kLoopStart = (Integer)cv;
					}
					break;
				}
				case loopEnd:
				{
					if (((Integer)cv < 0) && ((Integer)cv > af.sampleBank[row2].kLoopEnd))
					{
						af.sampleBank[row2].kLoopEnd = (Integer)currValue;
						JOptionPane.showMessageDialog(null, "Loop End must be in bounds.");
					}
					else
					{
						af.sampleBank[row2].kLoopEnd = (Integer)cv;
					}
					break;
				}
				case loudness:
				{
					if (((Integer)cv == 0) || ((Integer)cv == 1))
					{
						af.sampleBank[row2].kLoudness = (Integer)cv;
					}
					else
					{
						af.sampleBank[row2].kLoudness = (Integer)currValue;
						JOptionPane.showMessageDialog(null, "Loudness must be a 1 or 0.");
					}
					break;
				}
				case sampleRate:
				{
					if (((Integer)cv > 192000) || ((Integer)cv <= 0))
					{
						af.sampleBank[row2].sampleRate = (Integer)currValue;
						JOptionPane.showMessageDialog(null,
													  "Sample rate must be from 1 to 1962000.");
					}
					else
					{
						af.sampleBank[row2].sampleRate = (Integer)cv;
					}
					break;
				}
				case tuning:
				{
					if ((Integer)cv > -64 && (Integer)cv < 64)
					{
						af.sampleBank[row2].kTuning = (Integer)cv;
					}
					else
					{
						af.sampleBank[row2].kTuning = (Integer)currValue;
						JOptionPane.showMessageDialog(null, "Tuning must be -63 to 63.");
					}
					break;
				}
				default:
					break;
				}
			}
		});

		table1.setDropTarget(new DropTarget()
		{
			public synchronized void drop(DropTargetDropEvent dtde)
			{
				int col = table1.columnAtPoint(dtde.getLocation());
				int row2 = table1.rowAtPoint(dtde.getLocation());
				frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				try
				{
					dtde.acceptDrop(DnDConstants.ACTION_MOVE);
					if (dtde.getTransferable().isDataFlavorSupported(DataFlavor.javaFileListFlavor))
					{
						final List fileList = (List)dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
						Collections.sort(fileList);
						for (int i = 0; i < fileList.size(); i++)
						{
							File f = (File) fileList.get(i);
							String sfn = f.getName();
							if (sfn.toLowerCase().endsWith(".all"))
							{
								af.allFile = new File(f.getAbsolutePath());
								af.readFile();
								dtde.dropComplete(true);
								table1.setRowSelectionInterval(0, 0);
								deleteButton.setEnabled(true);
								deleteAllButton.setEnabled(true);
								squinchButton.setEnabled(true);
								mergeButton.setEnabled(true);
								saveButton.setEnabled(true);
								table1.requestFocusInWindow();
								frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
								updateSelection();
								return;
							}
							else if (!sfn.toLowerCase().endsWith(".wav") || f.isDirectory())
							{
								JOptionPane.showMessageDialog(null, sfn + " failed to load.");
							}
							else
							{
								if (!deleteAllButton.isEnabled()) return;
								Sample s = new Sample();
								if (s.loadSample(f, 0) < 0)
								{
									JOptionPane.showMessageDialog(null, sfn + " is not a valid WAV.");
								}
								else
								{
									s.kSampleName = sfn.substring(0, sfn.indexOf('.'));
									if (s.kSampleName.length() > 16)
									{
										s.kSampleName = s.kSampleName.substring(0, 16);
									}
									s.kChunkStart = 0;
									s.kCategory = dropCatCombo.getSelectedIndex();
									af.sampleBank[row2] = s;
									updateChunkStarts();
									row2++;
									if (row2 >= MAX_ROWS)
									{
										row2 = MAX_ROWS - 1;
										break;
									}
									table1.setRowSelectionInterval(row2, row2);
								}
							}
						}
					}
					else if (dtde.getTransferable().isDataFlavorSupported(DataFlavor.stringFlavor))
					{
						String drop = (String) dtde.getTransferable().getTransferData(DataFlavor.stringFlavor);
						String[] r = drop.split("\\t");
						int oldRow = osc2row(Integer.parseInt(r[0]));
						Sample s = af.sampleBank[oldRow];
						af.sampleBank[oldRow] = af.sampleBank[row2];
						af.sampleBank[row2] = s;
					}
				}
				catch (Exception ex) {
					throwError(ex.toString());
				}
				dtde.dropComplete(true);
				table1.setRowSelectionInterval(row2, row2);
				updateSelection();
				frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		});
		scrollPane = new JScrollPane(table1);

		dropCatCombo.setSelectedIndex(17); // user cat

		// BUTTON ACTIONS
		playButton.setFocusable(false);
		playButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent ae)
			{
				int row2 = getRow();
				if (af.sampleBank[row2].kChunkStart > 0)
				{
					int length = af.sampleBank[row2].kSampleSize;
					int st	 = af.sampleBank[row2].numChannels + 1;
					int srate  = af.sampleBank[row2].sampleRate;
					int bps	= af.sampleBank[row2].bitsPerSample;
					if (srate > 48000) srate = 48000;  /// Clip max
					AudioFormat auf = new AudioFormat(srate, bps, st, true, false);
					try
					{
						Clip clip = AudioSystem.getClip();
						clip.open(auf, af.sampleBank[row2].kSampleChunk, 0, length);
						clip.start();
					}
					catch (Exception ex) {
						throwError(ex.toString());
					}
				}
			}
		});

		mergeButton.setFocusable(false);
		mergeButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent ae)
			{
				if (mergeWindow == null) mergeWindow = new Merge();
				mergeWindow.mShow();
			}
		});

		convertButton.setFocusable(false);
		convertButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent ae)
			{
				if (convertWindow == null) convertWindow = new Convert();
				convertWindow.cShow();
			};
		});

		deleteButton.setFocusable(false);
		deleteButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent ae)
			{
				int row2 = getRow();
				af.sampleBank[row2] = new Sample();
				incRow(row2);
			}
		});

		deleteAllButton.setFocusable(false);
		deleteAllButton.setBackground(new Color(255, 128, 128));
		deleteAllButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent ae)
			{
				if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null, "Are you sure?"))
				{
					int row2 = getRow();
					for (int i = row2; i < MAX_ROWS; i++) af.sampleBank[i] = new Sample();
					updateSelection();
				}
			}
		});

		squinchButton.setFocusable(false);
		squinchButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent ae)
			{
				for (int i = 0; i < MAX_ROWS - 1; i++)
				{
					if (af.sampleBank[i].kSampleName.equals(""))
					{
						for (int j = i + 1; j < MAX_ROWS; j++)
						{
							if (!af.sampleBank[j].kSampleName.equals(""))
							{
								af.sampleBank[i] = af.sampleBank[j];
								af.sampleBank[j] = new Sample();
								break;
							}
						}
					}
				}
				updateSelection();
			}
		});

		saveButton.setFocusable(false);
		saveButton.setBackground(new Color(128, 255, 128));
		saveButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent ae)
			{
				frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				af.writeFile();
				frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		});

		quitButton.setFocusable(false);
		quitButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent ae)
			{
				if (saveButton.isEnabled())
				{
					int reply = JOptionPane.showConfirmDialog(null, "Save before Quit?");
					if (reply == JOptionPane.YES_OPTION) af.writeFile();
					else if (reply == JOptionPane.CANCEL_OPTION) return;
				}
				System.exit(0);
			}
		});

		loopButton.setFocusable(false);
		loopButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent ae)
			{
				int s;
				int row2 = getRow();
				if ((Integer) af.sampleBank[row2].kLooping == 0)
				{
					s = 1;
					af.sampleBank[row2].kLoopStart = af.sampleBank[row2].kSampleSize -
													 (2 * (af.sampleBank[row2].numChannels + 1));
					af.sampleBank[row2].kLoopEnd = af.sampleBank[row2].kLoopStart;
					af.sampleBank[row2].kLooping = 1;
					loopButton.setText("Make Loop");
				}
				else
				{
					s = 0;
					af.sampleBank[row2].kLoopStart = 0;
					af.sampleBank[row2].kLooping = 0;
					loopButton.setText("Unloop");
				}
				incRow(row2);
			}
		});

		exportButton.setFocusable(false);
		exportButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent ae)
			{
				int row2 = getRow();
				exportWave(row2);
				incRow(row2);
			}
		});

		exportAllButton.setFocusable(false);
		exportAllButton.setBackground(new Color(128, 128, 255));
		exportAllButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent ae)
			{
				if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null, "Are you sure?"))
				{
					for (int i = 0; i < MAX_ROWS; i++)
					{
						exportWave(i);
					}
				}
			}
		});

		findField.setPreferredSize(new Dimension(50, 27));
		findField.addKeyListener(new KeyAdapter()
		{
			public void keyPressed(final KeyEvent ke)
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						String f = findField.getText().toUpperCase();
						if (f.length() > 0)
						{
							int startHere = 0;
							int row2 = getRow();
							if (ke.getKeyChar() == KeyEvent.VK_ENTER) startHere = row2 + 1;
							if (startHere < MAX_ROWS)
							{
								for (int i = startHere; i < MAX_ROWS; i++)
								{
									if (af.sampleBank[i].kSampleName.toUpperCase().contains(f))
									{
										table1.setRowSelectionInterval(i, i);
										break;
									}
								}
							}
						}
						else
						{
							table1.setRowSelectionInterval(0, 0);
						}
						table1.scrollRectToVisible(table1.getCellRect(table1.getSelectedRow(), 0, true));
					}
				});
			}
		});

		// FRAME
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("EdiTribe");
		frame.setLayout(new FlowLayout());
		frame.add(sampleLabel);
		frame.add(sampleLabel2);
		frame.add(freeSpaceLabel);
		frame.add(freeSpaceLabel2);
		frame.add(memoryLabel);
		frame.add(memoryLabel2);
		frame.add(findLabel);
		frame.add(findField);
		frame.add(convertButton);
		frame.add(saveButton);
		frame.add(quitButton);
		frame.add(scrollPane);
		frame.add(playButton);
		frame.add(dropCatCombo);
		frame.add(deleteAllButton);
		frame.add(deleteButton);
		frame.add(squinchButton);
		frame.add(exportButton);
		frame.add(exportAllButton);
		frame.add(loopButton);
		frame.add(mergeButton);
		frame.pack();
		frame.setSize(740, 740);
		frame.setResizable(false);
		frame.setVisible(true);

		updateSelection();
		deleteButton.setEnabled(false);
		deleteAllButton.setEnabled(false);
		squinchButton.setEnabled(false);
		mergeButton.setEnabled(false);
		saveButton.setEnabled(false);
	}

	private void updateSelection()
	{
		int row2 = getRow();
		int col = getCol();
		currValue = table1.getValueAt(row2, col);
		if (af.sampleBank[row2].kChunkStart == -1)
		{
			loopButton.setEnabled(false);
			exportButton.setEnabled(false);
			exportAllButton.setEnabled(false);
			playButton.setEnabled(false);
		}
		else
		{
			loopButton.setEnabled(true);
			exportButton.setEnabled(true);
			exportAllButton.setEnabled(true);
			playButton.setEnabled(true);
			if (af.sampleBank[row2].kLooping == 0) loopButton.setText("Unloop");
			else loopButton.setText(" Loop ");
		}

		int tr = 4096;
		int totalSamples = 0;
		for (int i = 0; i < MAX_ROWS; i++)
		{
			if (af.sampleBank[i].kChunkStart > 0)
			{
				totalSamples++;
				tr += 1232 + af.sampleBank[i].kSampleSize;
			}
		}
		int spaceRemaining = 26214396 - tr;
		if (spaceRemaining < 0)
		{
			freeSpaceLabel2.setForeground(Color.red);
			memoryLabel2.setForeground(Color.red);
		}
		else
		{
			freeSpaceLabel2.setForeground(Color.black);
			memoryLabel2.setForeground(Color.black);
		}
		DecimalFormat twoPlaces = new DecimalFormat("##0.00");
		freeSpaceLabel2.setText(twoPlaces.format(((float) spaceRemaining - 1232.f) / 96000.0f));
		memoryLabel2.setText(Integer.toString(spaceRemaining));
		sampleLabel2.setText(totalSamples + "	");

		table1.repaint();
	}

	private void exportWave(int row2)
	{
		if (!af.sampleBank[row2].kSampleName.equals(""))
		{
			String expName = af.sampleBank[row2].kSampleName.replaceAll("\\W+", "_");
			expName = row2osc(row2) + " " + expName.trim() + ".wav";
			File file = new File(expName);
			if (file.isFile()) file.delete();
			try
			{
				af.sampleBank[row2].saveSample(file, row2);
			}
			catch (Exception ex) {
				throwError(ex.toString());
			}
		}
	}

	private void setWidth(int column, int width)
	{
		table1.getColumnModel().getColumn(column).setMinWidth(width);
		table1.getColumnModel().getColumn(column).setPreferredWidth(width);
		table1.getColumnModel().getColumn(column).setMaxWidth(width);
	}

	private void updateChunkStarts()
	{
		int nextStart = 4096;
		for (int i = 0; i < MAX_ROWS; i++)
		{
			if (af.sampleBank[i].kChunkStart != -1)
			{
				af.sampleBank[i].kChunkStart = nextStart;
				nextStart += af.sampleBank[i].kSampleSize + 1232;
			}
		}
	}

	private int getCol()
	{
		int col = table1.getSelectedColumn();
		if (col < 0) col = 0;
		return col;
	}

	private int getRow()
	{
		int row2 = table1.getSelectedRow();
		if (row2 < 0) row2 = 0;
		return row2;
	}

	private void incRow(int row2)
	{
		row2++;
		if (row2 >= MAX_ROWS) row2 = MAX_ROWS - 1;
		table1.setRowSelectionInterval(row2, row2);
		table1.scrollRectToVisible(new Rectangle(table1.getCellRect(table1.getSelectedRow(), 0, true)));
		updateSelection();
	}

	private Integer row2osc(int r)
	{
		if (r < 403) return r + 19;
		else		 return r + 98;
	}

	private Integer osc2row(int o)
	{
		if (o < 422) return o - 19;
		else		 return o - 98;
	}

	private short osc2import(int r)
	{
		short bump = 0;
		if	  (r < 55)	bump = 31;
		else if (r < 81)	bump = 32;
		else if (r < 93)	bump = 33;
		else if (r < 102)   bump = 34;
		else if (r < 147)   bump = 35;
		else if (r < 148)   bump = 36;
		else if (r < 149)   bump = 37;
		else if (r < 151)   bump = 38;
		else if (r < 422)   bump = 39;
		else				bump = 49;
		return (short) (r + bump);
	}

	/********************************************************************
	*==================== CONVERT ======================================*
	********************************************************************/

	private class Convert
	{
		private String[] sampleRates = { "11025", "16000", "22050", "32000", "44100", "48000"};
		private String[] trimLevels = { "-20dB", "-30dB", "-40dB", "-50dB", "-60dB", "-70dB", "-80dB", "-90dB", "-oodB"};

		JFrame cFrame = new JFrame("Sample Converter");
		JButton sourceFolderButton = new JButton("Set Source Folder");
		JLabel sampleRateLabel = new JLabel("Sample Rate:");
		JComboBox sampleRateCombo = new JComboBox(sampleRates);
		JCheckBox averageChannelsCheckBox = new JCheckBox("Average L & R Channels?", false);
		JLabel trimLevelBeginLabel = new JLabel("Trim Silence from Beginning:");
		JComboBox trimLevelBeginCombo = new JComboBox(trimLevels);
		JCheckBox zeroBeginCheckBox = new JCheckBox("Begin With Zero Crossing?", false);
		JLabel trimLevelEndLabel = new JLabel("Trim Silence from End:");
		JComboBox trimLevelEndCombo = new JComboBox(trimLevels);
		JCheckBox zeroEndCheckBox = new JCheckBox("End With Zero Crossing?", false);
		JButton cancelButton = new JButton("Cancel");
		JButton convertButton = new JButton("Convert");
		JFileChooser fcSource = new JFileChooser();
		JFileChooser fcDest = new JFileChooser();

		private Convert()
		{
			cancelButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent ae)
				{
					cFrame.setVisible(false);
				}
			});
			convertButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent ae)
				{
					cFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
					throwError(dateFormat.format(new Date()) + ": Conversion begin...");
					String nd = fcSource.getSelectedFile().toString() + "-converted";
					File newDir = new File(nd);
					cConvert(fcSource.getSelectedFile(), newDir);
					throwError(dateFormat.format(new Date()) + ": Conversion complete!");
					cFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				}
			});

			fcSource.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			sourceFolderButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent ae)
				{
					if (fcSource.showOpenDialog(cFrame) == JFileChooser.APPROVE_OPTION)
					{
						sourceFolderButton.setText(fcSource.getSelectedFile().getName());
						convertButton.setEnabled(true);
					}
				}
			});

			sampleRateCombo.setSelectedItem("44100");
			trimLevelBeginCombo.setSelectedItem("-oodB");
			trimLevelEndCombo.setSelectedItem("-oodB");
			convertButton.setEnabled(false);

			cFrame.setLayout(new GridLayout(0, 1));
			cFrame.add(sourceFolderButton);
			cFrame.add(sampleRateLabel);
			cFrame.add(sampleRateCombo);
			cFrame.add(averageChannelsCheckBox);
			cFrame.add(trimLevelBeginLabel);
			cFrame.add(trimLevelBeginCombo);
			cFrame.add(zeroBeginCheckBox);
			cFrame.add(trimLevelEndLabel);
			cFrame.add(trimLevelEndCombo);
			cFrame.add(zeroEndCheckBox);
			cFrame.add(cancelButton);
			cFrame.add(convertButton);
			cFrame.setSize(210, 300);
			cFrame.setLocation(200, 200);
			cFrame.setResizable(false);
		}

		private void cConvert(File ff, File df)
		{
			if (!df.exists())
			{
				df.mkdir();
			}
			File[] subs = ff.listFiles();
			for (File f : subs)
			{
				if (f.isDirectory())
				{
					String nd = df.toString() + System.getProperty("file.separator") + f.getName();
					File newDir = new File(nd);
					cConvert(f, newDir);
				}
				else if (f.getName().toLowerCase().endsWith(".wav"))
				{
					Sample s = new Sample();
					s.kSaveKorg = false;
					try
					{
						if (s.loadSample(f, 0) != -1)
						{
							if (mangle(s))
							{
								File newF = new File(df.toString() + System.getProperty("file.separator") + f.getName());
								s.saveSample(newF, 0);
							}
							else
							{
								throwError(f.toString() + " failed conversion.");
							}
						}
					}
					catch (Exception ex) {
						throwError(ex.toString());
					}
				}
			}
			System.gc();
		}

		private boolean mangle(Sample s)
		{
			int numSamples = (s.kSampleSize / (s.numChannels + 1)) / ((s.bitsPerSample + 7) / 8);
			if (numSamples < 1) return false;
			double samples[] = new double[numSamples];
			int ctr = 0;
			// Collect samples and flatten to mono, throw away right channel
			if (s.audioFormat == 1)
			{
				if (s.bitsPerSample == 16)
				{
					byte[] bb = new byte[2];
					for (int i = 0; i < numSamples; i++)
					{
						bb[0] = s.kSampleChunk[ctr + 1];
						bb[1] = s.kSampleChunk[ctr + 0];
						samples[i] = (ByteBuffer.wrap(bb).getShort() / 32768.0);
						if (s.numChannels == 0)
						{
							ctr += 2;
						}
						else
						{
							if (averageChannelsCheckBox.isSelected())
							{
								bb[0] = s.kSampleChunk[ctr + 3];
								bb[1] = s.kSampleChunk[ctr + 2];
								samples[i] = (samples[i] + (ByteBuffer.wrap(bb).getShort() / 32768.0)) * 0.5;
							}
							ctr += 4;
						}
					}
				}
				else if (s.bitsPerSample == 24)
				{
					byte[] bb = new byte[4];
					for (int i = 0; i < numSamples; i++)
					{
						bb[0] = s.kSampleChunk[ctr + 2];
						bb[1] = s.kSampleChunk[ctr + 1];
						bb[2] = s.kSampleChunk[ctr + 0];
						if (bb[2] < 0) bb[3] = -1;
						else bb[3] = 0;
						samples[i] = (ByteBuffer.wrap(bb).getInt() / 8388608.0);
						if (s.numChannels == 0)
						{
							ctr += 3;
						}
						else
						{
							if (averageChannelsCheckBox.isSelected())
							{
								bb[0] = s.kSampleChunk[ctr + 5];
								bb[1] = s.kSampleChunk[ctr + 4];
								bb[2] = s.kSampleChunk[ctr + 3];
								if (bb[2] < 0) bb[3] = -1;
								else bb[3] = 0;
								samples[i] = (samples[i] + (ByteBuffer.wrap(bb).getInt() / 8388608.0)) * 0.5;
							}
							ctr += 6;
						}
					}
				}
			}
			else if (s.audioFormat == 3)  // 32-bit float
			{
				byte[] bb = new byte[4];
				for (int i = 0; i < numSamples; i++)
				{
					bb[0] = s.kSampleChunk[ctr + 3];
					bb[1] = s.kSampleChunk[ctr + 2];
					bb[2] = s.kSampleChunk[ctr + 1];
					bb[3] = s.kSampleChunk[ctr + 0];
					samples[i] = ByteBuffer.wrap(bb).getFloat();
					if (s.numChannels == 0)
					{
						ctr += 4;
					}
					else
					{
						if (averageChannelsCheckBox.isSelected())
						{
							bb[0] = s.kSampleChunk[ctr + 7];
							bb[1] = s.kSampleChunk[ctr + 6];
							bb[2] = s.kSampleChunk[ctr + 5];
							bb[3] = s.kSampleChunk[ctr + 4];
							samples[i] = (samples[i] + ByteBuffer.wrap(bb).getFloat()) * 0.5;
						}
						ctr += 8;
					}
				}
			}
			else return false;

			// Normalize and Remove DC
			double normal = 0.0;
			double dc = 0.0;
			for (int i = 0; i < numSamples; i++) dc += samples[i];
			dc /= numSamples;
			for (int i = 0; i < numSamples; i++)
			{
				samples[i] -= dc;
				normal = Math.max(normal, Math.abs(samples[i]));
			}
			if (normal > 0.0000001)
			{
				normal = 1.0 / normal;
				for (int i = 0; i < numSamples; i++)
				{
					samples[i] *= normal;
				}
			}

			// Trim Ends and update numSamples
			int beginSample = 0;
			int endSample = numSamples - 1;
			if (trimLevelBeginCombo.getSelectedIndex() < 8)
			{
				double beginLevel = Math.sqrt(Math.pow(10, -1 * (trimLevelBeginCombo.getSelectedIndex() + 2)));
				while ((beginSample < numSamples - 2) && (Math.abs(samples[beginSample]) < beginLevel))
				{
					beginSample++;
				}
				if (zeroBeginCheckBox.isSelected())
				{
					while ((beginSample > 0) && (Math.abs(samples[beginSample]) > 0.0001))
					{
						beginSample--;
					}
				}
			}
			if (trimLevelEndCombo.getSelectedIndex() < 8)
			{
				double endLevel   = Math.sqrt(Math.pow(10, -1 * (trimLevelEndCombo.getSelectedIndex() + 2)));
				while ((endSample > 0) && (Math.abs(samples[endSample]) < endLevel))
				{
					endSample--;
				}
				if (zeroEndCheckBox.isSelected())
				{
					while ((endSample < numSamples - 2) && (Math.abs(samples[endSample]) > 0.0001))
					{
						endSample++;
					}
				}
			}

			numSamples = endSample - beginSample + 1;
			if (numSamples < 1) return false;
			int newSampleRate = Integer.parseInt((String)sampleRateCombo.getSelectedItem());
			double step = (double) s.sampleRate / (double) newSampleRate;
			double newNumSamples = numSamples / step;
			s.kSampleSize = (int)newNumSamples * 2;
			s.kSampleChunk = new byte[s.kSampleSize];
			if (s.sampleRate != newSampleRate)
			{
				double ptr = beginSample * step;
				for (int i = 0; i < (int)newNumSamples; i++)
				{
					double mu = (ptr - Math.floor(ptr));
					double newSample = mu * (samples[(int) Math.ceil(ptr)] -
											 samples[(int) Math.floor(ptr)]) + samples[(int) Math.floor(ptr)];
					int ns = (int)(newSample * 32768.0);
					s.kSampleChunk[i * 2 + 1] = (byte) (ns >> 8);
					s.kSampleChunk[i * 2]	 = (byte) (ns & 255);
					ptr += step;
					if (ptr > numSamples - 2) ptr = numSamples - 2;
				}
			}
			else
			{
				for (int i = 0; i < numSamples; i++)
				{
					int ns = (int)(samples[beginSample + i] * 32768.0);
					s.kSampleChunk[i * 2 + 1] = (byte) (ns >> 8);
					s.kSampleChunk[i * 2]	 = (byte) (ns & 255);
				}
			}
			s.audioFormat = 1;
			s.byteRate = newSampleRate * 2; // SampleRate * NumChannels * BitsPerSample/8
			s.sampleRate = newSampleRate;
			s.numChannels = 0;
			s.bitsPerSample = 16;
			s.blockAlign = 2;
			s.kPlayLogPeriod = playLogPeriod(newSampleRate);
			return true;
		}

		public void cShow()
		{
			cFrame.setVisible(true);
		}
	}

	/********************************************************************
	*==================== MERGE   ======================================*
	********************************************************************/
	private class Merge
	{
		int mCount = 0;
		int mTotal = 0;
		int mMax = 0;
		String[] mColumnNames = { "#", "Sample Name", "Size" };
		Object[][] mData = new Object[64][3];
		JFrame mFrame = new JFrame("Sample Merger");
		JButton mPlayButton = new JButton("Play");
		JButton mDeleteButton = new JButton("Remove");
		JButton mDeleteAllButton = new JButton("Remove All");
		JButton mSquinchButton = new JButton("Squinch");
		JButton mSaveButton = new JButton("Insert...");
		JButton mSortButton = new JButton("Sort");
		JButton mCloseButton = new JButton("Close");
		JCheckBox mEqualSlices = new JCheckBox("Equal Slices");
		JLabel mTotal1 = new JLabel("Total Samples: ");
		JLabel mTotal2 = new JLabel();
		JLabel mIndividual1 = new JLabel("Individual Size: ");
		JLabel mIndividual2 = new JLabel();
		JLabel mSize1 = new JLabel("Combined Size: ");
		JLabel mSize2 = new JLabel();
		JTable mTable = new JTable(mData, mColumnNames)
		{
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		JScrollPane mScroll = new JScrollPane(mTable);

		public Merge()
		{
			mFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
			for (int i = 0; i < 64; i++) mData[i][0] = i + 1;
			mScroll.setPreferredSize(new Dimension(690, 440));
			mTable.getColumnModel().getColumn(0).setPreferredWidth(35);
			mTable.getColumnModel().getColumn(1).setPreferredWidth(555);
			mTable.getColumnModel().getColumn(2).setPreferredWidth(100);
			mTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			mTable.setDragEnabled(true);

			mTable.getSelectionModel().addListSelectionListener(new ListSelectionListener()
			{
				public void valueChanged(ListSelectionEvent e)
				{
					mTable.scrollRectToVisible(new Rectangle(mTable.getCellRect(mTable.getSelectedRow(), 0, true)));
				}
			});

			mTable.setDropTarget(new DropTarget()
			{
				public synchronized void drop(DropTargetDropEvent dtde)
				{
					int mRow = mTable.rowAtPoint(dtde.getLocation());
					mTable.setRowSelectionInterval(mRow, mRow);
					try
					{
						dtde.acceptDrop(DnDConstants.ACTION_COPY);
						if (dtde.getTransferable().isDataFlavorSupported(DataFlavor.javaFileListFlavor))
						{
							final List fileList = (List) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
							Collections.sort(fileList);
							for (int i = 0; i < fileList.size(); i++)
							{
								File f = (File) fileList.get(i);
								if (f.getName().toLowerCase().endsWith(".wav"))
								{
									Sample s = new Sample();
									if (s.loadSample(f, 0) == -1)
									{
										JOptionPane.showMessageDialog(null, f + " is not a valid WAV.");
									}
									else if (s.numChannels > 0)
									{
										JOptionPane.showMessageDialog(null, f + " must be mono.");
									}
									else
									{
										mData[mRow][1] = f;
										mData[mRow][2] = s.kSampleSize;
										mGetCount();
										mRow++;
										if (mRow > 63)
										{
											mRow = 63;
											break;
										}
									}
								}
							}
						}
						else if (dtde.getTransferable().isDataFlavorSupported(DataFlavor.stringFlavor))
						{
							String drop = (String) dtde.getTransferable().getTransferData(DataFlavor.stringFlavor);
							String[] r = drop.split("\\t");
							int oldRow = Integer.parseInt(r[0]) - 1;
							mRowSwap(mRow, oldRow);
							mGetCount();
						}
						dtde.dropComplete(true);
						mTable.setRowSelectionInterval(mRow, mRow);
					}
					catch (Exception ex) {
						throwError(ex.toString());
					}
				}
			});

			mPlayButton.setFocusable(false);
			mPlayButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent ae)
				{
					int mRow = mTable.getSelectedRow();
					if ((mRow < 0) || (mData[mRow][1] == null)) return;
					try
					{
						AudioInputStream stream = AudioSystem.getAudioInputStream((File) mData[mRow][1]);
						Clip clip = AudioSystem.getClip();
						clip.open(stream);
						clip.start();
					}
					catch (Exception ex) {
						throwError(ex.toString());
					}
				}
			});

			mDeleteButton.setFocusable(false);
			mDeleteButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent ae)
				{
					int mRow = mTable.getSelectedRow();
					mData[mRow][1] = null;
					mData[mRow][2] = null;
					mRow++;
					if (mRow >= 64) mRow = 63;
					mTable.setRowSelectionInterval(mRow, mRow);
					mGetCount();
				}
			});

			mSquinchButton.setFocusable(false);
			mSquinchButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent ae)
				{
					for (int i = 0; i < 63; i++)
					{
						if (mData[i][1] == null)
						{
							for (int j = i + 1; j < 64; j++)
							{
								if (mData[j][1] != null)
								{
									mData[i][1] = mData[j][1];
									mData[i][2] = mData[j][2];
									mData[j][1] = null;
									mData[j][2] = null;
									break;
								}
							}
						}
					}
					mTable.setRowSelectionInterval(0, 0);
					mGetCount();
				}
			});

			mDeleteAllButton.setFocusable(false);
			mDeleteAllButton.setBackground(new Color(255, 128, 128));
			mDeleteAllButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent ae)
				{
					if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null,
							"Are you sure?"))
					{
						for (int i = 0; i < 64; i++)
						{
							mData[i][1] = null;
							mData[i][2] = null;
						}
						mTable.setRowSelectionInterval(0, 0);
						mGetCount();
					}
				}
			});

			mSaveButton.setFocusable(false);
			mSaveButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent ae)
				{
					Sample s = new Sample();
					String reply = JOptionPane.showInputDialog(null, "Insert as?", "MegaSlice");
					if (reply != null)
					{
						// gather up megasample chunk
						byte[] samples;
						if (mEqualSlices.isSelected())
						{
							samples = new byte[mMax * mCount];
						}
						else
						{
							samples = new byte[mTotal];
						}
						int ptr = 0;
						for (int i = 0; i < mCount; i++)
						{
							s = new Sample();
							long fileptr = 0;
							try
							{
								fileptr = s.loadSample((File) mData[i][1], fileptr);
								if (s.numChannels == 1)
								{
									JOptionPane.showMessageDialog(null, mData[i][1] + " is not Mono.");
									return;
								}
							}
							catch (Exception ex) {
								throwError(ex.toString());
							}
							for (int j = 0; j < s.kSampleChunk.length; j++)
							{
								samples[ptr] = s.kSampleChunk[j];
								ptr++;
							}
							if (mEqualSlices.isSelected() && s.kSampleChunk.length < mMax)
							{
								for (int k = 0; k < mMax - s.kSampleChunk.length; k++)
								{
									samples[ptr] = 0;
									ptr++;
								}
							}
						}

						// now create a sample
						s.kSampleChunk = samples;
						s.kSampleSize  = samples.length;
						s.kCategory = dropCatCombo.getSelectedIndex();
						if (reply.length() > 16) reply = reply.substring(0, 16);
						s.kSampleName = reply;
						s.numChannels = 0;
						s.kSampleStart = 0;
						s.kLoopStart = s.kSampleSize - 2;  // must be mono, so -2
						s.kLoopEnd = s.kSampleSize - 2;
						s.kLooping = 1;
						s.kLoudness = 0;
						s.kTuning = 0;
						s.kKorgChunk = new byte[1188];
						for (int i = 0; i < 1188; i++) s.kKorgChunk[i] = 0;
						if (mCount > 1)
						{
							// write slice data
							int j = 0;
							ptr = 0;
							for (int i = 96; i < 1120; i += 16)
							{
								int q = mData[j][2] == null ? 0 : (Integer)mData[j][2];
								s.kKorgChunk[i + 0] = (byte) (ptr & 0xFF);
								s.kKorgChunk[i + 1] = (byte) ((ptr & 0xFF00) >> 8);
								s.kKorgChunk[i + 2] = (byte) ((ptr & 0xFF0000) >> 16);
								s.kKorgChunk[i + 3] = (byte) ((ptr & 0xFF000000) >> 24);
								s.kKorgChunk[i + 4] = (byte) ((q  / 2) & 0xFF);
								s.kKorgChunk[i + 5] = (byte) (((q / 2) & 0xFF00) >> 8);
								s.kKorgChunk[i + 6] = (byte) (((q / 2) & 0xFF0000) >> 16);
								s.kKorgChunk[i + 7] = (byte) (((q / 2) & 0xFF000000) >> 24);
								s.kKorgChunk[i + 8] = s.kKorgChunk[i + 4];
								s.kKorgChunk[i + 9] = s.kKorgChunk[i + 5];
								s.kKorgChunk[i + 10] = s.kKorgChunk[i + 6];
								s.kKorgChunk[i + 11] = s.kKorgChunk[i + 7];
								s.kKorgChunk[i + 12] = (byte) 0xFF;
								s.kKorgChunk[i + 13] = (byte) 0xFF;
								s.kKorgChunk[i + 14] = (byte) 0x00;
								s.kKorgChunk[i + 15] = (byte) 0x00;
								if (mEqualSlices.isSelected())
								{
									ptr += mMax / 2;
								}
								else
								{
									ptr += q / 2;
								}
								j++;
							}
							int stepCount = 0;
							for (int i = 1120; i < 1184; i++)
							{
								if (stepCount < mCount)
								{
									s.kKorgChunk[i] = (byte) stepCount;
									stepCount++;
								}
								else
								{
									s.kKorgChunk[i] = (byte) 0xFF;
								}
							}
							s.kKorgChunk[1184] = (byte) ((((mCount - 1) / 16) + 1) * 16);
							s.kKorgChunk[1186] = (byte) mCount;
						}
						s.kChunkStart = 0;
						int row2 = getRow();
						af.sampleBank[row2] = s;
						updateChunkStarts();
						table1.repaint();
						incRow(row2);
					}
				}
			});

			mSortButton.setFocusable(false);
			mSortButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent ae)
				{
					Arrays.sort(mData, new Comparator<Object[]>()
					{
						public int compare(Object o1[], Object o2[])
						{
							if ((o1[2] == null) || (o2[2] == null)) return 0;
							return -((Integer) o1[2]).compareTo((Integer) o2[2]);
						}
					});
					for (int i = 0; i < 64; i++)
					{
						mData[i][0] = i + 1;
						if (mData[i][1] == null) mData[i][2] = null;
					}
					mTable.setRowSelectionInterval(0, 0);
					mGetCount();
				}
			});

			mCloseButton.setFocusable(false);
			mCloseButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent ae)
				{
					mFrame.setVisible(false);
				}
			});

			mEqualSlices.setFocusable(false);
			mEqualSlices.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent ae)
				{
					mGetCount();
				}
			});

			mFrame.setLayout(new FlowLayout());
			mFrame.add(mSaveButton);
			mFrame.add(mTotal1);
			mFrame.add(mTotal2);
			mFrame.add(mIndividual1);
			mFrame.add(mIndividual2);
			mFrame.add(mSize1);
			mFrame.add(mSize2);
			mFrame.add(mCloseButton);
			mFrame.add(mScroll);
			mFrame.add(mEqualSlices);
			mFrame.add(mPlayButton);
			mFrame.add(mDeleteAllButton);
			mFrame.add(mDeleteButton);
			mFrame.add(mSquinchButton);
			mFrame.add(mSortButton);
			mFrame.setSize(700, 540);
			mFrame.setLocation(100, 100);
			mFrame.setResizable(false);
		}

		public void mShow()
		{
			mFrame.setVisible(true);
		}

		private void mGetCount()
		{
			mCount = 0;
			mMax = 0;
			mTotal = 0;
			for (int i = 0; i < 64; i++)
			{
				if (mData[i][1] == null) break;
				mCount++;
				mTotal += (Integer) mData[i][2];
				if ((Integer) mData[i][2] > mMax) mMax = (Integer) mData[i][2];
			}
			mTotal2.setText(Integer.toString(mCount) + "	");
			mIndividual2.setText(Integer.toString(mTotal + (mCount * 1232)) + "	");
			int sz;
			if (mEqualSlices.isSelected()) sz = (mCount * mMax) + 1232;
			else sz = mTotal + 1232;
			if (sz < 1233) sz = 0;
			mSize2.setText(sz + "	");
			if (mCount < 1) mSaveButton.setEnabled(false);
			else mSaveButton.setEnabled(true);
			mTable.repaint();
		}

		private void mRowSwap(int n, int o)
		{
			Object temp1 = mData[n][1];
			Object temp2 = mData[n][2];
			mData[n][1] = mData[o][1];
			mData[n][2] = mData[o][2];
			mData[o][1] = temp1;
			mData[o][2] = temp2;
			mTable.setRowSelectionInterval(n, n);
		}

	}

	/********************************************************************
	*==================== ALLFILE ======================================*
	********************************************************************/

	private class AllFile
	{
		Sample sampleBank[] = new Sample[902];
		File allFile;

		public AllFile()
		{
			for (int i = 0; i < MAX_ROWS; i++) sampleBank[i] = new Sample();
		}

		private void readFile()
		{
			try
			{
				RandomAccessFile raf = new RandomAccessFile(allFile, "rw");
				byte[] e2s = new byte[16];
				raf.read(e2s);
				String str = new String(e2s);
				if (str.equals("e2s sample all" + (char) 26 + (char) 0))
				{
					frame.setTitle("EdiTribe - " + allFile);
					raf.seek(88);

					int lastSt = 0;
					for (int i = 19; i < 1000; i++)
					{
						int st = Integer.reverseBytes(raf.readInt());
						if (st > 0) af.sampleBank[osc2row(i)].kChunkStart = st;
					}
					raf.close();
					long fileptr = 4096;
					for (int i = 0; i < MAX_ROWS; i++)
					{
						if (af.sampleBank[i].kChunkStart > 0) fileptr = af.sampleBank[i].loadSample(allFile, fileptr);
					}
				}
				else
				{
					JOptionPane.showMessageDialog(null, "Not a valid .all file.");
				}
				raf.close();
			}
			catch (Exception ex) {
				throwError(ex.toString());
			}
		}

		private void writeFile()
		{
			byte[] data = null;

			updateChunkStarts();
			try
			{
				RandomAccessFile newall = new RandomAccessFile("tempall.all", "rw");

				newall.writeBytes("e2s sample all" + (char) 26 + (char) 0);
				for (int i = 0; i < 4080; i++) newall.write(0);
				for (int j = 0; j < MAX_ROWS; j++)
				{
					if (af.sampleBank[j].kChunkStart != -1)
					{
						newall.seek(16 + (row2osc(j) - 1) * 4);
						newall.writeInt(Integer.reverseBytes(af.sampleBank[j].kChunkStart));
					}
				}
				newall.setLength(4096);
				newall.close();

				for (int i = 0; i < MAX_ROWS; i++)
				{
					if (af.sampleBank[i].kChunkStart != -1) af.sampleBank[i].saveSample(new File("tempall.all"), i);
				}


				// delete old .all file
				allFile.delete();

				// rename file
				File tempAll = new File("tempall.all");
				tempAll.renameTo(allFile);
			}
			catch (Exception ex) {
				throwError(ex.toString());
			}
		}
	}


	/********************************************************************
	*==================== SAMPLE  ======================================*
	********************************************************************/

	private class Sample
	{
		String kSampleName = "";
		boolean kSaveKorg = true;
		byte kSampleChunk[] = null;
		byte kKorgChunk[] = new byte[1188];

		int kSampleSize = 0;

		int audioFormat = 0;
		int numChannels = 0;
		int sampleRate = 0;
		int byteRate = 0;
		int blockAlign = 0;
		int bitsPerSample = 0;

		int kChunkStart = -1;
		int kCategory = 17;
		int kPlayLogPeriod = 0;
		int kAmplitude = 0xFFFF;

		int kSampleStart = 0;
		int kLoopStart = 0;
		int kLoopEnd = 0;
		int kLooping = 1;
		int kLoudness = 0;
		int kTuning = 0;

		public long loadSample(File f, long fp)
		{
			long pointer = fp;
			long initPointer = fp;
			long fileLength = 0;
			try
			{
				RandomAccessFile raf = new RandomAccessFile(f, "rw");
				while (true)
				{
					raf.seek(pointer);
					String chunk = "";
					for (int i = 0; i < 4; i++) chunk += (char) raf.readByte();
					int chunkLength = Integer.reverseBytes(raf.readInt());
					if (chunk.equals("RIFF"))
					{
						fileLength = chunkLength + 8;  // raf.length()
						chunk = "";
						for (int i = 0; i < 4; i++) chunk += (char) raf.readByte();
						if (!chunk.equals("WAVE"))
						{
							raf.close();
							return -1;
						}
						chunkLength = 4;  // set up for addition at wend
					}
					else if (chunk.equals("fmt "))
					{
						raf.seek(pointer + 8);
						audioFormat = Short.reverseBytes(raf.readShort());
						numChannels = (short) (Short.reverseBytes(raf.readShort()) - 1);
						sampleRate = Integer.reverseBytes(raf.readInt());
						byteRate = Integer.reverseBytes(raf.readInt());
						blockAlign = Short.reverseBytes(raf.readShort());
						bitsPerSample = Short.reverseBytes(raf.readShort());
					}
					else if (chunk.equals("data"))
					{
						kSampleSize = chunkLength;
						kLoopStart = kSampleSize - blockAlign;
						kLoopEnd = kSampleSize - blockAlign;
						raf.seek(pointer + 8);
						kSampleChunk = new byte[chunkLength];
						raf.read(kSampleChunk);
					}
					else if (chunk.equals("korg"))
					{
						raf.seek(pointer + 18);
						kSampleName = "";
						for (int i = 0; i < 16; i++) kSampleName += (char) raf.readUnsignedByte();
						kSampleName = kSampleName.trim();
						raf.seek(pointer + 34);
						kCategory = Short.reverseBytes(raf.readShort());
						raf.seek(pointer + 50);
						kPlayLogPeriod = Short.reverseBytes(raf.readShort());
						kAmplitude = Short.reverseBytes(raf.readShort());
						raf.seek(pointer + 56);
						kSampleStart = Integer.reverseBytes(raf.readInt());
						kLoopStart = Integer.reverseBytes(raf.readInt());
						kLoopEnd = Integer.reverseBytes(raf.readInt());
						kLooping = Integer.reverseBytes(raf.readInt());
						raf.seek(pointer + 82);
						kLoudness = raf.read();
						raf.seek(pointer + 93);
						kTuning = raf.read();
						raf.seek(pointer);
						raf.read(kKorgChunk);
					}
					else if (chunk.equals("smpl"))
					{
						kLooping = 0;
						raf.seek(pointer + 52);
						kLoopStart = Integer.reverseBytes(raf.readInt());
						// pointer to last sample
						kLoopEnd = Integer.reverseBytes(raf.readInt());
						if (kLoopEnd > kSampleSize - blockAlign) kLoopEnd = kSampleSize - blockAlign;
						kSaveKorg = true;
					}
					if (fileLength == 0)
					{
						raf.close();
						return -1;
					}
					pointer += chunkLength + 8;
					if (pointer >= initPointer + fileLength)
					{
						pointer = raf.getFilePointer();
						raf.close();
						break;
					}
				}
			}
			catch (Exception ex) {
				throwError(ex.toString());
			}
			if (kLoopEnd == 0)
			{
				kLoopEnd = kSampleSize - blockAlign;
				if (kLooping == 1) kLoopStart = kLoopEnd;
			}
			return pointer;
		}

		public void saveSample(File f, int row2)
		{
			try
			{
				RandomAccessFile raf = new RandomAccessFile(f, "rw");
				raf.seek(raf.length());
				// write RIFF block
				raf.writeBytes("RIFF");
				// write total file length
				int fs = kSampleSize + 44 - 8;
				if (kSaveKorg) fs += 1188;
				raf.writeInt(Integer.reverseBytes(fs));
				// write WAVE
				raf.writeBytes("WAVE");
				// write fmt block
				raf.writeBytes("fmt ");
				// write fmt chunk size
				raf.writeInt(Integer.reverseBytes(16));
				// write fmt data
				raf.writeShort(Short.reverseBytes((short) 1));
				raf.writeShort(Short.reverseBytes((short) (numChannels + 1)));
				raf.writeInt(Integer.reverseBytes(sampleRate));
				raf.writeInt(Integer.reverseBytes(byteRate));
				raf.writeShort(Short.reverseBytes((short) blockAlign));
				raf.writeShort(Short.reverseBytes((short) bitsPerSample));
				// write data
				raf.writeBytes("data");
				// write data size
				raf.writeInt(Integer.reverseBytes(kSampleSize));
				// write sample data
				raf.write(kSampleChunk);
				if (kSaveKorg)
				{
					// KORG BLOCK
					// write initial korg block data
					writeHextToFile(raf, "6B6F72679C04000065736C6994040000");
					// write unit sample number
					// row2+18 as they are stored 0-based but 1-based on screen
					raf.writeShort(Short.reverseBytes((short) (row2osc(row2) - 1)));
					// write display name
					raf.writeBytes(kSampleName);
					// pad the name with 0s
					for (int j = 0; j < (16 - kSampleName.length()); j++) raf.write(0);
					// store the category
					raf.writeShort(Short.reverseBytes((short) kCategory));
					// int -> short...
					raf.writeShort(Short.reverseBytes(osc2import(row2osc(row2))));
					// write middle string
					writeHextToFile(raf, "0000007F0001000000000000");
					// write playLogPeriod, Volume
					if (kPlayLogPeriod == 0) kPlayLogPeriod = playLogPeriod(sampleRate);
					raf.writeShort(Short.reverseBytes((short)kPlayLogPeriod));
					raf.writeShort(Short.reverseBytes((short)kAmplitude));
					writeHextToFile(raf, "0000");
					raf.writeInt(Integer.reverseBytes(kSampleStart)); // start of sample
					raf.writeInt(Integer.reverseBytes(kLoopStart));
					raf.writeInt(Integer.reverseBytes(kLoopEnd));
					raf.writeInt(Integer.reverseBytes(kLooping));
					// write more zeros
					writeHextToFile(raf, "00000000");
					raf.writeInt(Integer.reverseBytes(kSampleSize));
					// write a 01
					writeHextToFile(raf, "01");
					raf.write(numChannels);
					raf.write(kLoudness);
					// write end string
					writeHextToFile(raf, "01B0040000");
					raf.writeInt(Integer.reverseBytes(sampleRate));
					raf.write(0);
					raf.write(kTuning);
					// write sample number again
					raf.writeShort(Short.reverseBytes((short) (row2osc(row2) - 1)));
					for (int j = 96; j < 1188; j++) raf.write(kKorgChunk[j]);
				}
				raf.close();
			}
			catch (Exception ex) {
				throwError(ex.toString());
			}
		}
	}

	public void writeHextToFile(RandomAccessFile raf, String hex)
	{
		try
		{
			for (int i = 0; i < hex.length(); i+=2)
			{
				int j = Integer.parseInt(hex.substring(i, i+2), 16);
				raf.write(j);
			}
		}
		catch (Exception ex) {}
	}

	/********************************************************************
	*==================== STATICS ======================================*
	********************************************************************/


	public static int playLogPeriod(int plp)
	{
		int retVal;
		switch (plp)
		{
		case 11025:
			retVal = 21880;
			break;
		case 16000:
			retVal = 20229;
			break;
		case 22050:
			retVal = 18808;
			break;
		case 32000:
			retVal = 17157;
			break;
		case 44100:
			retVal = 15736;
			break;
		case 48000:
			retVal = 15360;
			break;
		default:
			retVal = 15736;
			break;
		}
		return retVal;
	}

	public static void throwError(String message)
	{
		message = message.replaceAll("[\t\f\n\r]", " • ");
		File of = new File("error.txt");
		try
		{
			if (!of.exists()) of.createNewFile();
			FileWriter fw = new FileWriter(of, true);
			fw.write(message + System.getProperty("line.separator"));
			fw.close();
		}
		catch (Exception ex)
		{
			System.out.println(ex);
		}
	}

	public static void main(String[] args)
	{
		try
		{
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
			{
				if ("Nimbus".equals(info.getName()))
				{
					UIManager.setLookAndFeel(info.getClassName());
				}
			}
		}
		catch (Exception ex) {
			throwError(ex.toString());
		}
		new Editribe();
	}
}

