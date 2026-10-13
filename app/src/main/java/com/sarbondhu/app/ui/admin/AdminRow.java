package com.sarbondhu.app.ui.admin;

import androidx.annotation.ColorRes;

/** One display row in an admin list. badgeText may be null for no badge. */
public class AdminRow {

    final int id;
    final String title;
    final String subtitle;
    final String badgeText;
    @ColorRes
    final int badgeColor;

    public AdminRow(int id, String title, String subtitle, String badgeText, @ColorRes int badgeColor) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.badgeText = badgeText;
        this.badgeColor = badgeColor;
    }
}
