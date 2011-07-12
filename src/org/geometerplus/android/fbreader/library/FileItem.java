/*
 * Copyright (C) 2010-2011 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader.library;

import java.util.*;

import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.image.ZLImage;

import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.fbreader.library.Book;
import org.geometerplus.fbreader.library.Library;
import org.geometerplus.fbreader.tree.FBTree;

class FileItem extends FBTree {
	private final ZLFile myFile;
	private final String myName;
	private final String mySummary;
	private final boolean myIsSelectable;

	public FileItem(ZLFile file, String name, String summary) {
		myFile = file;
		myName = name;
		mySummary = summary;
		myIsSelectable = false;
	}

	public FileItem(ZLFile file) {
		if (file.isArchive() && file.getPath().endsWith(".fb2.zip")) {
			final List<ZLFile> children = file.children();
			if (children.size() == 1) {
				final ZLFile child = children.get(0);
				if (child.getPath().endsWith(".fb2")) {
					myFile = child;
					myName = file.getLongName();
					mySummary = null;
					myIsSelectable = true;
					return;
				}
			} 
		}
		myFile = file;
		myName = null;
		mySummary = null;
		myIsSelectable = true;
	}

	@Override
	public String getName() {
		return myName != null ? myName : myFile.getShortName();
	}

	@Override
	public String getSummary() {
		if (mySummary != null) {
			return mySummary;
		}

		final Book book = getBook();
		if (book != null) {
			return book.getTitle();
		}

		return null;
	}

	public boolean isSelectable() {
		return myIsSelectable;
	}

	public int getIcon() {
		if (getBook() != null) {
			return R.drawable.ic_list_library_book;
		} else if (myFile.isDirectory()) {
			if (myFile.isReadable()) {
				return R.drawable.ic_list_library_folder;
			} else {
				return R.drawable.ic_list_library_permission_denied;
			}
		} else if (myFile.isArchive()) {
			return R.drawable.ic_list_library_zip;
		} else {
			return R.drawable.ic_list_library_permission_denied;
		}
	}

	@Override
	public ZLImage createCover() {
		return Library.getCover(myFile);
	}

	public ZLFile getFile() {
		return myFile;
	}

	public Book getBook() {
		return Book.getByFile(myFile);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof FileItem)) {
			return true;
		}
		return myFile.equals(((FileItem)o).myFile);
	}
}