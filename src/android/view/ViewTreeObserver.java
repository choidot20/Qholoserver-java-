/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.view;

import android.graphics.Rect;

import java.util.ArrayList;

/**
 * A view tree observer is used to register listeners that can be notified of global
 * changes in the view tree. Such global events include, but are not limited to,
 * layout of the whole tree, beginning of the drawing pass, touch mode change....
 *
 * A ViewTreeObserver should never be instantiated by applications as it is provided
 * by the views hierarchy. Refer to {@link android.view.View#getViewTreeObserver()}
 * for more information.
 */
public final class ViewTreeObserver {
    private ArrayList<OnGlobalFocusChangeListener> mOnGlobalFocusListeners;
    private ArrayList<OnGlobalLayoutListener> mOnGlobalLayoutListeners;
    private ArrayList<OnPreDrawListener> mOnPreDrawListeners;
    private ArrayList<OnTouchModeChangeListener> mOnTouchModeChangeListeners;
    private ArrayList<OnComputeInternalInsetsListener> mOnComputeInternalInsetsListeners;

    private boolean mAlive = true;

    /**
     * Interface definition for a callback to be invoked when the focus state within
     * the view tree changes.
     */
    public interface OnGlobalFocusChangeListener {
        /**
         * Callback method to be invoked when the focus changes in the view tree. When
         * the view tree transitions from touch mode to non-touch mode, oldFocus is null.
         * When the view tree transitions from non-touch mode to touch mode, newFocus is
         * null. When focus changes in non-touch mode (without transition from or to
         * touch mode) either oldFocus or newFocus can be null.
         *
         * @param oldFocus The previously focused view, if any.
         * @param newFocus The newly focused View, if any.
         */
        public void onGlobalFocusChanged(View oldFocus, View newFocus);
    }

    /**
     * Interface definition for a callback to be invoked when the global layout state
     * or the visibility of views within the view tree changes.
     */
    public interface OnGlobalLayoutListener {
        /**
         * Callback method to be invoked when the global layout state or the visibility of views
         * within the view tree changes
         */
        public void onGlobalLayout();
    }

    /**
     * Interface definition for a callback to be invoked when the view tree is about to be drawn.
     */
    public interface OnPreDrawListener {
        /**
         * Callback method to be invoked when the view tree is about to be drawn. At this point, all
         * views in the tree have been measured and given a frame. Clients can use this to adjust
         * their scroll bounds or even to request a new layout before drawing occurs.
         *
         * @return Return true to proceed with the current drawing pass, or false to cancel.
         *
         * @see android.view.View#onMeasure
         * @see android.view.View#onLayout
         * @see android.view.View#onDraw
         */
        public boolean onPreDraw();
    }

    /**
     * Interface definition for a callback to be invoked when the touch mode changes.
     */
    public interface OnTouchModeChangeListener {
        /**
         * Callback method to be invoked when the touch mode changes.
         *
         * @param isInTouchMode True if the view hierarchy is now in touch mode, false  otherwise.
         */
        public void onTouchModeChanged(boolean isInTouchMode);
    }

    /**
     * Parameters used with OnComputeInternalInsetsListener.
     * {@hide pending API Council approval}
     */
    public final static class InternalInsetsInfo {
        /**
         * Offsets from the frame of the window at which the content of
         * windows behind it should be placed.
         */
        public final Rect contentInsets = new Rect();
        
        /**
         * Offsets from the fram of the window at which windows behind it
         * are visible.
         */
        public final Rect visibleInsets = new Rect();
        
        /**
         * Option for {@link #setTouchableInsets(int)}: the entire window frame
         * can be touched.
         */
        public static final int TOUCHABLE_INSETS_FRAME = 0;
        
        /**
         * Option for {@link #setTouchableInsets(int)}: the area inside of
         * the content insets can be touched.
         */
        public static final int TOUCHABLE_INSETS_CONTENT = 1;
        
        /**
         * Option for {@link #setTouchableInsets(int)}: the area inside of
         * the visible insets can be touched.
         */
        public static final int TOUCHABLE_INSETS_VISIBLE = 2;
        
        /**
         * Set which parts of the window can be touched: either
         * {@link #TOUCHABLE_INSETS_FRAME}, {@link #TOUCHABLE_INSETS_CONTENT},
         * or {@link #TOUCHABLE_INSETS_VISIBLE}. 
         */
        public void setTouchableInsets(int val) {
            mTouchableInsets = val;
        }
        
