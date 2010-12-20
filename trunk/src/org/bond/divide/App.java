package org.bond.divide;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

public class App extends JFrame implements Runnable {

	private static final long serialVersionUID = 0;
	
	private static final String FOLDER = "FOLDER";
	
	private static final String SEP = "_by_";
	
	private String src;
	
	private JTextField folder;
	private JButton browse;
	private JProgressBar bar;
	private JButton divide;
	private JLabel total;
	private JLabel noBy;
	private JLabel manyBy;
	private JLabel dupe;
	private JLabel dir;
	private JLabel move;
	private JLabel error;
	
	// reports stuff
	private int totalFiles = 0;
	private List<String> noBys = new ArrayList<String>();
	private List<String> manyBys = new ArrayList<String>();
	private List<String> dirs = new ArrayList<String>();
	private List<String> dups = new ArrayList<String>();
	private List<String> moves = new ArrayList<String>();
	private List<String> errors = new ArrayList<String>();
	
	public App() {
		super("Divide-1.0.2");
		setMinimumSize(new Dimension(400, 200));
		LayoutManager lm = new GridLayout(3, 1);
		setLayout(lm);
		add(createTop());
		add(createMiddle());
		add(createBottom());
		pack();
		setDefaultCloseOperation(HIDE_ON_CLOSE);
	}
	
	protected JPanel createTop() {
		JPanel p = new JPanel(new GridBagLayout());
		p.add(createFolder());
		browse = new JButton("Browse");
		browse.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				browse();
			}
		});
		p.add(browse);
		return p;
	}
	
	protected JPanel createFolder() {
		JPanel p = new JPanel(new FlowLayout());
		JLabel label = new JLabel("Folder:");
		p.add(label);
		folder = new JTextField(src, 20);
		folder.setMinimumSize(new Dimension(folder.getWidth(), folder.getHeight()));
		folder.setEditable(false);
		p.add(folder);
		label.setLabelFor(folder);
		return p;
	}
	
	protected JPanel createMiddle() {
		JPanel p = new JPanel(new FlowLayout());
		bar = new JProgressBar();
		p.add(bar);
		divide = new JButton("Divide!");
		divide.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				divide();
			}
		});
		p.add(divide);
		return p;
	}
	
	protected JPanel createBottom() {
		JPanel p = new JPanel(new FlowLayout());
		JLabel l1 = new JLabel("Files:");
		p.add(l1);
		total = new JLabel("-");
		p.add(total);
		JLabel l2 = new JLabel("No '_by_':");
		p.add(l2);
		noBy = new JLabel("-");
		p.add(noBy);
		JLabel l3 = new JLabel("Many '_by_':");
		p.add(l3);
		manyBy = new JLabel("-");
		p.add(manyBy);
		JLabel l4 = new JLabel("Duplicates:");
		p.add(l4);
		dupe = new JLabel("-");
		p.add(dupe);
		JLabel l5 = new JLabel("Folders created:");
		p.add(l5);
		dir = new JLabel("-");
		p.add(dir);
		JLabel l6 = new JLabel("Moved:");
		p.add(l6);
		move = new JLabel("-");
		p.add(move);
		JLabel l7 = new JLabel("Errors:");
		p.add(l7);
		error = new JLabel("-");
		p.add(error);
		return p;
	}
	
	public static void main(String[] args) {
//		log("class=" + App.class);
		new App().setVisible(true);
	}
	
	protected static void log(String s) {
		System.out.println(s);
	}
	
	@Override
	public void processWindowEvent(WindowEvent e) {
		if (e.getID() == WindowEvent.WINDOW_OPENED) {
			readPrefs();
		}
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			writePrefs();
			dispose();
		}
	}

	protected void readPrefs() {
//		log("loading prefs");
		Preferences prefs = Preferences.userNodeForPackage(App.class);
		src = prefs.get(FOLDER, null);
		if (src != null) {
			folder.setText(src);
			// could be old non-existing folder
			final File f = new File(src);
			final boolean ok = f.exists() && f.isDirectory();
			divide.setEnabled(ok); // ok if folder exists
		} else {
			divide.setEnabled(false); // no folder
		}
//		log("pref:FOLDER=" + src);
	}
	
	protected void writePrefs() {
//		log("saving prefs");
		Preferences prefs = Preferences.userNodeForPackage(App.class);
		if (src != null) {
			prefs.put(FOLDER, src);
//			log("pref:FOLDER=" + src);
		} else if (src == null) {
			prefs.remove(FOLDER);
//			log("pref:FOLDER removed");
		}
	}
	
	protected void browse() {
		final JFileChooser d = new JFileChooser(src);
		d.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		final int result = d.showDialog(this, "Folder to divide..");
		final boolean ok = JFileChooser.APPROVE_OPTION == result;
		if (ok) {
			src = d.getSelectedFile().getAbsolutePath();
			folder.setText(src);
			divide.setEnabled(true);
			log("selected=" + src);
		} else {
			// leave "divide" button as it is
			log("error/cancel=" + result);
		}
	}
	
	protected void divide() {
		browse.setEnabled(false);
		divide.setEnabled(false);
//		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		new Thread(this).run();
	}
	
	public void run() {
		File root = new File(src);
		processFolder(root);
		report(src);
		browse.setEnabled(true);
		divide.setEnabled(true);
//		setDefaultCloseOperation(HIDE_ON_CLOSE);
	}
	
	protected void processFolder(File f) {
//		log("progress=" + bar.getMinimum() + "/" + bar.getMaximum()
//				+ "/" + bar.getValue());
		bar.setValue(1);
		File[] list = f.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				final boolean exists = pathname.exists();
				final boolean file = pathname.isFile();
//				final boolean dir = pathname.isDirectory();
//				log("file=" + pathname + "; x=" + exists + "; f=" + file + "; d=" + dir);
				return exists && file /*&& !dir*/;
			}
		});
		totalFiles = list.length;
		total.setText("" + totalFiles);
		bar.setMaximum(totalFiles + 3); // start=+1,list=+1,report=+1
		bar.setValue(2);
