// Copyright (C) 2005-2006 epoximator
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

/*
 * Created on 29.01.05
 *
 * @version 	04 (1.09,1.07,1.06,1.01)
 * @author 		epoximator
 */
package epox.webaom;

import epox.swing.UniqueStringList;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;

public class FileHandler {
	public UniqueStringList allowedExtensions;
	public ExtensionFileFilter extensionFilter;

	public FileHandler() {
		allowedExtensions = new UniqueStringList(Options.FIELD_SEPARATOR);
		extensionFilter = new ExtensionFileFilter();
	}

	public synchronized void addExtension(String extension) {
		allowedExtensions.add(extension);
	}

	public synchronized void removeExtension(int index) {
		allowedExtensions.removeElementAt(index);
	}

	public synchronized boolean addFile(File file) {
		if ((allowedExtensions.includes(getExtension(file)) || allowedExtensions.getSize() == 0)
				&& !AppContext.jobs.has(file) && !isFileLocked(file)) {
			Job job = AppContext.jobs.add(file);
			if (job != null) {
				job.updateHealth(Job.H_PAUSED);
				return true;
			}
		}
		return false;
	}

	protected String getExtension(File file) {
		int dotIndex = file.getName().lastIndexOf(".");
		if (dotIndex < 0) {
			return null;
		}
		return file.getName().substring(dotIndex + 1).toLowerCase();
	}

	private boolean isFileLocked(File file) {
		try {
			InputStream inputStream = new FileInputStream(file);
			inputStream.close();
			return false;
		} catch (FileNotFoundException e) {
			// File not found means it's inaccessible
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	public synchronized void saveOptions(Options options) {
		String extensionList = "";
		Object[] extensions = allowedExtensions.getStrings();
		for (Object extension : extensions) {
			extensionList += extension + Options.FIELD_SEPARATOR;
		}
		options.setString(Options.STR_EXTENSIONS, extensionList);
	}

	public synchronized void loadOptions(Options options) {
		StringTokenizer tokenizer = new StringTokenizer(options.getString(Options.STR_EXTENSIONS),
				Options.FIELD_SEPARATOR);
		while (tokenizer.hasMoreTokens()) {
			allowedExtensions.add(tokenizer.nextToken());
		}
	}

	protected class ExtensionFileFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter {
		public boolean accept(File file) {
			if (file.isDirectory()) {
				return true;
			}
			return allowedExtensions.includes(getExtension(file)) || allowedExtensions.getSize() == 0;
		}

		public String getDescription() {
			return "Me WANTS!";
		}
	}
}