        public int getTouchableInsets() {
            return mTouchableInsets;
        }
        
        int mTouchableInsets;
        
        void reset() {
            final Rect givenContent = contentInsets;
            final Rect givenVisible = visibleInsets;
            givenContent.left = givenContent.top = givenContent.right
                    = givenContent.bottom = givenVisible.left = givenVisible.top
                    = givenVisible.right = givenVisible.bottom = 0;
            mTouchableInsets = TOUCHABLE_INSETS_FRAME;
        }
        
        @Override public boolean equals(Object o) {
            try {
                if (o == null) {
                    return false;
                }
                InternalInsetsInfo other = (InternalInsetsInfo)o;
                if (!contentInsets.equals(other.contentInsets)) {
                    return false;
                }
                if (!visibleInsets.equals(other.visibleInsets)) {
                    return false;
                }
                return mTouchableInsets == other.mTouchableInsets;
            } catch (ClassCastException e) {
                return false;
            }
        }
        
        void set(InternalInsetsInfo other) {
            contentInsets.set(other.contentInsets);
            visibleInsets.set(other.visibleInsets);
            mTouchableInsets = other.mTouchableInsets;
        }
    }
    
    /**
     * Interface definition for a callback to be invoked when layout has
     * completed and the client can compute its interior insets.
     * {@hide pending API Council approval}
     */
    public interface OnComputeInternalInsetsListener {
        /**
         * Callback method to be invoked when layout has completed and the
         * client can compute its interior insets.
         *
         * @param inoutInfo Should be filled in by the implementation with
         * the information about the insets of the window.  This is called
         * with whatever values the previous OnComputeInternalInsetsListener
         * returned, if there are multiple such listeners in the window.
         */
        public void onComputeInternalInsets(InternalInsetsInfo inoutInfo);
    }

    /**
     * Creates a new ViewTreeObserver. This constructor should not be called
     */
    ViewTreeObserver() {
    }

    /**
     * Merges all the listeners registered on the specified observer with the listeners
     * registered on this object. After this method is invoked, the specified observer
     * will return false in {@link #isAlive()} and should not be used anymore.
     *
     * @param observer The ViewTreeObserver whose listeners must be added to this observer
     */
    void merge(ViewTreeObserver observer) {
        if (observer.mOnGlobalFocusListeners != null) {
            if (mOnGlobalFocusListeners != null) {
                mOnGlobalFocusListeners.addAll(observer.mOnGlobalFocusListeners);
            } else {
                mOnGlobalFocusListeners = observer.mOnGlobalFocusListeners;
            }
        }

        if (observer.mOnGlobalLayoutListeners != null) {
            if (mOnGlobalLayoutListeners != null) {
                mOnGlobalLayoutListeners.addAll(observer.mOnGlobalLayoutListeners);
            } else {
                mOnGlobalLayoutListeners = observer.mOnGlobalLayoutListeners;
            }
        }

        if (observer.mOnPreDrawListeners != null) {
            if (mOnPreDrawListeners != null) {
                mOnPreDrawListeners.addAll(observer.mOnPreDrawListeners);
            } else {
                mOnPreDrawListeners = observer.mOnPreDrawListeners;
            }
        }

        if (observer.mOnTouchModeChangeListeners != null) {
            if (mOnTouchModeChangeListeners != null) {
                mOnTouchModeChangeListeners.addAll(observer.mOnTouchModeChangeListeners);
            } else {
                mOnTouchModeChangeListeners = observer.mOnTouchModeChangeListeners;
            }
        }

        if (observer.mOnComputeInternalInsetsListeners != null) {
            if (mOnComputeInternalInsetsListeners != null) {
                mOnComputeInternalInsetsListeners.addAll(observer.mOnComputeInternalInsetsListeners);
            } else {
                mOnComputeInternalInsetsListeners = observer.mOnComputeInternalInsetsListeners;
            }
        }

        observer.kill();
    }

    /**
     * Register a callback to be invoked when the focus state within the view tree changes.
     *
     * @param listener The callback to add
     *
     * @throws IllegalStateException If {@link #isAlive()} returns false
     */
    public void addOnGlobalFocusChangeListener(OnGlobalFocusChangeListener listener) {
        checkIsAlive();

        if (mOnGlobalFocusListeners == null) {
            mOnGlobalFocusListeners = new ArrayList<OnGlobalFocusChangeListener>();
        }

        mOnGlobalFocusListeners.add(listener);
    }