//		log("list of " + list.length + " files");
		for (File i : list) {
			if (i.getName().contains(SEP)) {
				parseFilename(i);
			} else {
//				log("file '" + i.getName() + "' doesnt have '" + SEP + "' in it's name");
				noBys.add(i.getName());
				noBy.setText("" + noBys.size());
			}
			bar.setValue(bar.getValue() + 1);
		}
	}
	
	protected void parseFilename(File f) {
		String[] s = f.getName().split(SEP);
		if (s.length == 2) {
			// take last part
			String ext = s[s.length - 1];
			int dot = ext.lastIndexOf('.');
			// auther is before dot
			String auth = ext.substring(0, dot);
			// file extension is after dot
			ext = ext.substring(dot);
			// remove garbage after dash if present
			int dash = auth.lastIndexOf('-');
			if (dash != -1) {
				auth = auth.substring(0, dash);
			}
//			log("s=" + file + " by " + auth);
			moveFile(f, auth);
		} else if (s.length > 2) {
//			log("manyBYs=" + f.getName());
			manyBys.add(f.getName());
			manyBy.setText("" + manyBys.size());
		} else {
//			log("noBY=" + f.getName());
		}
	}
	
	protected void moveFile(File f, String a) {
		String parent = f.getParent();
//		log("dir=" + dir);
		File d = new File(parent + "/" + a);
//		log("authorDir=" + d + "; exists=" + d.exists());
		File t = new File(d.getPath() + "/" + f.getName());
//		log("target=" + t + "; exists=" + t.exists());
		final boolean exists = t.exists();
		if (exists) {
//			log("duplicate=" + d.getName() + "/" + t.getName());
			dups.add(d.getName() + "/" + t.getName());
			dupe.setText("" + dups.size());
		} else {
			// no duplicate
			final boolean created = d.mkdir();
			if (created) {
				dirs.add(d.getName());
				dir.setText("" + dirs.size());
			}
			final boolean moved = f.renameTo(t);
			if (moved) {
				moves.add(f.getName());
				move.setText("" + moves.size());
			} else {
				errors.add(f.getName());
				error.setText("" + errors.size());
			}
		}
	}
	
	protected void report(String dir) {
		File r = new File(dir + "/" + "_report.txt");
		if (!r.exists()) {
			try {
				r.createNewFile();
			} catch (IOException e) {
				log("failed to create _report.txt");
			}
		}
		try {
			PrintStream ps = new PrintStream(new FileOutputStream(r, true));
			ps.println();
			Date d = new Date();
			String stamp = d.toLocaleString();
			ps.println("=== " + stamp + " ===");
			ps.println();
			ps.println("Total files found: " + totalFiles);
			ps.println();
			ps.println("Files without '_by_': " + noBys.size());
			for (String s : noBys) {
				ps.println(s);
			}
			ps.println();
			ps.println("Files with multiple '_by_'-s: " + manyBys.size());
			for (String s : manyBys) {
				ps.println(s);
			}
			ps.println();
			ps.println("Folders created: " + dirs.size());
			for (String s : dirs) {
				ps.println(s);
			}
			ps.println();
			ps.println("Duplicate files: " + dups.size());
			for (String s : dups) {
				ps.println(s);
			}
			ps.println();
			ps.println("Moved files: " + moves.size());
			for (String s : moves) {
				ps.println(s);
			}
			ps.println();
			ps.println("Failed to move: " + errors.size());
			for (String s : errors) {
				ps.println(s);
			}
			ps.println();
			ps.println("=== " + stamp + " ===");
			ps.flush();
			ps.close();
		} catch (FileNotFoundException e) {
			log("_report.txt not present");
		}
		bar.setValue(bar.getValue() + 1);
	}
	
}
