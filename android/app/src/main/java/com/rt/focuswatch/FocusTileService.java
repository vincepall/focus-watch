package com.rt.focuswatch;

import androidx.wear.tiles.ActionBuilders;
import androidx.wear.tiles.ColorBuilders;
import androidx.wear.tiles.DimensionBuilders;
import androidx.wear.tiles.LayoutElementBuilders;
import androidx.wear.tiles.ModifiersBuilders;
import androidx.wear.tiles.RequestBuilders;
import androidx.wear.tiles.ResourceBuilders;
import androidx.wear.tiles.TileBuilders;
import androidx.wear.tiles.TileService;
import androidx.wear.tiles.TimelineBuilders;
import com.google.common.util.concurrent.ListenableFuture;

public class FocusTileService extends TileService {

    private static final String RESOURCES_VERSION = "1";

    @Override
    protected ListenableFuture<TileBuilders.Tile> onTileRequest(RequestBuilders.TileRequest requestParams) {
        // Action to open Focusverandering app
        ActionBuilders.LaunchAction launchAction = new ActionBuilders.LaunchAction.Builder()
                .setAndroidActivity(new ActionBuilders.AndroidActivity.Builder()
                        .setPackageName("com.rt.focuswatch")
                        .setClassName("com.rt.focuswatch.MainActivity")
                        .build())
                .build();

        ModifiersBuilders.Clickable openAppClickable = new ModifiersBuilders.Clickable.Builder()
                .setId("open_focus_app")
                .setOnClick(launchAction)
                .build();

        // 1. Header Title
        LayoutElementBuilders.Text titleText = new LayoutElementBuilders.Text.Builder()
                .setText("FOCUSVERANDERING")
                .setFontStyle(new LayoutElementBuilders.FontStyle.Builder()
                        .setSize(DimensionBuilders.sp(12))
                        .setColor(ColorBuilders.argb(0xFFF5DF4D)) // Accent Yellow
                        .setWeight(700)
                        .build())
                .build();

        // 2. Subtitle
        LayoutElementBuilders.Text subText = new LayoutElementBuilders.Text.Builder()
                .setText("Focus & Dikte Calculator")
                .setFontStyle(new LayoutElementBuilders.FontStyle.Builder()
                        .setSize(DimensionBuilders.sp(11))
                        .setColor(ColorBuilders.argb(0xFFAAAAAA)) // Muted Grey
                        .build())
                .build();

        // 3. Action Button (Pill shaped)
        LayoutElementBuilders.Text btnText = new LayoutElementBuilders.Text.Builder()
                .setText("▶ OPEN CALCULATOR")
                .setFontStyle(new LayoutElementBuilders.FontStyle.Builder()
                        .setSize(DimensionBuilders.sp(12))
                        .setColor(ColorBuilders.argb(0xFF000000))
                        .setWeight(700)
                        .build())
                .build();

        LayoutElementBuilders.Box actionButton = new LayoutElementBuilders.Box.Builder()
                .setWidth(DimensionBuilders.dp(152))
                .setHeight(DimensionBuilders.dp(40))
                .setModifiers(new ModifiersBuilders.Modifiers.Builder()
                        .setBackground(new ModifiersBuilders.Background.Builder()
                                .setColor(ColorBuilders.argb(0xFFF5DF4D))
                                .setCorner(new ModifiersBuilders.Corner.Builder()
                                        .setRadius(DimensionBuilders.dp(20))
                                        .build())
                                .build())
                        .setClickable(openAppClickable)
                        .build())
                .addContent(btnText)
                .build();

        // 4. Bottom Hint
        LayoutElementBuilders.Text hintText = new LayoutElementBuilders.Text.Builder()
                .setText("Tik om te openen")
                .setFontStyle(new LayoutElementBuilders.FontStyle.Builder()
                        .setSize(DimensionBuilders.sp(9))
                        .setColor(ColorBuilders.argb(0xFF666666))
                        .build())
                .build();

        // Layout Column
        LayoutElementBuilders.Column column = new LayoutElementBuilders.Column.Builder()
                .setWidth(DimensionBuilders.expand())
                .setHeight(DimensionBuilders.expand())
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .addContent(new LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(16)).build())
                .addContent(titleText)
                .addContent(new LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(4)).build())
                .addContent(subText)
                .addContent(new LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(14)).build())
                .addContent(actionButton)
                .addContent(new LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(8)).build())
                .addContent(hintText)
                .build();

        // Root Box - Clickable everywhere
        LayoutElementBuilders.Box rootBox = new LayoutElementBuilders.Box.Builder()
                .setWidth(DimensionBuilders.expand())
                .setHeight(DimensionBuilders.expand())
                .setModifiers(new ModifiersBuilders.Modifiers.Builder()
                        .setClickable(openAppClickable)
                        .setBackground(new ModifiersBuilders.Background.Builder()
                                .setColor(ColorBuilders.argb(0xFF000000))
                                .build())
                        .build())
                .addContent(column)
                .build();

        TimelineBuilders.TimelineEntry timelineEntry = new TimelineBuilders.TimelineEntry.Builder()
                .setLayout(new LayoutElementBuilders.Layout.Builder()
                        .setRoot(rootBox)
                        .build())
                .build();

        TimelineBuilders.Timeline timeline = new TimelineBuilders.Timeline.Builder()
                .addTimelineEntry(timelineEntry)
                .build();

        TileBuilders.Tile tile = new TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setTimeline(timeline)
                .build();

        return new ImmediateFuture<>(tile);
    }

    @Override
    protected ListenableFuture<ResourceBuilders.Resources> onResourcesRequest(RequestBuilders.ResourcesRequest requestParams) {
        return new ImmediateFuture<>(new ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .build());
    }
}
