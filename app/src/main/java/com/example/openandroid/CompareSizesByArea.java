package com.example.openandroid;

import android.util.Size;

import java.util.Comparator;

public class CompareSizesByArea implements Comparator<Size> {
    /**
     * Compares two {@code Size}s based on their areas.
     */
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

}