    /**
     * Remove a previously installed focus change callback.
     *
     * @param victim The callback to remove
     *
     * @throws IllegalStateException If {@link #isAlive()} returns false
     *
     * @see #addOnGlobalFocusChangeListener(OnGlobalFocusChangeListener)
     */
    public void removeOnGlobalFocusChangeListener(OnGlobalFocusChangeListener victim) {
        checkIsAlive();
        if (mOnGlobalFocusListeners == null) {
            return;
        }
        mOnGlobalFocusListeners.remove(victim);
    }

    /**
     * Register a callback to be invoked when the global layout state or the visibility of views
     * within the view tree changes
     *
     * @param listener The callback to add
     *
     * @throws IllegalStateException If {@link #isAlive()} returns false
     */
    public void addOnGlobalLayoutListener(OnGlobalLayoutListener listener) {
        checkIsAlive();

        if (mOnGlobalLayoutListeners == null) {
            mOnGlobalLayoutListeners = new ArrayList<OnGlobalLayoutListener>();
        }

        mOnGlobalLayoutListeners.add(listener);
    }

    /**
     * Remove a previously installed global layout callback
     *
     * @param victim The callback to remove
     *
     * @throws IllegalStateException If {@link #isAlive()} returns false
     *
     * @see #addOnGlobalLayoutListener(OnGlobalLayoutListener)
     */
    public void removeGlobalOnLayoutListener(OnGlobalLayoutListener victim) {
        checkIsAlive();
        if (mOnGlobalLayoutListeners == null) {
            return;
        }
        mOnGlobalLayoutListeners.remove(victim);
    }

    /**
     * Register a callback to be invoked when the view tree is about to be drawn
     *
     * @param listener The callback to add
     *
     * @throws IllegalStateException If {@link #isAlive()} returns false
     */
    public void addOnPreDrawListener(OnPreDrawListener listener) {
        checkIsAlive();

        if (mOnPreDrawListeners == null) {
            mOnPreDrawListeners = new ArrayList<OnPreDrawListener>();
        }

        mOnPreDrawListeners.add(listener);
    }

    /**
     * Remove a previously installed pre-draw callback
     *
     * @param victim The callback to remove
     *
     * @throws IllegalStateException If {@link #isAlive()} returns false
     *
     * @see #addOnPreDrawListener(OnPreDrawListener)
     */
    public void removeOnPreDrawListener(OnPreDrawListener victim) {
        checkIsAlive();
        if (mOnPreDrawListeners == null) {
            return;
        }
        mOnPreDrawListeners.remove(victim);
    }

    /**
     * Register a callback to be invoked when the invoked when the touch mode changes.
     *
     * @param listener The callback to add
     *
     * @throws IllegalStateException If {@link #isAlive()} returns false
     */
    public void addOnTouchModeChangeListener(OnTouchModeChangeListener listener) {
        checkIsAlive();

        if (mOnTouchModeChangeListeners == null) {
            mOnTouchModeChangeListeners = new ArrayList<OnTouchModeChangeListener>();
        }

        mOnTouchModeChangeListeners.add(listener);
    }

    /**
     * Remove a previously installed touch mode change callback
     *
     * @param victim The callback to remove
     *
     * @throws IllegalStateException If {@link #isAlive()} returns false
     *
     * @see #addOnTouchModeChangeListener(OnTouchModeChangeListener)
     */
    public void removeOnTouchModeChangeListener(OnTouchModeChangeListener victim) {
        checkIsAlive();
        if (mOnTouchModeChangeListeners == null) {
            return;
        }
        mOnTouchModeChangeListeners.remove(victim);
    }

    /**
     * Register a callback to be invoked when the invoked when it is time to
     * compute the window's internal insets.
     *
     * @param listener The callback to add
     *
     * @throws IllegalStateException If {@link #isAlive()} returns false
     * {@hide pending API Council approval}
     */
    public void addOnComputeInternalInsetsListener(OnComputeInternalInsetsListener listener) {
        checkIsAlive();

        if (mOnComputeInternalInsetsListeners == null) {
            mOnComputeInternalInsetsListeners = new ArrayList<OnComputeInternalInsetsListener>();
        }

        mOnComputeInternalInsetsListeners.add(listener);
    }

