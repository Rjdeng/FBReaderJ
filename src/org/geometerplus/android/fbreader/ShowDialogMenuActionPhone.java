/**
 *
 */
package org.geometerplus.android.fbreader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geometerplus.android.fbreader.api.ApiException;
import org.geometerplus.android.fbreader.api.TextPosition;
import org.geometerplus.android.util.UIUtil;
import org.geometerplus.fbreader.bookmodel.TOCTree;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.fbreader.FBView;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.fbreader.library.Bookmark;
import org.geometerplus.fbreader.library.Library;
import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption;
import org.geometerplus.zlibrary.text.model.ZLTextModel;
import org.geometerplus.zlibrary.text.view.ZLTextElement;
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.text.view.ZLTextView.PagePosition;
import org.geometerplus.zlibrary.text.view.ZLTextWord;
import org.geometerplus.zlibrary.text.view.ZLTextWordCursor;
import org.geometerplus.zlibrary.text.view.style.ZLTextBaseStyle;
import org.geometerplus.zlibrary.text.view.style.ZLTextStyleCollection;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary;
import org.geometerplus.zlibrary.ui.android.view.AndroidFontUtil;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.onyx.android.sdk.tts.OnyxTtsSpeaker;
import com.onyx.android.sdk.ui.data.DirectoryItem;
import com.onyx.android.sdk.ui.dialog.DialogDirectory;
import com.onyx.android.sdk.ui.dialog.DialogDirectory.DirectoryTab;
import com.onyx.android.sdk.ui.dialog.DialogDirectoryPhone;
import com.onyx.android.sdk.ui.dialog.DialogFontFaceSettings;
import com.onyx.android.sdk.ui.dialog.DialogFontFaceSettings.onSettingsFontFaceListener;
import com.onyx.android.sdk.ui.dialog.DialogGotoPage;
import com.onyx.android.sdk.ui.dialog.DialogGotoPage.AcceptNumberListener;
import com.onyx.android.sdk.ui.dialog.DialogReaderMenuPhone;
import com.onyx.android.sdk.ui.dialog.DialogScreenRefresh;
import com.onyx.android.sdk.ui.dialog.DialogScreenRefresh.onScreenRefreshListener;
import com.onyx.android.sdk.ui.dialog.data.AnnotationItem;
import com.onyx.android.sdk.ui.dialog.data.IReaderMenuHandler;
/**
 * @author dxwts
 *
 */
public class ShowDialogMenuActionPhone extends FBAndroidAction
{

    private ArrayList<String> mFonts = null;
    private ZLTextBaseStyle mBaseStyle = null;

    private static DialogReaderMenuPhone sDialogReaderMenu;
    private static OnyxTtsSpeaker sTtsSpeaker = null;
    
    private FBReader mFbReader = null;
    
    private int myParagraphIndex = -1;
    private int myParagraphsNumber = 0;

    ShowDialogMenuActionPhone(FBReader baseActivity, FBReaderApp fbreader)
    {
        super(baseActivity, fbreader);
        mFbReader = baseActivity;
    }

