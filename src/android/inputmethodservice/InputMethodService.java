/*
 * Copyright (C) 2007-2008 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package android.inputmethodservice;

import static android.view.ViewGroup.LayoutParams.FILL_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputBinding;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;

/**
 * InputMethodService provides a standard implementation of an InputMethod,
 * which final implementations can derive from and customize.  See the
 * base class {@link AbstractInputMethodService} and the {@link InputMethod}
 * interface for more information on the basics of writing input methods.
 */
public class InputMethodService extends AbstractInputMethodService {
    static final String TAG = "InputMethodService";
    static final boolean DEBUG = false;
    
    LayoutInflater mInflater;
    View mRootView;
    SoftInputWindow mWindow;
    boolean mWindowCreated;
    boolean mWindowAdded;
    boolean mWindowVisible;
    FrameLayout mExtractFrame;
    FrameLayout mCandidatesFrame;
    FrameLayout mInputFrame;
    
    IBinder mToken;
    
    InputBinding mInputBinding;
    InputConnection mInputConnection;
    boolean mInputStarted;
    EditorInfo mInputInfo;
    
    boolean mShowInputRequested;
    boolean mShowCandidatesRequested;
    
    boolean mFullscreenApplied;
    boolean mIsFullscreen;
    View mExtractView;
    ExtractEditText mExtractEditText;
    ExtractedText mExtractedText;
    int mExtractedToken;
    
    View mInputView;
    boolean mIsInputViewShown;
    
    int mStatusIcon;

    final Insets mTmpInsets = new Insets();
    final int[] mTmpLocation = new int[2];
    
    final ViewTreeObserver.OnComputeInternalInsetsListener mInsetsComputer =
            new ViewTreeObserver.OnComputeInternalInsetsListener() {
        public void onComputeInternalInsets(ViewTreeObserver.InternalInsetsInfo info) {
            if (isFullscreenMode()) {
                // In fullscreen mode, we just say the window isn't covering
                // any content so we don't impact whatever is behind.
                View decor = getWindow().getWindow().getDecorView();
                info.contentInsets.top = info.visibleInsets.top
                        = decor.getHeight();
                info.setTouchableInsets(ViewTreeObserver.InternalInsetsInfo.TOUCHABLE_INSETS_FRAME);
            } else {
                onComputeInsets(mTmpInsets);
                info.contentInsets.top = mTmpInsets.contentTopInsets;
                info.visibleInsets.top = mTmpInsets.visibleTopInsets;
                info.setTouchableInsets(mTmpInsets.touchableInsets);
            }
        }
    };

    /**
     * Concrete implementation of
     * {@link AbstractInputMethodService.AbstractInputMethodImpl} that provides
     * all of the standard behavior for an input method.
     */
    public class InputMethodImpl extends AbstractInputMethodImpl {
        /**
         * Take care of attaching the given window token provided by the system.
         */
        public void attachToken(IBinder token) {
            if (mToken == null) {
                mToken = token;
                mWindow.setToken(token);
            }
        }
        
        /**
         * Handle a new input binding, calling
         * {@link InputMethodService#onBindInput InputMethodService.onBindInput()}
         * when done.
         */
        public void bindInput(InputBinding binding) {
            mInputBinding = binding;
            mInputConnection = binding.getConnection();
            onBindInput();
        }

        /**
         * Clear the current input binding.
         */
        public void unbindInput() {
            mInputStarted = false;
            mInputBinding = null;
            mInputConnection = null;
        }

        public void startInput(EditorInfo attribute) {
            doStartInput(attribute, false);
        }

        public void restartInput(EditorInfo attribute) {
            doStartInput(attribute, false);
        }

        /**
         * Handle a request by the system to hide the soft input area.
         */
        public void hideSoftInput() {
            if (DEBUG) Log.v(TAG, "hideSoftInput()");
            mShowInputRequested = false;
            hideWindow();
        }

        /**
         * Handle a request by the system to show the soft input area.
         */
        public void showSoftInput() {
            if (DEBUG) Log.v(TAG, "showSoftInput()");
            showWindow(true);
        }
    }
    
