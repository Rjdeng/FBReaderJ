/*
 * Copyright (C) 2009-2012 Geometer Plus <contact@geometerplus.com>
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

package org.geometerplus.android.fbreader;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.geometerplus.android.fbreader.api.ApiListener;
import org.geometerplus.android.fbreader.api.ApiServerImplementation;
import org.geometerplus.android.fbreader.api.PluginApi;
import org.geometerplus.android.fbreader.library.KillerCallback;
import org.geometerplus.android.fbreader.library.SQLiteBooksDatabase;
import org.geometerplus.android.util.UIUtil;
import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.fbreader.library.Library;
import org.geometerplus.fbreader.tips.TipsManager;
import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.library.ZLibrary;
import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenator;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.ui.android.R;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidActivity;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary;
import org.geometerplus.zlibrary.ui.android.view.AndroidFontUtil;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.onyx.android.sdk.data.cms.OnyxCmsCenter;
import com.onyx.android.sdk.data.cms.OnyxHistoryEntry;
import com.onyx.android.sdk.data.cms.OnyxHistoryEntryHelper;
import com.onyx.android.sdk.data.cms.OnyxMetadata;
import com.onyx.android.sdk.ui.SelectionPopupMenu;
import com.onyx.android.sdk.ui.dialog.DialogSearchView;

public final class FBReader extends ZLAndroidActivity {
	public static final String BOOK_PATH_KEY = "BookPath";

	public static final int REQUEST_PREFERENCES = 1;
	public static final int REQUEST_BOOK_INFO = 2;
	public static final int REQUEST_CANCEL_MENU = 3;

	public static final int RESULT_DO_NOTHING = RESULT_FIRST_USER;
	public static final int RESULT_REPAINT = RESULT_FIRST_USER + 1;
	public static final int RESULT_RELOAD_BOOK = RESULT_FIRST_USER + 2;
	
	private int myFullScreenFlag;
	private FBReaderApp fbReader = null;
	private int mScreenWidth = 0;
	private int mScreenHeight = 0;
	private static final String PLUGIN_ACTION_PREFIX = "___";
	private final List<PluginApi.ActionInfo> myPluginActions =
		new LinkedList<PluginApi.ActionInfo>();
	private DialogSearchView mDialogSearchView = null;
	private final BroadcastReceiver myPluginInfoReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final ArrayList<PluginApi.ActionInfo> actions = getResultExtras(true).<PluginApi.ActionInfo>getParcelableArrayList(PluginApi.PluginInfo.KEY);
			if (actions != null) {
				synchronized (myPluginActions) {
					final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();
					int index = 0;
					while (index < myPluginActions.size()) {
						fbReader.removeAction(PLUGIN_ACTION_PREFIX + index++);
					}
					myPluginActions.addAll(actions);
					index = 0;
					for (PluginApi.ActionInfo info : myPluginActions) {
						fbReader.addAction(
							PLUGIN_ACTION_PREFIX + index++,
							new RunPluginAction(FBReader.this, fbReader, info.getId())
						);
					}
				}
			}
		}
	};

	@Override
	protected ZLFile fileFromIntent(Intent intent) {
		String filePath = intent.getStringExtra(BOOK_PATH_KEY);
		if (filePath == null) {
			final Uri data = intent.getData();
			if (data != null) {
				filePath = data.getPath();
			}
		}
		return filePath != null ? ZLFile.createFileByPath(filePath) : null;
	}

	@Override
	protected Runnable getPostponedInitAction() {
		return new Runnable() {
			public void run() {
				runOnUiThread(new Runnable() {
					public void run() {
						new TipRunner().start();
						DictionaryUtil.init(FBReader.this);
					}
				});
			}
		};
	}
 
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		mScreenWidth = this.getWindowManager().getDefaultDisplay().getWidth();
    	mScreenHeight = this.getWindowManager().getDefaultDisplay().getHeight();
    	
		final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();
		final ZLAndroidLibrary zlibrary = (ZLAndroidLibrary)ZLibrary.Instance();
		myFullScreenFlag =
			zlibrary.ShowStatusBarOption.getValue() ? 0 : WindowManager.LayoutParams.FLAG_FULLSCREEN;
		getWindow().setFlags(
			WindowManager.LayoutParams.FLAG_FULLSCREEN, myFullScreenFlag
		);

		if (fbReader.getPopupById(TextSearchPopup.ID) == null) {
			new TextSearchPopup(fbReader);
		}
		if (fbReader.getPopupById(NavigationPopup.ID) == null) {
			new NavigationPopup(fbReader);
		}
		if (fbReader.getPopupById(SelectionPopup.ID) == null) {
			new SelectionPopup(fbReader);
		}
		
		fbReader.addAction(ActionCode.SHOW_LIBRARY, new ShowLibraryAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_PREFERENCES, new ShowPreferencesAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_BOOK_INFO, new ShowBookInfoAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_TOC, new ShowTOCAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_BOOKMARKS, new ShowBookmarksAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_NETWORK_LIBRARY, new ShowNetworkLibraryAction(this, fbReader));
		
		fbReader.addAction(ActionCode.SHOW_MENU, new ShowMenuAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_NAVIGATION, new ShowNavigationAction(this, fbReader));
		fbReader.addAction(ActionCode.DICTIONARY, new DictionaryAction(this, fbReader));
		fbReader.addAction(ActionCode.SEARCH, new SearchAction(this, fbReader));
		fbReader.addAction(ActionCode.SHARE_BOOK, new ShareBookAction(this, fbReader));

		fbReader.addAction(ActionCode.SELECTION_SHOW_PANEL, new SelectionShowPanelAction(this, fbReader));
		fbReader.addAction(ActionCode.SELECTION_HIDE_PANEL, new SelectionHidePanelAction(this, fbReader));
		fbReader.addAction(ActionCode.SELECTION_COPY_TO_CLIPBOARD, new SelectionCopyAction(this, fbReader));
		fbReader.addAction(ActionCode.SELECTION_SHARE, new SelectionShareAction(this, fbReader));
		fbReader.addAction(ActionCode.SELECTION_TRANSLATE, new SelectionTranslateAction(this, fbReader));
		fbReader.addAction(ActionCode.SELECTION_BOOKMARK, new SelectionBookmarkAction(this, fbReader));

		fbReader.addAction(ActionCode.PROCESS_HYPERLINK, new ProcessHyperlinkAction(this, fbReader));

		fbReader.addAction(ActionCode.SHOW_CANCEL_MENU, new ShowCancelMenuAction(this, fbReader));

		fbReader.addAction(ActionCode.SET_SCREEN_ORIENTATION_SYSTEM, new SetScreenOrientationAction(this, fbReader, ZLibrary.SCREEN_ORIENTATION_SYSTEM));
		fbReader.addAction(ActionCode.SET_SCREEN_ORIENTATION_SENSOR, new SetScreenOrientationAction(this, fbReader, ZLibrary.SCREEN_ORIENTATION_SENSOR));
		fbReader.addAction(ActionCode.SET_SCREEN_ORIENTATION_PORTRAIT, new SetScreenOrientationAction(this, fbReader, ZLibrary.SCREEN_ORIENTATION_PORTRAIT));
		fbReader.addAction(ActionCode.SET_SCREEN_ORIENTATION_LANDSCAPE, new SetScreenOrientationAction(this, fbReader, ZLibrary.SCREEN_ORIENTATION_LANDSCAPE));
		if (ZLibrary.Instance().supportsAllOrientations()) {
			fbReader.addAction(ActionCode.SET_SCREEN_ORIENTATION_REVERSE_PORTRAIT, new SetScreenOrientationAction(this, fbReader, ZLibrary.SCREEN_ORIENTATION_REVERSE_PORTRAIT));
			fbReader.addAction(ActionCode.SET_SCREEN_ORIENTATION_REVERSE_LANDSCAPE, new SetScreenOrientationAction(this, fbReader, ZLibrary.SCREEN_ORIENTATION_REVERSE_LANDSCAPE));
		}
		
		fbReader.addAction(ActionCode.SHOW_DIALOG_TOC, new ShowDialogTOCAction(this, fbReader));
		
		if (isPhoneStyle()) {
			fbReader.addAction(ActionCode.SHOW_DIALOG_MENU, new ShowDialogMenuActionPhone(this, fbReader));
    	} else {
    		fbReader.addAction(ActionCode.SHOW_DIALOG_MENU, new ShowDialogMenuAction(this, fbReader));
    	}
		
		fbReader.addAction(ActionCode.ADD_BOOKMARK, new AddBookmarkAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_DIALOG_BOOKMARKS, new ShowDialogBookmarksAction(this, fbReader));
		
		if (isPhoneStyle()) {
			if (fbReader.Model != null && fbReader.Model.Book != null && fbReader.Model.Book.File != null) {
				String md5 = null;
		        OnyxMetadata metadata = OnyxCmsCenter.getMetadata(this, fbReader.Model.Book.File.getPhysicalFile().getPath());
		        if (metadata != null) {
		        	md5 = metadata.getMD5();
		        }
		        OnyxHistoryEntryHelper.recordStartReading(this, md5);
			}
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
	    ZLApplication.Instance().runAction(ActionCode.SHOW_DIALOG_MENU);
	    return false;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		final Uri data = intent.getData();
		fbReader = (FBReaderApp)FBReaderApp.Instance();
		if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
			super.onNewIntent(intent);
		} else if (Intent.ACTION_VIEW.equals(intent.getAction())
					&& data != null && "fbreader-action".equals(data.getScheme())) {
			fbReader.runAction(data.getEncodedSchemeSpecificPart(), data.getFragment());
		} else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			final String pattern = intent.getStringExtra(SearchManager.QUERY);
			doSearch(pattern);
		} else {
			super.onNewIntent(intent);
		}
	}

	void doSearch(final String pattern) {
		fbReader = (FBReaderApp)FBReaderApp.Instance();
		final Runnable runnable = new Runnable() {
			public void run() {
				final TextSearchPopup popup = (TextSearchPopup)fbReader.getPopupById(TextSearchPopup.ID);
				popup.initPosition();
				fbReader.TextSearchPatternOption.setValue(pattern);
				if (fbReader.getTextView().search(pattern, true, false, false, false) != 0) {
					runOnUiThread(new Runnable() {
						public void run() {
							if (mDialogSearchView != null) {
		                        if (!mDialogSearchView.isShowing()) {
		                            mDialogSearchView.show();
		                        }
		                    }

		                    SearchMenuHandler handler = new SearchMenuHandler(FBReader.this);
		                    mDialogSearchView = new DialogSearchView(FBReader.this, handler);
		                    mDialogSearchView.show();
						
						}
					});
				} else {
					runOnUiThread(new Runnable() {
						public void run() {
							UIUtil.showErrorMessage(FBReader.this, "textNotFound");
							popup.StartPosition = null;
						}
					});
				}
			}
		};
		UIUtil.wait("search", runnable, this);
	}

	@Override
	public void onStart() {
		super.onStart();

		initPluginActions();

		final ZLAndroidLibrary zlibrary = (ZLAndroidLibrary)ZLibrary.Instance();

		final int fullScreenFlag =
			zlibrary.ShowStatusBarOption.getValue() ? 0 : WindowManager.LayoutParams.FLAG_FULLSCREEN;
		if (fullScreenFlag != myFullScreenFlag) {
			finish();
			startActivity(new Intent(this, getClass()));
		}

		SetScreenOrientationAction.setOrientation(this, zlibrary.OrientationOption.getValue());

		final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();
		final RelativeLayout root = (RelativeLayout)findViewById(R.id.root_view);
		((PopupPanel)fbReader.getPopupById(TextSearchPopup.ID)).setPanelInfo(this, root);
		((PopupPanel)fbReader.getPopupById(NavigationPopup.ID)).setPanelInfo(this, root);
		((PopupPanel)fbReader.getPopupById(SelectionPopup.ID)).setPanelInfo(this, root);
	}

	private void initPluginActions() {
		final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();
		synchronized (myPluginActions) {
			int index = 0;
			while (index < myPluginActions.size()) {
				fbReader.removeAction(PLUGIN_ACTION_PREFIX + index++);
			}
			myPluginActions.clear();
		}

		sendOrderedBroadcast(
			new Intent(PluginApi.ACTION_REGISTER),
			null,
			myPluginInfoReceiver,
			null,
			RESULT_OK,
			null,
			null
		);
	}

	private class TipRunner extends Thread {
		TipRunner() {
			setPriority(MIN_PRIORITY);
		}

		public void run() {
			final TipsManager manager = TipsManager.Instance();
			switch (manager.requiredAction()) {
				case Initialize:
//					startActivity(new Intent(
//						TipsActivity.INITIALIZE_ACTION, null, FBReader.this, TipsActivity.class
//					));
					break;
				case Show:
//					startActivity(new Intent(
//						TipsActivity.SHOW_TIP_ACTION, null, FBReader.this, TipsActivity.class
//					));
					break;
				case Download:
					manager.startDownloading();
					break;
				case None:
					break;
			}
		}
	}

	   private Book createBookForFile(ZLFile file) {
	        if (file == null) {
	            return null;
	        }
	        Book book = Book.getByFile(file);
	        if (book != null) {
	            book.insertIntoBookList();
	            return book;
	        }
	        if (file.isArchive()) {
	            for (ZLFile child : file.children()) {
	                book = Book.getByFile(child);
	                if (book != null) {
	                    book.insertIntoBookList();
	                    return book;
	                }
	            }
	        }
	        return null;
	    }

	    private void emptyDifferentText()
	    {
	        final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();
	        Book book = createBookForFile(fileFromIntent(getIntent()));
	        if (fbReader == null) {
	            return;
	        }

	        if (book == null) {
	            if (fbReader.Model == null) {
	                book = Library.Instance().getRecentBook();
	                if (book == null || !book.File.exists()) {
	                    return;
	                }
	            }
	            if (book == null) {
	                return;
	            }
	        }

	        if (fbReader.Model == null) {
	            return;
	        }
	        if (fbReader.Model.Book != null && fbReader.Model.Book.File != null) {
	            if (book.File.getPath().equals(fbReader.Model.Book.File.getPath())) {
	                return;
	            }
	        }

	        if (fbReader.getBookTextView() != null) {
	            fbReader.getBookTextView().clearCaches();
	        }
	        if (fbReader.getfootnoteView() != null) {
	            fbReader.getfootnoteView().clearCaches();
	        }
	    }

	@Override
	public void onResume() {
		super.onResume();
		try {
			sendBroadcast(new Intent(getApplicationContext(), KillerCallback.class));
		} catch (Throwable t) {
		}

		emptyDifferentText();

		PopupPanel.restoreVisibilities(FBReaderApp.Instance());
		ApiServerImplementation.sendEvent(this, ApiListener.EVENT_READ_MODE_OPENED);
	}

	@Override
	public void onStop() {
		ApiServerImplementation.sendEvent(this, ApiListener.EVENT_READ_MODE_CLOSED);
		PopupPanel.removeAllWindows(FBReaderApp.Instance(), this);
		super.onStop();
	}
	
	@Override
	protected void onDestroy()
	{
	    ShowDialogMenuAction.shutdownTts();
	    if (isPhoneStyle()) {
	    	OnyxHistoryEntryHelper.recordFinishReading(this);
	    }
	    super.onDestroy();
	}

	@Override
	protected FBReaderApp createApplication() {
		if (SQLiteBooksDatabase.Instance() == null) {
			new SQLiteBooksDatabase(this, "READER");
		}
		return new FBReaderApp();
	}

	@Override
	public boolean onSearchRequested() {
		return super.onSearchRequested();
	}

	public boolean isPhoneStyle()
    {
        return (mScreenWidth == 480 && mScreenHeight == 800) || (mScreenHeight == 480 && mScreenWidth == 800);
    }
	
	private SelectionPopupMenu mSelectionPopupMenu = null;
	public void showSelectionPanel() {
	    final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();
	    final ZLTextView view = fbReader.getTextView();
	    if (mSelectionPopupMenu == null) {
	        final RelativeLayout root = (RelativeLayout)findViewById(R.id.root_view);
	        SelectionPopupMenuHandler handler = new SelectionPopupMenuHandler();
	        mSelectionPopupMenu = new SelectionPopupMenu(FBReader.this, handler, root);
	        mSelectionPopupMenu.move(view.getSelectionStartY(), view.getSelectionEndY());
	        mSelectionPopupMenu.show();
	    }
	    else if (!mSelectionPopupMenu.isShow()) {
	        mSelectionPopupMenu.move(view.getSelectionStartY(), view.getSelectionEndY());
	        mSelectionPopupMenu.show();
	    }

//	    ((SelectionPopup)fbReader.getPopupById(SelectionPopup.ID))
//	        .move(view.getSelectionStartY(), view.getSelectionEndY());
//	    fbReader.showPopup(SelectionPopup.ID);
	}

	public void hideSelectionPanel() {
	    if (mSelectionPopupMenu != null && mSelectionPopupMenu.isShow()) {
            mSelectionPopupMenu.hide();
        }

//		final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();
//		final FBReaderApp.PopupPanel popup = fbReader.getActivePopup();
//		if (popup != null && popup.getId() == SelectionPopup.ID) {
//			fbReader.hideActivePopup();
//		}
	}

	public SelectionPopupMenu getSelectionPopupMenu()
	{
	    return mSelectionPopupMenu;
	}
	
	public DialogSearchView getDialogSearchView()
	{
	    return mDialogSearchView;
	}

	private void onPreferencesUpdate(int resultCode) {
		final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();
		switch (resultCode) {
			case RESULT_DO_NOTHING:
				break;
			case RESULT_REPAINT:
			{
				AndroidFontUtil.clearFontCache();
				final BookModel model = fbReader.Model;
				if (model != null) {
					final Book book = model.Book;
					if (book != null) {
						book.reloadInfoFromDatabase();
						ZLTextHyphenator.Instance().load(book.getLanguage());
					}
				}
				fbReader.clearTextCaches();
				fbReader.getViewWidget().repaint();
				break;
			}
			case RESULT_RELOAD_BOOK:
				fbReader.reloadBook();
				break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_PREFERENCES:
			case REQUEST_BOOK_INFO:
				onPreferencesUpdate(resultCode);
				break;
			case REQUEST_CANCEL_MENU:
				((FBReaderApp)FBReaderApp.Instance()).runCancelAction(resultCode - 1);
				break;
		}
	}

	public void navigate() {
		((NavigationPopup)FBReaderApp.Instance().getPopupById(NavigationPopup.ID)).runNavigation();
	}

	private Menu addSubMenu(Menu menu, String id) {
		final ZLAndroidApplication application = (ZLAndroidApplication)getApplication();
		return application.myMainWindow.addSubMenu(menu, id);
	}

	private void addMenuItem(Menu menu, String actionId, String name) {
		final ZLAndroidApplication application = (ZLAndroidApplication)getApplication();
		application.myMainWindow.addMenuItem(menu, actionId, null, name);
	}

	private void addMenuItem(Menu menu, String actionId, int iconId) {
		final ZLAndroidApplication application = (ZLAndroidApplication)getApplication();
		application.myMainWindow.addMenuItem(menu, actionId, iconId, null);
	}

	private void addMenuItem(Menu menu, String actionId) {
		final ZLAndroidApplication application = (ZLAndroidApplication)getApplication();
		application.myMainWindow.addMenuItem(menu, actionId, null, null);
	}

	public ZLAndroidLibrary getALAndroidLibary() {
	    return (ZLAndroidLibrary) ZLibrary.Instance();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		addMenuItem(menu, ActionCode.INCREASE_FONT);
        addMenuItem(menu, ActionCode.DECREASE_FONT);
		addMenuItem(menu, ActionCode.SHOW_LIBRARY, R.drawable.ic_menu_library);
		addMenuItem(menu, ActionCode.INCREASE_LINESPACING);
		addMenuItem(menu, ActionCode.DECREASE_LINESPACING);
		addMenuItem(menu, ActionCode.SHOW_NETWORK_LIBRARY, R.drawable.ic_menu_networklibrary);
		addMenuItem(menu, ActionCode.SHOW_TOC, R.drawable.ic_menu_toc);
		addMenuItem(menu, ActionCode.SHOW_BOOKMARKS, R.drawable.ic_menu_bookmarks);
		addMenuItem(menu, ActionCode.SWITCH_TO_NIGHT_PROFILE, R.drawable.ic_menu_night);
		addMenuItem(menu, ActionCode.SWITCH_TO_DAY_PROFILE, R.drawable.ic_menu_day);
		addMenuItem(menu, ActionCode.SEARCH, R.drawable.ic_menu_search);
		addMenuItem(menu, ActionCode.SHARE_BOOK, R.drawable.ic_menu_search);
		addMenuItem(menu, ActionCode.SHOW_PREFERENCES);
		addMenuItem(menu, ActionCode.SHOW_BOOK_INFO);
		final Menu subMenu = addSubMenu(menu, "screenOrientation");
		addMenuItem(subMenu, ActionCode.SET_SCREEN_ORIENTATION_SYSTEM);
		addMenuItem(subMenu, ActionCode.SET_SCREEN_ORIENTATION_SENSOR);
		addMenuItem(subMenu, ActionCode.SET_SCREEN_ORIENTATION_PORTRAIT);
		addMenuItem(subMenu, ActionCode.SET_SCREEN_ORIENTATION_LANDSCAPE);
		if (ZLibrary.Instance().supportsAllOrientations()) {
			addMenuItem(subMenu, ActionCode.SET_SCREEN_ORIENTATION_REVERSE_PORTRAIT);
			addMenuItem(subMenu, ActionCode.SET_SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
		}
		addMenuItem(menu, ActionCode.SHOW_NAVIGATION);
		synchronized (myPluginActions) {
			int index = 0;
			for (PluginApi.ActionInfo info : myPluginActions) {
				if (info instanceof PluginApi.MenuActionInfo) {
					addMenuItem(
						menu,
						PLUGIN_ACTION_PREFIX + index++,
						((PluginApi.MenuActionInfo)info).MenuItemName
					);
				}
			}
		}

		final ZLAndroidApplication application = (ZLAndroidApplication)getApplication();
		application.myMainWindow.refresh();

		return true;
	}
}