    /* (non-Javadoc)
     * @see org.geometerplus.zlibrary.core.application.ZLApplication.ZLAction#run(java.lang.Object[])
     */
    @Override
    protected void run(Object... params)
    {
        final ZLTextStyleCollection collection = ZLTextStyleCollection.Instance();
        mBaseStyle = collection.getBaseStyle();

        IReaderMenuHandler menu_handler = new IReaderMenuHandler()
        {

            @Override
            public void updateCurrentPage(LinearLayout l)
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void showTTsView()
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void showTOC()
            {
                ShowDialogMenuActionPhone.this.showDirectoryDialog(DirectoryTab.toc);
            }

            @Override
            public void showSetFontView()
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void showLineSpacingView()
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void showBookMarks()
            {
                ShowDialogMenuActionPhone.this.showDirectoryDialog(DirectoryTab.bookmark);
            }

            @Override
            public void setLineSpacing(LineSpacingProperty property)
            {
                if(property == LineSpacingProperty.normal) {
                    ZLIntegerRangeOption option =
                            ZLTextStyleCollection.Instance().getBaseStyle().LineSpaceOption;
                        option.setValue(10);
                        Reader.clearTextCaches();
                        Reader.getViewWidget().repaint();

                } else if (property == LineSpacingProperty.big) {
                    ZLIntegerRangeOption option =
                            ZLTextStyleCollection.Instance().getBaseStyle().LineSpaceOption;
                        option.setValue(15);
                        Reader.clearTextCaches();
                        Reader.getViewWidget().repaint();
                } else if (property == LineSpacingProperty.small) {
                    ZLIntegerRangeOption option =
                            ZLTextStyleCollection.Instance().getBaseStyle().LineSpaceOption;
                        option.setValue(8);
                        Reader.clearTextCaches();
                        Reader.getViewWidget().repaint();
                }
                else if (property == LineSpacingProperty.enlarge) {
                    ZLApplication.Instance().runAction(ActionCode.INCREASE_LINESPACING);
                }
                else if (property == LineSpacingProperty.decreases) {
                    ZLApplication.Instance().runAction(ActionCode.DECREASE_LINESPACING);
                }
            }

            @Override
            public void setFontFace()
            {
                mFonts = new ArrayList<String>();
                final String optionValue = mBaseStyle.FontFamilyOption.getValue();
                AndroidFontUtil.fillFamiliesList(mFonts);
                String[] fontfoces = new String[mFonts.size()];
                for (int i = 0; i < mFonts.size(); i++) {
                    fontfoces[i] = mFonts.get(i);
                }
                DialogFontFaceSettings dlg = new DialogFontFaceSettings(BaseActivity, fontfoces, optionValue, null);
                dlg.show();
                dlg.setOnSettingsFontFaceListener(new onSettingsFontFaceListener()
                {

                    @Override
                    public void settingfontFace(int location)
                    {
                        mBaseStyle.FontFamilyOption.setValue(mFonts.get(location));
                        sDialogReaderMenu.setButtonFontFaceText(mFonts.get(location));
                        Reader.clearTextCaches();
                        Reader.getViewWidget().repaint();
                    }
                });
            }

            @Override
            public void startDictionary()
            {
                ZLApplication.Instance().runAction(ActionCode.DICTIONARY);
            }
            
            @Override
            public void searchContent()
            {
//                ZLApplication.Instance().doAction(ActionCode.SEARCH);
                ZLApplication.Instance().runAction(ActionCode.SEARCH);
            }
            
            @Override
			public void searchContent(String query) 
            {
				ZLApplication.Instance().runAction(ActionCode.SEARCH , query);	
			}

            @Override
            public void rotationScreen(int i)
            {
                if (i == -1) {
                    ZLApplication.Instance().runAction(ActionCode.SET_SCREEN_ORIENTATION_LANDSCAPE);
                }
                else {
                    ZLApplication.Instance().runAction(ActionCode.SET_SCREEN_ORIENTATION_PORTRAIT);
                }
            }
            
            @Override
            public boolean canPreviousNavigation()
            {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void previousNavigation()
            {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public boolean canNextNavigation()
            {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void nextNavigation()
            {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public boolean canPreviousPage()
            {
                return true;
            }

            @Override
            public void previousPage()
            {
                ZLApplication.Instance().runAction(ActionCode.TURN_PAGE_BACK);
            }
            
            @Override
            public boolean canNextPage()
            {
                return true;
            }

            @Override
            public void nextPage()
            {
                ZLApplication.Instance().runAction(ActionCode.TURN_PAGE_FORWARD);
            }

            @Override
            public void increaseFontSize()
            {
                ZLApplication.Instance().runAction(ActionCode.INCREASE_FONT);
            }

            @Override
            public void gotoPage(int i)
            {
                final ZLTextView view = (ZLTextView) ZLApplication.Instance().getCurrentView();
                if (i == 1) {
                    view.gotoHome();
                } else {
                    view.gotoPage(i);
                }
                ZLApplication.Instance().getCurrentView().Application.getViewWidget().reset();
                ZLApplication.Instance().getCurrentView().Application.getViewWidget().repaint();
            }

            @Override
            public int getPageIndex()
            {
                ZLApplication ZLApp = ZLApplication.Instance();
                FBView view = (FBView) ZLApp.getCurrentView();
                final PagePosition pos = view.pagePosition();
                return pos.Current;
            }

            @Override
            public int getPageCount()
            {
                ZLApplication ZLApp = ZLApplication.Instance();
                FBView view = (FBView) ZLApp.getCurrentView();
                final PagePosition pos = view.pagePosition();
                return pos.Total;
            }

            @Override
            public String getFontFace()
            {
                return mBaseStyle.FontFamilyOption.getValue();
            }

            @Override
            public void decreaseFontSize()
            {
                ZLApplication.Instance().runAction(ActionCode.DECREASE_FONT);
            }

            @Override
            public void changeRotationScreen(int orientation)
            {
                String rotation = ActionCode.SET_SCREEN_ORIENTATION_PORTRAIT;
                if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    rotation = ActionCode.SET_SCREEN_ORIENTATION_LANDSCAPE;
                }
                else if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                    rotation = ActionCode.SET_SCREEN_ORIENTATION_PORTRAIT;
                }
                else if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                    rotation = ActionCode.SET_SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                }
                else if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
                    rotation = ActionCode.SET_SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                }

                ZLApplication.Instance().runAction(rotation);
            }

            @Override
            public void changeFontsize(FontSizeProperty property)
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void toggleFontEmbolden()
            {
                mBaseStyle.BoldOption.setValue(!mBaseStyle.BoldOption.getValue());
                Reader.clearTextCaches();
                Reader.getViewWidget().repaint();
            }

            @Override
            public void showGoToPageDialog()
            {
                final DialogGotoPage dialogGotoPage = new DialogGotoPage(BaseActivity, null);
                dialogGotoPage.setAcceptNumberListener(new AcceptNumberListener()
                {

                    @Override
                    public void onAcceptNumber(int num)
                    {
                        final ZLTextView view = (ZLTextView) ZLApplication.Instance().getCurrentView();
                        if (num == 1) {
                            view.gotoHome();
                        } else {
                            view.gotoPage(num);
                        }
                        ZLApplication.Instance().getCurrentView().Application.getViewWidget().reset();
                        ZLApplication.Instance().getCurrentView().Application.getViewWidget().repaint();

                        dialogGotoPage.dismiss();
                        sDialogReaderMenu.dismiss();
                    }
                });
                dialogGotoPage.show();
            }

            @Override
            public void zoomToPage()
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void zoomToWidth()
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void zoomToHeight()
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void zoomBySelection()
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void zoomByTwoPoints()
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void zoomByValue(double z)
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void zoomIn()
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void zoomOut()
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void showAnnotation()
            {
                ShowDialogMenuActionPhone.this.showDirectoryDialog(DirectoryTab.annotation);
            }

            @Override
            public void toggleFullscreen()
            {
                final ZLAndroidLibrary zlibrary = (ZLAndroidLibrary)ZLAndroidLibrary.Instance();
                zlibrary.ShowStatusBarOption.setValue(!zlibrary.ShowStatusBarOption.getValue());
                sDialogReaderMenu.dismiss();

                WindowManager.LayoutParams params = mFbReader.getWindow().getAttributes();
                if (zlibrary.ShowStatusBarOption.getValue()) {
                    params.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
                } else {
                    params.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
                }
                mFbReader.getWindow().setAttributes(params);
                mFbReader.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            }

            @Override
            public boolean showZoomSettings()
            {
                return false;
            }
            
            @Override
            public boolean showSpacingSettings()
            {
                return true;
            }

            @Override
            public boolean isFullscreen()
            {
                return !((ZLAndroidLibrary)ZLAndroidLibrary.Instance()).ShowStatusBarOption.getValue();
            }

            @Override
            public void setScreenRefresh()
            {
                final ZLAndroidLibrary zlibrary = (ZLAndroidLibrary)ZLAndroidLibrary.Instance();
                DialogScreenRefresh dlg = new DialogScreenRefresh(BaseActivity, zlibrary.ScreenRefreshOption.getValue());
                dlg.setOnScreenRefreshListener(new onScreenRefreshListener()
                {

                    @Override
                    public void screenFefresh(int pageTurning)
                    {
                        zlibrary.ScreenRefreshOption.setValue(pageTurning);
                    }
                });
                dlg.show();
            }

            @Override
            public void showReaderSettings()
            {
            	 Intent intent = new Intent(mFbReader, ReaderSettingsActivity.class);
            	 mFbReader.startActivity(intent);
            }
            
            @Override
            public boolean ttsIsSpeaking()
            {
                if (sTtsSpeaker == null) {
                    return false;
                }
                
                return sTtsSpeaker.isActive() && !sTtsSpeaker.isPaused();
            }

            @Override
            public void ttsInit() {
                if (sTtsSpeaker == null) {
                    sTtsSpeaker = new OnyxTtsSpeaker(mFbReader);
                    sTtsSpeaker.setOnSpeakerCompletionListener(new OnyxTtsSpeaker.OnSpeakerCompletionListener()
                    {
                        
                        @Override
                        public void onSpeakerCompletion()
                        {
                            ++myParagraphIndex;
                            if (sTtsSpeaker.isActive()) {
                                if (!isPageEndOfText()) {
                                    sTtsSpeaker.startTts(gotoNextParagraph());
                                }
                                else {
                                    sTtsSpeaker.stop();
                                }
                            }
                        }
                    });
                }
                

            }

            @Override
            public void ttsSpeak() {
                assert(sTtsSpeaker != null);
                
                if (sTtsSpeaker.isPaused()) {
                    sTtsSpeaker.resume();
                } 
                else {
                    sTtsSpeaker.stop();

                    myParagraphIndex = ShowDialogMenuActionPhone.this.Reader.getTextView().getStartCursor().getParagraphIndex();
                    myParagraphsNumber = ShowDialogMenuActionPhone.this.Reader.Model.getTextModel().getParagraphsNumber();
                    sTtsSpeaker.startTts(gotoNextParagraph());
                }
            }

            @Override
            public void ttsPause() {
                assert(sTtsSpeaker != null);
                
                sTtsSpeaker.pause();
            }

            @Override
            public void ttsStop() {
                assert(sTtsSpeaker != null);
                
                sTtsSpeaker.stop();
                clearHighlighting();
            }

            @Override
            public boolean canChangeFontFace() {
            	return true;
            }
        };

        sDialogReaderMenu = new DialogReaderMenuPhone(BaseActivity, menu_handler);
        sDialogReaderMenu.setCanceledOnTouchOutside(true);
        sDialogReaderMenu.show();
    }