    /**
     * Concrete implementation of
     * {@link AbstractInputMethodService.AbstractInputMethodSessionImpl} that provides
     * all of the standard behavior for an input method session.
     */
    public class InputMethodSessionImpl extends AbstractInputMethodSessionImpl {
        public void finishInput() {
            if (!isEnabled()) {
                return;
            }
            onFinishInput();
            mInputStarted = false;
        }

        /**
         * Call {@link InputMethodService#onDisplayCompletions
         * InputMethodService.onDisplayCompletions()}.
         */
        public void displayCompletions(CompletionInfo[] completions) {
            if (!isEnabled()) {
                return;
            }
            onDisplayCompletions(completions);
        }
        
        /**
         * Call {@link InputMethodService#onUpdateExtractedText
         * InputMethodService.onUpdateExtractedText()}.
         */
        public void updateExtractedText(int token, ExtractedText text) {
            if (!isEnabled()) {
                return;
            }
            onUpdateExtractedText(token, text);
        }
        
        /**
         * Call {@link InputMethodService#onUpdateSelection
         * InputMethodService.onUpdateSelection()}.
         */
        public void updateSelection(int oldSelStart, int oldSelEnd,
                int newSelStart, int newSelEnd) {
            if (!isEnabled()) {
                return;
            }
            InputMethodService.this.onUpdateSelection(oldSelStart, oldSelEnd,
                    newSelStart, newSelEnd);
        }
        
        /**
         * Call {@link InputMethodService#onUpdateCursor
         * InputMethodService.onUpdateCursor()}.
         */
        public void updateCursor(Rect newCursor) {
            if (!isEnabled()) {
                return;
            }
            InputMethodService.this.onUpdateCursor(newCursor);
        }
        
        /**
         * Call {@link InputMethodService#onAppPrivateCommand
         * InputMethodService.onAppPrivateCommand()}.
         */
        public void appPrivateCommand(String action, Bundle data) {
            if (!isEnabled()) {
                return;
            }
            InputMethodService.this.onAppPrivateCommand(action, data);
        }
    }
    
    /**
     * Information about where interesting parts of the input method UI appear.
     */
    public static final class Insets {
        /**
         * This is the top part of the UI that is the main content.  It is
         * used to determine the basic space needed, to resize/pan the
         * application behind.  It is assumed that this inset does not
         * change very much, since any change will cause a full resize/pan
         * of the application behind.  This value is relative to the top edge
         * of the input method window.
         */
        int contentTopInsets;
        
        /**
         * This is the top part of the UI that is visibly covering the
         * application behind it.  This provides finer-grained control over
         * visibility, allowing you to change it relatively frequently (such
         * as hiding or showing candidates) without disrupting the underlying
         * UI too much.  For example, this will never resize the application
         * UI, will only pan if needed to make the current focus visible, and
         * will not aggressively move the pan position when this changes unless
         * needed to make the focus visible.  This value is relative to the top edge
         * of the input method window.
         */
        int visibleTopInsets;
        
        /**
         * Option for {@link #touchableInsets}: the entire window frame
         * can be touched.
         */
        public static final int TOUCHABLE_INSETS_FRAME
                = ViewTreeObserver.InternalInsetsInfo.TOUCHABLE_INSETS_FRAME;
        
        /**
         * Option for {@link #touchableInsets}: the area inside of
         * the content insets can be touched.
         */
        public static final int TOUCHABLE_INSETS_CONTENT
                = ViewTreeObserver.InternalInsetsInfo.TOUCHABLE_INSETS_CONTENT;
        
        /**
         * Option for {@link #touchableInsets}: the area inside of
         * the visible insets can be touched.
         */
        public static final int TOUCHABLE_INSETS_VISIBLE
                = ViewTreeObserver.InternalInsetsInfo.TOUCHABLE_INSETS_VISIBLE;
        
        /**
         * Determine which area of the window is touchable by the user.  May
         * be one of: {@link #TOUCHABLE_INSETS_FRAME},
         * {@link #TOUCHABLE_INSETS_CONTENT}, or {@link #TOUCHABLE_INSETS_VISIBLE}. 
         */
        public int touchableInsets;
    }
    
    @Override public void onCreate() {
        super.onCreate();
        mInflater = (LayoutInflater)getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mWindow = new SoftInputWindow(this);
        initViews();
    }
    
