package com.example.openandroid.view.focus;

/**
 * Primary interface for interacting with the focus UI.
 */
public interface IFocusView {

    /**
     * State of focus UI.
     */
    enum FocusViewState {
        /**
         * Indicates focus view is inactive(not shown).
         */
        STATE_IDLE,
        /**
         * Indicates active focusing view in progress.
         */
        STATE_ACTIVE_FOCUSING,
        /**
         * Indicates active focused view in progress.
         */
        STATE_ACTIVE_FOCUSED,
        /**
         * Indicates passive focus view in progress.
         */
        STATE_PASSIVE_FOCUSING
    }

    /**
     * Check the state of the passive focus animation.
     *
     * @return whether the passive focus animation is running.
     */
    boolean isPassiveFocusRunning();

    /**
     * Check the state of the active focus animation.
     *
     * @return whether the active focus animation is running.
     */
    boolean isActiveFocusRunning();

    /**
     * Start a passive focus animation.
     */
    void startPassiveFocus();

    /**
     * Start an active focus animation.
     */
    void startActiveFocus();

    /**
     * Stop any currently running focus animations.
     */
    void stopFocusAnimations();

    /**
     * Set the location of the focus center.
     */
    void setFocusLocation(float viewX, float viewY);

    /**
     * Set the location of the focus center.
     */
    void centerFocusLocation();
}

