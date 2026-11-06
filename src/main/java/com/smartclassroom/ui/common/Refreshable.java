package com.smartclassroom.ui.common;

/** Implemented by every screen that loads data. The dashboard calls refresh() each time the screen is shown. */
public interface Refreshable {
    void refresh();
}