    void initViews() {
        mWindowVisible = false;
        mWindowCreated = false;
        mShowInputRequested = false;
        mShowCandidatesRequested = false;
        
        mRootView = mInflater.inflate(
                com.android.internal.R.layout.input_method, null);
        mWindow.setContentView(mRootView);
        mRootView.getViewTreeObserver().addOnComputeInternalInsetsListener(mInsetsComputer);
        
        mExtractFrame = (FrameLayout)mRootView.findViewById(android.R.id.extractArea);
        mExtractView = null;
        mExtractEditText = null;
        mFullscreenApplied = false;
        
        mCandidatesFrame = (FrameLayout)mRootView.findViewById(android.R.id.candidatesArea);
        mInputFrame = (FrameLayout)mRootView.findViewById(android.R.id.inputArea);
        mInputView = null;
        mIsInputViewShown = false;
        
        mExtractFrame.setVisibility(View.GONE);
        mCandidatesFrame.setVisibility(View.GONE);
        mInputFrame.setVisibility(View.GONE);
    }
    
    @Override public void onDestroy() {
        super.onDestroy();
        mRootView.getViewTreeObserver().removeOnComputeInternalInsetsListener(
                mInsetsComputer);
        if (mWindowAdded) {
            mWindow.dismiss();
        }
    }
    
    /**
     * Implement to return our standard {@link InputMethodImpl}.  Subclasses
     * can override to provide their own customized version.
     */
    public AbstractInputMethodImpl onCreateInputMethodInterface() {
        return new InputMethodImpl();
    }
    
    /**
     * Implement to return our standard {@link InputMethodSessionImpl}.  Subclasses
     * can override to provide their own customized version.
     */
    public AbstractInputMethodSessionImpl onCreateInputMethodSessionInterface() {
        return new InputMethodSessionImpl();
    }
    
    public LayoutInflater getLayoutInflater() {
        return mInflater;
    }
    
    public Dialog getWindow() {
        return mWindow;
    }
    
    /**
     * Return the currently active InputBinding for the input method, or
     * null if there is none.
     */
    public InputBinding getCurrentInputBinding() {
        return mInputBinding;
    }
    
    /**
     * Retrieve the currently active InputConnection that is bound to
     * the input method, or null if there is none.
     */
    public InputConnection getCurrentInputConnection() {
        return mInputConnection;
    }
    
    public boolean getCurrentInputStarted() {
        return mInputStarted;
    }
    
    public EditorInfo getCurrentInputInfo() {
        return mInputInfo;
    }
    
    /**
     * Re-evaluate whether the input method should be running in fullscreen
     * mode, and update its UI if this has changed since the last time it
     * was evaluated.  This will call {@link #onEvaluateFullscreenMode()} to
     * determine whether it should currently run in fullscreen mode.  You
     * can use {@link #isFullscreenMode()} to determine if the input method
     * is currently running in fullscreen mode.
     */
    public void updateFullscreenMode() {
        boolean isFullscreen = onEvaluateFullscreenMode();
        if (mIsFullscreen != isFullscreen || !mFullscreenApplied) {
            mIsFullscreen = isFullscreen;
            mFullscreenApplied = true;
            mWindow.getWindow().setBackgroundDrawable(
                    onCreateBackgroundDrawable());
            mExtractFrame.setVisibility(isFullscreen ? View.VISIBLE : View.GONE);
            if (isFullscreen) {
                if (mExtractView == null) {
                    View v = onCreateExtractTextView();
                    if (v != null) {
                        setExtractView(v);
                    }
                }
                startExtractingText();
                mWindow.getWindow().setLayout(FILL_PARENT, FILL_PARENT);
            } else {
                mWindow.getWindow().setLayout(WRAP_CONTENT, WRAP_CONTENT);
            }
        }
    }
    
    /**
     * Return whether the input method is <em>currently</em> running in
     * fullscreen mode.  This is the mode that was last determined and
     * applied by {@link #updateFullscreenMode()}.
     */
    public boolean isFullscreenMode() {
        return mIsFullscreen;
    }
    
    /**
     * Override this to control when the input method should run in
     * fullscreen mode.  The default implementation runs in fullsceen only
     * when the screen is in landscape mode and the input view is being
     * shown ({@link #onEvaluateInputViewShown} returns true).  If you change what
     * this returns, you will need to call {@link #updateFullscreenMode()}
     * yourself whenever the returned value may have changed to have it
     * re-evaluated and applied.
     */
    public boolean onEvaluateFullscreenMode() {
        Configuration config = getResources().getConfiguration();
        return config.orientation == Configuration.ORIENTATION_LANDSCAPE
                && onEvaluateInputViewShown();
    }
    