    public static void updatePage(int current, int total) {
        if (sDialogReaderMenu != null) {
            sDialogReaderMenu.setPageIndex(current);
            sDialogReaderMenu.setPageCount(total);
        }
    }
    
    /**
     * TODO ugly hacking for stop TTS
     */
    public static void shutdownTts()
    {
        if (sTtsSpeaker != null) {
            sTtsSpeaker.shutdown();
            sTtsSpeaker = null;
        }
    }

    private void showDirectoryDialog(DirectoryTab tab)
    {
        final FBReaderApp fbreader = (FBReaderApp)FBReaderApp.Instance();
        ArrayList<DirectoryItem> bookmarks = new ArrayList<DirectoryItem>();
        List<Bookmark>allBooksBookmarks = Library.Instance().allBookmarks();
        Collections.sort(allBooksBookmarks, new Bookmark.ByTimeComparator());

        if (fbreader.Model != null) {
            final long bookId = fbreader.Model.Book.getId();
            for (Bookmark bookmark : allBooksBookmarks) {
                if (bookmark.getBookId() == bookId) {
                    DirectoryItem item = new DirectoryItem(bookmark.getText(),bookmark.getBookmarkPage(),  bookmark);
                    bookmarks.add(item);
                }
            }
        }

        final TOCTree tocTree = fbreader.Model.TOCTree;
        ArrayList<DirectoryItem> TOCItems = new ArrayList<DirectoryItem>();
        if(tocTree.hasChildren()) {
            for (TOCTree t : tocTree) {
                if(t.getText() != null){
                    ZLTextView zlt = (ZLTextView)ZLApplication.Instance().getCurrentView();
                    ZLTextModel zltModel = zlt.getModel();
                    int textLength = zltModel.getTextLength(t.getReference().ParagraphIndex);
                    DirectoryItem item = new DirectoryItem(t.getText(), zlt.getPageNumber(textLength) + 1, t.getReference().ParagraphIndex);
                    TOCItems.add(item);
                }
            }
        }

        ArrayList<AnnotationItem> annotationItems = new ArrayList<AnnotationItem>();

        DialogDirectory.IGotoPageHandler gotoPageHandler = new DialogDirectory.IGotoPageHandler()
        {

            @Override
            public void jumpTOC(DirectoryItem item)
            {
                fbreader.addInvisibleBookmark();
                fbreader.BookTextView.gotoPosition(Integer.parseInt(item.getTag().toString()), 0, 0);
                fbreader.showBookTextView();
            }

            @Override
            public void jumpBookmark(DirectoryItem item)
            {
                Bookmark bookmark = (Bookmark) item.getTag();
                bookmark.onOpen();
                final FBReaderApp fbreader = (FBReaderApp)FBReaderApp.Instance();
                final long bookId = bookmark.getBookId();
                if ((fbreader.Model == null) || (fbreader.Model.Book.getId() != bookId)) {
                    final Book book = Book.getById(bookId);
                    if (book != null) {
                        fbreader.openBook(book, bookmark, null);
                    } else {
                        UIUtil.showErrorMessage(sDialogReaderMenu.getContext(), "cannotOpenBook");
                    }
                } else {
                    fbreader.gotoBookmark(bookmark);
                }
            }

            @Override
            public void jumpAnnotation(DirectoryItem item)
            {
                // TODO Auto-generated method stub

            }
        };

        DialogDirectoryPhone dialogDirectory = new DialogDirectoryPhone(BaseActivity, TOCItems, bookmarks, annotationItems, gotoPageHandler, tab);
        dialogDirectory.show();
    }

