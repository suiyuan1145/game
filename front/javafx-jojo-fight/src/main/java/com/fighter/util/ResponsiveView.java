package com.fighter.util;

import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

/** Keeps a fixed logical game canvas centered and uniformly scaled inside a resizable window. */
public final class ResponsiveView {
    private ResponsiveView() {}

    public static StackPane wrap(Node content, double logicalWidth, double logicalHeight) {
        StackPane root = new StackPane(content);
        root.setStyle("-fx-background-color: #02040c;");
        root.setMinSize(320, 200);
        root.setPrefSize(logicalWidth, logicalHeight);
        var scale = Bindings.min(root.widthProperty().divide(logicalWidth),
                root.heightProperty().divide(logicalHeight));
        content.scaleXProperty().bind(scale);
        content.scaleYProperty().bind(scale);
        return root;
    }
}