    /**
     * Compute the interesting insets into your UI.  The default implementation
     * uses the top of the candidates frame for the visible insets, and the
     * top of the input frame for the content insets.  The default touchable
     * insets are {@link Insets#TOUCHABLE_INSETS_VISIBLE}.
     * 
     * <p>Note that this method is not called when in fullscreen mode, since
     * in that case the application is left as-is behind the input method and
     * not impacted by anything in its UI.
     * 
     * @param outInsets Fill in with the current UI insets.
     */
    public void onComputeInsets(Insets outInsets) {
        int[] loc = mTmpLocation;
        if (mInputFrame.getVisibility() == View.VISIBLE) {
            mInputFrame.getLocationInWindow(loc);
            outInsets.contentTopInsets = loc[1];
        }
        if (mCandidatesFrame.getVisibility() == View.VISIBLE) {
            mCandidatesFrame.getLocationInWindow(loc);
            outInsets.visibleTopInsets = loc[1];
        } else {
            outInsets.visibleTopInsets = loc[1];
        }
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_VISIBLE;
    }
    
    /**
     * Re-evaluate whether the soft input area should currently be shown, and
     * update its UI if this has changed since the last time it
     * was evaluated.  This will call {@link #onEvaluateInputViewShown()} to
     * determine whether the input view should currently be shown.  You
     * can use {@link #isInputViewShown()} to determine if the input view
     * is currently shown.
     */
    public void updateInputViewShown() {
        boolean isShown = onEvaluateInputViewShown();
        if (mIsInputViewShown != isShown && mWindowVisible) {
            mIsInputViewShown = isShown;
            mInputFrame.setVisibility(isShown ? View.VISIBLE : View.GONE);
            if (mInputView == null) {
                View v = onCreateInputView();
                if (v != null) {
                    setInputView(v);
                }
            }
        }
    }
    
    /**
     * Return whether the soft input view is <em>currently</em> shown to the
     * user.  This is the state that was last determined and
     * applied by {@link #updateInputViewShown()}.
     */
    public boolean isInputViewShown() {
        return mIsInputViewShown;
    }
    
    /**
     * Override this to control when the soft input area should be shown to
     * the user.  The default implementation only shows the input view when
     * there is no hard keyboard or the keyboard is hidden.  If you change what
     * this returns, you will need to call {@link #updateInputViewShown()}
     * yourself whenever the returned value may have changed to have it
     * re-evalauted and applied.
     */
    public boolean onEvaluateInputViewShown() {
        Configuration config = getResources().getConfiguration();
        return config.keyboard == Configuration.KEYBOARD_NOKEYS
                || config.hardKeyboardHidden == Configuration.KEYBOARDHIDDEN_YES;
    }
    
    /**
     * Controls the visibility of the candidates display area.  By default
     * it is hidden.
     */
    public void setCandidatesViewShown(boolean shown) {
        if (mShowCandidatesRequested != shown) {
            mCandidatesFrame.setVisibility(shown ? View.VISIBLE : View.INVISIBLE);
            if (!mShowInputRequested) {
                // If we are being asked to show the candidates view while the app
                // has not asked for the input view to be shown, then we need
                // to update whether the window is shown.
                if (shown) {
                    showWindow(false);
                } else {
                    hideWindow();
                }
            }
            mShowCandidatesRequested = shown;
        }
    }
    
    public void setStatusIcon(int iconResId) {
        mStatusIcon = iconResId;
        if (mInputConnection != null && mWindowVisible) {
            mInputConnection.showStatusIcon(getPackageName(), iconResId);
        }
    }
    
