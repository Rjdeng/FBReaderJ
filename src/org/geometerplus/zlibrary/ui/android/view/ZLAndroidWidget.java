/*
 * Copyright (C) 2007-2012 Geometer Plus <contact@geometerplus.com>
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

package org.geometerplus.zlibrary.ui.android.view;

import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.library.Bookmark;
import org.geometerplus.fbreader.library.Library;
import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.view.ZLView;
import org.geometerplus.zlibrary.core.view.ZLViewWidget;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidActivity;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.onyx.android.sdk.device.EpdController;
import com.onyx.android.sdk.device.EpdController.UpdateMode;
import com.onyx.android.sdk.ui.dialog.DialogScreenRefresh;
import com.onyx.android.sdk.ui.util.BookmarkIcon;

public class ZLAndroidWidget extends View implements ZLViewWidget, View.OnLongClickListener {
    private final static String TAG = "ZLAndroidWidget";

	private final Paint myPaint = new Paint();
	private final BitmapManager myBitmapManager = new BitmapManager(this);
	private Bitmap myFooterBitmap;

	private Bitmap mBookmarkBitmap;
	
	private ITTSControl mITTSWorkListener = new ITTSControl()
    {
        @Override
        public void changeReadingPage()
        {
            //to do nothing
        }
    };

	public ZLAndroidWidget(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public ZLAndroidWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public ZLAndroidWidget(Context context) {
		super(context);
		init();
	}

	private void init() {
		// next line prevent ignoring first onKeyDown DPad event
		// after any dialog was closed
		setFocusableInTouchMode(true);
		setDrawingCacheEnabled(false);
		setOnLongClickListener(this);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		getAnimationProvider().terminate();
		if (myScreenIsTouched) {
			final ZLView view = ZLApplication.Instance().getCurrentView();
			myScreenIsTouched = false;
			view.onScrollingFinished(ZLView.PageIndex.current);
		}
	}

	@Override
	protected void onDraw(final Canvas canvas) {
		final Context context = getContext();
		if (context instanceof ZLAndroidActivity) {
			((ZLAndroidActivity)context).createWakeLock();
		} else {
			System.err.println("A surprise: view's context is not a ZLAndroidActivity");
		}
		super.onDraw(canvas);

//		final int w = getWidth();
//		final int h = getMainAreaHeight();

		if (getAnimationProvider().inProgress()) {
			onDrawInScrolling(canvas);
		} else {
			onDrawStatic(canvas);
			ZLApplication.Instance().onRepaintFinished();
		}
	}

	private AnimationProvider myAnimationProvider;
	private ZLView.Animation myAnimationType;
	private AnimationProvider getAnimationProvider() {
		final ZLView.Animation type = ZLApplication.Instance().getCurrentView().getAnimationType();
		if (myAnimationProvider == null || myAnimationType != type) {
			myAnimationType = type;
			switch (type) {
				case none:
					myAnimationProvider = new NoneAnimationProvider(myBitmapManager);
					break;
				case curl:
					myAnimationProvider = new CurlAnimationProvider(myBitmapManager);
					break;
				case slide:
					myAnimationProvider = new SlideAnimationProvider(myBitmapManager);
					break;
				case shift:
					myAnimationProvider = new ShiftAnimationProvider(myBitmapManager);
					break;
			}
		}
		return myAnimationProvider;
	}

	private void onDrawInScrolling(Canvas canvas) {
		final ZLView view = ZLApplication.Instance().getCurrentView();
//		final int w = getWidth();
//		final int h = getMainAreaHeight();

		final AnimationProvider animator = getAnimationProvider();
		final AnimationProvider.Mode oldMode = animator.getMode();
		animator.doStep();
		if (animator.inProgress()) {
			animator.draw(canvas);
			if (animator.getMode().Auto) {
				this.epdInvalidateHelper();
			}
			drawFooter(canvas);
			drawBookmarkIcon(canvas);
		} else {
			switch (oldMode) {
				case AnimatedScrollingForward:
				{
					final ZLView.PageIndex index = animator.getPageToScrollTo();
					myBitmapManager.shift(index == ZLView.PageIndex.next);
					view.onScrollingFinished(index);
					ZLApplication.Instance().onRepaintFinished();
					break;
				}
				case AnimatedScrollingBackward:
					view.onScrollingFinished(ZLView.PageIndex.current);
					break;
			}
			onDrawStatic(canvas);
		}
	}

	@Override
    public void reset() {
		myBitmapManager.reset();
	}

	@Override
    public void repaint() {
		postInvalidate();
	}

	@Override
    public void startManualScrolling(int x, int y, ZLView.Direction direction) {
		final AnimationProvider animator = getAnimationProvider();
		animator.setup(direction, getWidth(), getMainAreaHeight());
		animator.startManualScrolling(x, y);
	}

	@Override
    public void scrollManuallyTo(int x, int y) {
		final ZLView view = ZLApplication.Instance().getCurrentView();
		final AnimationProvider animator = getAnimationProvider();
		if (view.canScroll(animator.getPageToScrollTo(x, y))) {
			animator.scrollTo(x, y);
			this.epdInvalidateHelper();
		}
	}

	@Override
    public void startAnimatedScrolling(ZLView.PageIndex pageIndex, int x, int y, ZLView.Direction direction, int speed) {
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (pageIndex == ZLView.PageIndex.current || !view.canScroll(pageIndex)) {
			return;
		}
		final AnimationProvider animator = getAnimationProvider();
		animator.setup(direction, getWidth(), getMainAreaHeight());
		animator.startAnimatedScrolling(pageIndex, x, y, speed);
		if (animator.getMode().Auto) {
		    this.epdInvalidateHelper();
		}
	}

	@Override
    public void startAnimatedScrolling(ZLView.PageIndex pageIndex, ZLView.Direction direction, int speed) {
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (pageIndex == ZLView.PageIndex.current || !view.canScroll(pageIndex)) {
			return;
		}
		final AnimationProvider animator = getAnimationProvider();
		animator.setup(direction, getWidth(), getMainAreaHeight());
		animator.startAnimatedScrolling(pageIndex, null, null, speed);
		if (animator.getMode().Auto) {
		    this.epdInvalidateHelper();
		}
	}

	@Override
    public void startAnimatedScrolling(int x, int y, int speed) {
		final ZLView view = ZLApplication.Instance().getCurrentView();
		final AnimationProvider animator = getAnimationProvider();
		if (!view.canScroll(animator.getPageToScrollTo(x, y))) {
			animator.terminate();
			return;
		}
		animator.startAnimatedScrolling(x, y, speed);
		this.epdInvalidateHelper();
	}

	void drawOnBitmap(Bitmap bitmap, ZLView.PageIndex index) {
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (view == null) {
			return;
		}

		final ZLAndroidPaintContext context = new ZLAndroidPaintContext(
			new Canvas(bitmap),
			getWidth(),
			getMainAreaHeight(),
			view.isScrollbarShown() ? getVerticalScrollbarWidth() : 0
		);
		view.paint(context, index);
	}

	private void drawFooter(Canvas canvas) {
		final ZLView view = ZLApplication.Instance().getCurrentView();
		final ZLView.FooterArea footer = view.getFooterArea();

		if (footer == null) {
			myFooterBitmap = null;
			return;
		}

		if (myFooterBitmap != null &&
			(myFooterBitmap.getWidth() != getWidth() ||
			 myFooterBitmap.getHeight() != footer.getHeight())) {
			myFooterBitmap = null;
		}
		if (myFooterBitmap == null) {
			myFooterBitmap = Bitmap.createBitmap(getWidth(), footer.getHeight(), Bitmap.Config.RGB_565);
		}
		final ZLAndroidPaintContext context = new ZLAndroidPaintContext(
			new Canvas(myFooterBitmap),
			getWidth(),
			footer.getHeight(),
			view.isScrollbarShown() ? getVerticalScrollbarWidth() : 0
		);
		footer.paint(context);
		canvas.drawBitmap(myFooterBitmap, 0, getHeight() - footer.getHeight() - 5, myPaint);
	}

	private final int mBookmarkY = 0;
	private int mBookmarkX = 0;
	private void drawBookmarkIcon(Canvas canvas) {
	    final FBReaderApp fbreader = (FBReaderApp)FBReaderApp.Instance();
	    ZLTextPosition start_cursor = fbreader.getTextView().getStartCursor();
		ZLTextPosition end_cursor = fbreader.getTextView().getEndCursor();
	    if (fbreader != null && fbreader.Model != null) {
	        mBookmarkBitmap = BookmarkIcon.drawTriangle(false);
	        mBookmarkX = ZLAndroidWidget.this.getWidth() - mBookmarkBitmap.getWidth();
			for (Bookmark bookmark : Library.Instance().allBookmarks()) {
				if (bookmark.compareTo(start_cursor) >= 0 && bookmark.compareTo(end_cursor) < 0) {
					mBookmarkBitmap = BookmarkIcon.drawTriangle(true);
					break;
				}
			}
	        Paint paint = new Paint();
	        paint.setAlpha(220);
	        canvas.drawBitmap(mBookmarkBitmap, mBookmarkX, mBookmarkY, paint);
	    }
	}

	private int mPageRenderCount = 0;
	private ZLAndroidLibrary mZlibrary = (ZLAndroidLibrary)ZLAndroidLibrary.Instance();
	private void epdInvalidateHelper()
	{
	    mPageRenderCount++;
	    if (mPageRenderCount >= DialogScreenRefresh.RENDER_RESET_MAX_TIME) {
	        EpdController.invalidate(this, UpdateMode.GC);
	        mPageRenderCount = 0;
	    }
	    else {
	        EpdController.invalidate(this, UpdateMode.GU);
	    }
	}

	private void onDrawStatic(final Canvas canvas) {
	    Log.d(TAG, "onDrawStatic");
		myBitmapManager.setSize(getWidth(), getMainAreaHeight());
		canvas.drawBitmap(myBitmapManager.getBitmap(ZLView.PageIndex.current), 0, 0, myPaint);
		drawBookmarkIcon(canvas);
		drawFooter(canvas);

		new Thread() {
			@Override
			public void run() {
				final ZLView view = ZLApplication.Instance().getCurrentView();
				final ZLAndroidPaintContext context = new ZLAndroidPaintContext(
					canvas,
					getWidth(),
					getMainAreaHeight(),
					view.isScrollbarShown() ? getVerticalScrollbarWidth() : 0
				);
				view.preparePage(context, ZLView.PageIndex.next);
			}
		}.start();
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			onKeyDown(KeyEvent.KEYCODE_DPAD_CENTER, null);
		} else {
			ZLApplication.Instance().getCurrentView().onTrackballRotated((int)(10 * event.getX()), (int)(10 * event.getY()));
		}
		return true;
	}


	private class LongClickRunnable implements Runnable {
		@Override
        public void run() {
			if (performLongClick()) {
				myLongClickPerformed = true;
			}
		}
	}
	private volatile LongClickRunnable myPendingLongClickRunnable;
	private volatile boolean myLongClickPerformed;

	private void postLongClickRunnable() {
		myLongClickPerformed = false;
		myPendingPress = false;
		if (myPendingLongClickRunnable == null) {
			myPendingLongClickRunnable = new LongClickRunnable();
		}
		postDelayed(myPendingLongClickRunnable, 2 * ViewConfiguration.getLongPressTimeout());
	}

	private class ShortClickRunnable implements Runnable {
		@Override
        public void run() {
			final ZLView view = ZLApplication.Instance().getCurrentView();
			view.onFingerSingleTap(myPressedX, myPressedY);
			myPendingPress = false;
			myPendingShortClickRunnable = null;
		}
	}
	private volatile ShortClickRunnable myPendingShortClickRunnable;

	private volatile boolean myPendingPress;
	private volatile boolean myPendingDoubleTap;
	private int myPressedX, myPressedY;
	private boolean myScreenIsTouched;
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int x = (int)event.getX();
		int y = (int)event.getY();

		final ZLView view = ZLApplication.Instance().getCurrentView();
		switch (event.getAction()) {
			case MotionEvent.ACTION_UP:
				if (myPendingDoubleTap) {
					view.onFingerDoubleTap(x, y);
				} if (myLongClickPerformed) {
					view.onFingerReleaseAfterLongPress(x, y);
				} else {
					if (myPendingLongClickRunnable != null) {
						removeCallbacks(myPendingLongClickRunnable);
						myPendingLongClickRunnable = null;
					}
					if (myPendingPress) {
						if (view.isDoubleTapSupported()) {
							if (myPendingShortClickRunnable == null) {
								myPendingShortClickRunnable = new ShortClickRunnable();
							}
							postDelayed(myPendingShortClickRunnable, ViewConfiguration.getDoubleTapTimeout());
						} else {
						    if(mBookmarkBitmap != null && x >= mBookmarkX && x <= mBookmarkX + mBookmarkBitmap.getWidth() &&
						            y >= mBookmarkY && y <= mBookmarkY + mBookmarkBitmap.getHeight()) {
						        final FBReaderApp fbreader = (FBReaderApp)FBReaderApp.Instance();
						        final Bookmark bookmarkAdd = fbreader.addBookmark(20, true);
						        if (bookmarkAdd != null) {
						            for (Bookmark bookmark : Library.Instance().allBookmarks()) {
						                if (bookmark.getText().equals(bookmarkAdd.getText())) {
						                    bookmark.delete();
						                    ZLAndroidWidget.this.invalidate();
						                    return true;
						                }
						            }
						        }
						        ZLApplication.Instance().runAction(ActionCode.ADD_BOOKMARK);
						        ZLAndroidWidget.this.invalidate();
						    } else {
			                    view.onFingerSingleTap(x, y);
                            }
						}
					} else {
						view.onFingerRelease(x, y);
					}
				}
				myPendingDoubleTap = false;
				myPendingPress = false;
				myScreenIsTouched = false;
				break;
			case MotionEvent.ACTION_DOWN:
				if (myPendingShortClickRunnable != null) {
					removeCallbacks(myPendingShortClickRunnable);
					myPendingShortClickRunnable = null;
					myPendingDoubleTap = true;
				} else {
					postLongClickRunnable();
					myPendingPress = true;
				}
				myScreenIsTouched = true;
				myPressedX = x;
				myPressedY = y;
				break;
			case MotionEvent.ACTION_MOVE:
			{
				final int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
				final boolean isAMove =
					Math.abs(myPressedX - x) > slop || Math.abs(myPressedY - y) > slop;
				if (isAMove) {
					myPendingDoubleTap = false;
				}
				if (myLongClickPerformed) {
					view.onFingerMoveAfterLongPress(x, y);
				} else {
					if (myPendingPress) {
						if (isAMove) {
							if (myPendingShortClickRunnable != null) {
								removeCallbacks(myPendingShortClickRunnable);
								myPendingShortClickRunnable = null;
							}
							if (myPendingLongClickRunnable != null) {
								removeCallbacks(myPendingLongClickRunnable);
							}
							view.onFingerPress(myPressedX, myPressedY);
							myPendingPress = false;
						}
					}
					if (!myPendingPress) {
						view.onFingerMove(x, y);
					}
				}
				break;
			}
		}

		return true;
	}

	@Override
    public boolean onLongClick(View v) {
		final ZLView view = ZLApplication.Instance().getCurrentView();
		return view.onFingerLongPress(myPressedX, myPressedY);
	}

	private int myKeyUnderTracking = -1;
	private long myTrackingStartTime;

	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
		final ZLApplication application = ZLApplication.Instance();

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			final FBReaderApp fbreader = (FBReaderApp) FBReaderApp.Instance();
			if (!fbreader.getTextView().isSelectionEmpty()) {
				fbreader.getTextView().clearSelection();
				return true;
			}
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			ZLApplication.Instance().runAction(ActionCode.INCREASE_FONT);
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
			ZLApplication.Instance().runAction(ActionCode.DECREASE_FONT);
			return true;
		}
		else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
		    ZLApplication.Instance().runAction(ActionCode.VOLUME_KEY_SCROLL_BACK);
		    return true;
		}
		else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
		    ZLApplication.Instance().runAction(ActionCode.VOLUME_KEY_SCROLL_FORWARD);
		    return true;
		}
		if (application.hasActionForKey(keyCode, true) ||
			application.hasActionForKey(keyCode, false)) {
			if (myKeyUnderTracking != -1) {
				if (myKeyUnderTracking == keyCode) {
					return true;
				} else {
					myKeyUnderTracking = -1;
				}
			}
			if (application.hasActionForKey(keyCode, true)) {
				myKeyUnderTracking = keyCode;
				myTrackingStartTime = System.currentTimeMillis();
				return true;
			} else {
				return application.runActionByKey(keyCode, false);
			}
		} else {
			return false;
		}
	}

	@Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
		
		if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			 FBReaderApp fbreader = (FBReaderApp)FBReaderApp.Instance();
		        Bookmark bookmarkAdd = fbreader.addBookmark(20, true);
		        if (bookmarkAdd != null) {
		        	Log.d(TAG, "Library.Instance().allBookmarks()="+Library.Instance().allBookmarks().size());
		            for (Bookmark bookmark : Library.Instance().allBookmarks()) {
		                if (bookmark.getText().equals(bookmarkAdd.getText())) {
		                    bookmark.delete();
		                    ZLAndroidWidget.this.invalidate();
		                    return true;
		                }
		            }
		        }
		        ZLApplication.Instance().runAction(ActionCode.ADD_BOOKMARK);
		        ZLAndroidWidget.this.invalidate();
		}

		if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
	        mITTSWorkListener.changeReadingPage();
	        return true;
	    }
	    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
	        mITTSWorkListener.changeReadingPage();
	        return true;
	    }

		if (myKeyUnderTracking != -1) {
			if (myKeyUnderTracking == keyCode) {
				final boolean longPress = System.currentTimeMillis() >
					myTrackingStartTime + ViewConfiguration.getLongPressTimeout();
				ZLApplication.Instance().runActionByKey(keyCode, longPress);
			}
			myKeyUnderTracking = -1;
			return true;
		} else {
			final ZLApplication application = ZLApplication.Instance();
			return
				application.hasActionForKey(keyCode, false) ||
				application.hasActionForKey(keyCode, true);
		}

	}

	@Override
    protected int computeVerticalScrollExtent() {
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (!view.isScrollbarShown()) {
			return 0;
		}
		final AnimationProvider animator = getAnimationProvider();
		if (animator.inProgress()) {
			final int from = view.getScrollbarThumbLength(ZLView.PageIndex.current);
			final int to = view.getScrollbarThumbLength(animator.getPageToScrollTo());
			final int percent = animator.getScrolledPercent();
			return (from * (100 - percent) + to * percent) / 100;
		} else {
			return view.getScrollbarThumbLength(ZLView.PageIndex.current);
		}
	}

	@Override
    protected int computeVerticalScrollOffset() {
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (!view.isScrollbarShown()) {
			return 0;
		}
		final AnimationProvider animator = getAnimationProvider();
		if (animator.inProgress()) {
			final int from = view.getScrollbarThumbPosition(ZLView.PageIndex.current);
			final int to = view.getScrollbarThumbPosition(animator.getPageToScrollTo());
			final int percent = animator.getScrolledPercent();
			return (from * (100 - percent) + to * percent) / 100;
		} else {
			return view.getScrollbarThumbPosition(ZLView.PageIndex.current);
		}
	}

	@Override
    protected int computeVerticalScrollRange() {
		final ZLView view = ZLApplication.Instance().getCurrentView();
		if (!view.isScrollbarShown()) {
			return 0;
		}
		return view.getScrollbarFullSize();
	}

	private int getMainAreaHeight() {
		final ZLView.FooterArea footer = ZLApplication.Instance().getCurrentView().getFooterArea();
		return footer != null ? getHeight() - footer.getHeight() : getHeight();
	}

	public static interface ITTSControl{
	    public void changeReadingPage();
	}

	public void setOnTTSChangeRead(ITTSControl ttsWork) {
	    mITTSWorkListener = ttsWork;
	}
}