    /**
     * Remove a previously installed internal insets computation callback
     *
     * @param victim The callback to remove
     *
     * @throws IllegalStateException If {@link #isAlive()} returns false
     *
     * @see #addOnComputeInternalInsetsListener(OnComputeInternalInsetsListener)
     * {@hide pending API Council approval}
     */
    public void removeOnComputeInternalInsetsListener(OnComputeInternalInsetsListener victim) {
        checkIsAlive();
        if (mOnComputeInternalInsetsListeners == null) {
            return;
        }
        mOnComputeInternalInsetsListeners.remove(victim);
    }

    private void checkIsAlive() {
        if (!mAlive) {
            throw new IllegalStateException("This ViewTreeObserver is not alive, call "
                    + "getViewTreeObserver() again");
        }
    }

    /**
     * Indicates whether this ViewTreeObserver is alive. When an observer is not alive,
     * any call to a method (except this one) will throw an exception.
     *
     * If an application keeps a long-lived reference to this ViewTreeObserver, it should
     * always check for the result of this method before calling any other method.
     *
     * @return True if this object is alive and be used, false otherwise.
     */
    public boolean isAlive() {
        return mAlive;
    }

    /**
     * Marks this ViewTreeObserver as not alive. After invoking this method, invoking
     * any other method but {@link #isAlive()} and {@link #kill()} will throw an Exception.
     *
     * @hide
     */
    private void kill() {
        mAlive = false;
    }

    /**
     * Notifies registered listeners that focus has changed.
     */
    final void dispatchOnGlobalFocusChange(View oldFocus, View newFocus) {
        final ArrayList<OnGlobalFocusChangeListener> globaFocusListeners = mOnGlobalFocusListeners;
        if (globaFocusListeners != null) {
            final int count = globaFocusListeners.size();
            for (int i = count - 1; i >= 0; i--) {
                globaFocusListeners.get(i).onGlobalFocusChanged(oldFocus, newFocus);
            }
        }
    }

    /**
     * Notifies registered listeners that a global layout happened. This can be called
     * manually if you are forcing a layout on a View or a hierarchy of Views that are
     * not attached to a Window or in the GONE state.
     */
    public final void dispatchOnGlobalLayout() {
        final ArrayList<OnGlobalLayoutListener> globaLayoutListeners = mOnGlobalLayoutListeners;
        if (globaLayoutListeners != null) {
            final int count = globaLayoutListeners.size();
            for (int i = count - 1; i >= 0; i--) {
                globaLayoutListeners.get(i).onGlobalLayout();
            }
        }
    }

    /**
     * Notifies registered listeners that the drawing pass is about to start. If a
     * listener returns true, then the drawing pass is canceled and rescheduled. This can
     * be called manually if you are forcing the drawing on a View or a hierarchy of Views
     * that are not attached to a Window or in the GONE state.
     *
     * @return True if the current draw should be canceled and resceduled, false otherwise.
     */
    public final boolean dispatchOnPreDraw() {
        boolean cancelDraw = false;
        final ArrayList<OnPreDrawListener> preDrawListeners = mOnPreDrawListeners;
        if (preDrawListeners != null) {
            final int count = preDrawListeners.size();
            for (int i = count - 1; i >= 0; i--) {
                cancelDraw |= !preDrawListeners.get(i).onPreDraw();
            }
        }
        return cancelDraw;
    }

    /**
     * Notifies registered listeners that the touch mode has changed.
     *
     * @param inTouchMode True if the touch mode is now enabled, false otherwise.
     */
    final void dispatchOnTouchModeChanged(boolean inTouchMode) {
        final ArrayList<OnTouchModeChangeListener> touchModeListeners = mOnTouchModeChangeListeners;
        if (touchModeListeners != null) {
            final int count = touchModeListeners.size();
            for (int i = count - 1; i >= 0; i--) {
                touchModeListeners.get(i).onTouchModeChanged(inTouchMode);
            }
        }
    }

    /**
     * Returns whether there are listeners for computing internal insets.
     */
    final boolean hasComputeInternalInsetsListeners() {
        final ArrayList<OnComputeInternalInsetsListener> listeners = mOnComputeInternalInsetsListeners;
        return (listeners != null && listeners.size() > 0);
    }
    
    /**
     * Calls all listeners to compute the current insets.
     */
    final void dispatchOnComputeInternalInsets(InternalInsetsInfo inoutInfo) {
        final ArrayList<OnComputeInternalInsetsListener> listeners = mOnComputeInternalInsetsListeners;
        if (listeners != null) {
            final int count = listeners.size();
            for (int i = count - 1; i >= 0; i--) {
                listeners.get(i).onComputeInternalInsets(inoutInfo);
            }
        }
    }
}