    /**
     * Force switch to a new input method, as identified by <var>id</var>.  This
     * input method will be destroyed, and the requested one started on the
     * current input field.
     * 
     * @param id Unique identifier of the new input method ot start.
     */
    public void switchInputMethod(String id) {
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE))
                .setInputMethod(mToken, id);
    }
    
    public void setExtractView(View view) {
        mExtractFrame.removeAllViews();
        mExtractFrame.addView(view, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        mExtractView = view;
        if (view != null) {
            mExtractEditText = (ExtractEditText)view.findViewById(
                    com.android.internal.R.id.inputExtractEditText);
            startExtractingText();
        } else {
            mExtractEditText = null;
        }
    }
    
    /**
     * Replaces the current candidates view with a new one.  You only need to
     * call this when dynamically changing the view; normally, you should
     * implement {@link #onCreateCandidatesView()} and create your view when
     * first needed by the input method.
     */
    public void setCandidatesView(View view) {
        mCandidatesFrame.removeAllViews();
        mCandidatesFrame.addView(view, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }
    
    /**
     * Replaces the current input view with a new one.  You only need to
     * call this when dynamically changing the view; normally, you should
     * implement {@link #onCreateInputView()} and create your view when
     * first needed by the input method.
     */
    public void setInputView(View view) {
        mInputFrame.removeAllViews();
        mInputFrame.addView(view, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        mInputView = view;
    }
    
    /**
     * Called by the framework to create a Drawable for the background of
     * the input method window.  May return null for no background.  The default
     * implementation returns a non-null standard background only when in
     * fullscreen mode.
     */
    public Drawable onCreateBackgroundDrawable() {
        if (isFullscreenMode()) {
            return getResources().getDrawable(
                    com.android.internal.R.drawable.input_method_fullscreen_background);
        }
        return null;
    }
    
    /**
     * Called by the framework to create the layout for showing extacted text.
     * Only called when in fullscreen mode.  The returned view hierarchy must
     * have an {@link ExtractEditText} whose ID is 
     * {@link android.R.id#inputExtractEditText}.
     */
    public View onCreateExtractTextView() {
        return mInflater.inflate(
                com.android.internal.R.layout.input_method_extract_view, null);
    }
    
    /**
     * Create and return the view hierarchy used to show candidates.  This will
     * be called once, when the candidates are first displayed.  You can return
     * null to have no candidates view; the default implementation returns null.
     * 
     * <p>To control when the candidates view is displayed, use
     * {@link #setCandidatesViewShown(boolean)}.
     * To change the candidates view after the first one is created by this
     * function, use {@link #setCandidatesView(View)}.
     */
    public View onCreateCandidatesView() {
        return null;
    }
    
    /**
     * Create and return the view hierarchy used for the input area (such as
     * a soft keyboard).  This will be called once, when the input area is
     * first displayed.  You can return null to have no input area; the default
     * implementation returns null.
     * 
     * <p>To control when the input view is displayed, implement
     * {@link #onEvaluateInputViewShown()}.
     * To change the input view after the first one is created by this
     * function, use {@link #setInputView(View)}.
     */
    public View onCreateInputView() {
        return null;
    }
    
    /**
     * Called when an input session is starting or restarting.
     * 
     * @param info Description of the type of text being edited.
     * @param restarting Set to true if we are restarting input on the
     * same text field as before.
     */
    public void onStartInputView(EditorInfo info, boolean restarting) {
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        boolean visible = mWindowVisible;
        boolean showingInput = mShowInputRequested;
        boolean showingCandidates = mShowCandidatesRequested;
        initViews();
        if (visible) {
            if (showingCandidates) {
                setCandidatesViewShown(true);
            }
            showWindow(showingInput);
        }
    }

    public void showWindow(boolean showInput) {
        if (DEBUG) Log.v(TAG, "Showing window: showInput=" + showInput
                + " mShowInputRequested=" + mShowInputRequested
                + " mWindowAdded=" + mWindowAdded
                + " mWindowCreated=" + mWindowCreated
                + " mWindowVisible=" + mWindowVisible
                + " mInputStarted=" + mInputStarted);
        boolean doShowInput = false;
        boolean wasVisible = mWindowVisible;
        mWindowVisible = true;
        if (!mShowInputRequested) {
            doShowInput = true;
            mShowInputRequested = true;
        } else {
            showInput = true;
        }
        
        if (doShowInput) {
            if (DEBUG) Log.v(TAG, "showWindow: updating UI");
            updateFullscreenMode();
            updateInputViewShown();
        }
        
        if (!mWindowAdded || !mWindowCreated) {
            mWindowAdded = true;
            mWindowCreated = true;
            View v = onCreateCandidatesView();
            if (DEBUG) Log.v(TAG, "showWindow: candidates=" + v);
            if (v != null) {
                setCandidatesView(v);
            }
        }
        if (doShowInput) {
            if (mInputStarted) {
                if (DEBUG) Log.v(TAG, "showWindow: starting input view");
                onStartInputView(mInputInfo, false);
            }
            startExtractingText();
        }
        
        if (!wasVisible) {
            if (DEBUG) Log.v(TAG, "showWindow: showing!");
            mWindow.show();
            if (mInputConnection != null) {
                mInputConnection.showStatusIcon(getPackageName(), mStatusIcon);
            }
        }
    }
    
    public void hideWindow() {
        if (mWindowVisible) {
            mWindow.hide();
            mWindowVisible = false;
            if (mInputConnection != null) {
                mInputConnection.hideStatusIcon();
            }
        }
    }
    
    public void onBindInput() {
    }
    
    public void onStartInput(EditorInfo attribute, boolean restarting) {
    }
    
    void doStartInput(EditorInfo attribute, boolean restarting) {
        mInputStarted = true;
        mInputInfo = attribute;
        onStartInput(attribute, restarting);
        if (mWindowVisible) {
            if (mWindowCreated) {
                onStartInputView(mInputInfo, restarting);
            }
            startExtractingText();
        }
    }
    
    public void onFinishInput() {
    }
    
    /**
     * Called when the application has reported auto-completion candidates that
     * it would like to have the input method displayed.  Typically these are
     * only used when an input method is running in full-screen mode, since
     * otherwise the user can see and interact with the pop-up window of
     * completions shown by the application.
     * 
     * <p>The default implementation here does nothing.
     */
    public void onDisplayCompletions(CompletionInfo[] completions) {
    }
    
    /**
     * Called when the application has reported new extracted text to be shown
     * due to changes in its current text state.  The default implementation
     * here places the new text in the extract edit text, when the input
     * method is running in fullscreen mode.
     */
    public void onUpdateExtractedText(int token, ExtractedText text) {
        if (mExtractedToken != token) {
            return;
        }
        if (mExtractEditText != null && text != null) {
            mExtractedText = text;
            mExtractEditText.setExtractedText(text);
        }
    }
    
    /**
     * Called when the application has reported a new selection region of
     * the text.  This is called whether or not the input method has requested
     * extracted text updates, although if so it will not receive this call
     * if the extracted text has changed as well.
     * 
     * <p>The default implementation takes care of updating the cursor in
     * the extract text, if it is being shown.
     */
    public void onUpdateSelection(int oldSelStart, int oldSelEnd,
            int newSelStart, int newSelEnd) {
        if (mExtractEditText != null && mExtractedText != null) {
            final int off = mExtractedText.startOffset;
            mExtractEditText.setSelection(newSelStart-off, newSelEnd-off);
        }
    }

    /**
     * Called when the application has reported a new location of its text
     * cursor.  This is only called if explicitly requested by the input method.
     * The default implementation does nothing.
     */
    public void onUpdateCursor(Rect newCursor) {
    }

    /**
     * Close this input method's soft input area, removing it from the display.
     * The input method will continue running, but the user can no longer use
     * it to generate input by touching the screen.
     */
    public void dismissSoftInput() {
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE))
                .hideSoftInputFromInputMethod(mToken);
    }
    
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mWindowVisible && event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            dismissSoftInput();
            return true;
        }
        return false;
    }

    public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onTrackballEvent(MotionEvent event) {
        return false;
    }

    public void onAppPrivateCommand(String action, Bundle data) {
    }
    
    void startExtractingText() {
        if (mExtractEditText != null && getCurrentInputStarted()
                && isFullscreenMode()) {
            mExtractedToken++;
            ExtractedTextRequest req = new ExtractedTextRequest();
            req.token = mExtractedToken;
            req.hintMaxLines = 10;
            req.hintMaxChars = 10000;
            mExtractedText = mInputConnection.getExtractedText(req,
                    InputConnection.EXTRACTED_TEXT_MONITOR);
            if (mExtractedText != null) {
                mExtractEditText.setExtractedText(mExtractedText);
            }
            mExtractEditText.setInputType(getCurrentInputInfo().inputType);
            mExtractEditText.setHint(mInputInfo.hintText);
        }
    }
}