    private ZLTextFixedPosition getZLTextPosition(TextPosition position) {
        return new ZLTextFixedPosition(
            position.ParagraphIndex,
            position.ElementIndex,
            position.CharIndex
        );
    }
    
    public void highlightArea(TextPosition start, TextPosition end) {
        this.Reader.getTextView().highlight(
            getZLTextPosition(start),
            getZLTextPosition(end)
        );
    }
    
    public void clearHighlighting() {
        this.Reader.getTextView().clearHighlighting();
    }
    
    public void highlightParagraph() throws ApiException {
        if (0 <= myParagraphIndex && myParagraphIndex < myParagraphsNumber) {
            highlightArea(
                new TextPosition(myParagraphIndex, 0, 0),
                new TextPosition(myParagraphIndex, Integer.MAX_VALUE, 0)
            );
        } else {
            clearHighlighting();
        }
    }
    
    public String getParagraphText(int paragraphIndex) {
        final StringBuffer sb = new StringBuffer();
        final ZLTextWordCursor cursor = new ZLTextWordCursor(this.Reader.getTextView().getStartCursor());
        cursor.moveToParagraph(paragraphIndex);
        cursor.moveToParagraphStart();
        while (!cursor.isEndOfParagraph()) {
            ZLTextElement element = cursor.getElement();
            if (element instanceof ZLTextWord) {
                sb.append(element.toString() + " ");
            }
            cursor.nextWord();
        }
        return sb.toString();
    }
    
    public boolean isPageEndOfText() {
        final ZLTextWordCursor cursor = this.Reader.getTextView().getEndCursor();
        return cursor.isEndOfParagraph() && cursor.getParagraphCursor().isLast();
    }
    
    public void setPageStart(TextPosition position) {
        this.Reader.getTextView().gotoPosition(position.ParagraphIndex, position.ElementIndex, position.CharIndex);
        this.Reader.getViewWidget().repaint();
        this.Reader.storePosition();
    }
    
    public String gotoNextParagraph() {
        String text = "";
        for (; myParagraphIndex < myParagraphsNumber; ++myParagraphIndex) {
            final String s = getParagraphText(myParagraphIndex);
            if (s.length() > 0) {
                text = s;
                break;
            }
        }
        if (!"".equals(text) && !isPageEndOfText()) {
            setPageStart(new TextPosition(myParagraphIndex, 0, 0));
        }
            try {
                highlightParagraph();
            } catch (ApiException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        return text;
    }
}
