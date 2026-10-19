package xyz.volcanobay.empiric.core.ui.widget;

import xyz.volcanobay.empiric.core.ui.ContextEntry;
import xyz.volcanobay.empiric.core.ui.Keys;
import xyz.volcanobay.empiric.core.ui.UiCanvas;
import xyz.volcanobay.empiric.core.ui.UiPoint;
import xyz.volcanobay.empiric.core.ui.Widget;
import xyz.volcanobay.empiric.core.ui.layout.Align;
import xyz.volcanobay.empiric.core.ui.layout.FlexLayout;
import xyz.volcanobay.empiric.core.ui.layout.Insets;

import java.util.List;

/**
 * A popup list of actions. Choosing one closes every popup and runs it. A line with a submenu opens it beside the line
 * when the pointer rests on it.
 */
public class ContextMenu extends Panel {

    private ContextMenu submenu;
    private Row submenuRow;

    public ContextMenu(List<ContextEntry> entries) {
        style("panel", "widget_border");
        padding(Insets.of(1));
        layout(FlexLayout.column(0).align(Align.STRETCH));
        for (ContextEntry entry : entries) {
            add(entry.separator() ? new Separator() : new Row(entry));
        }
    }

    private void openSubmenu(Row row) {
        if (submenuRow == row && submenu != null && submenu.root() != null) {
            return;
        }
        closeSubmenu();
        List<ContextEntry> entries = row.entry.submenu().get();
        if (entries.isEmpty()) {
            return;
        }
        submenu = new ContextMenu(entries);
        submenuRow = row;
        UiPoint corner = row.toRoot(row.width(), -1);
        root().openPopup(submenu, corner.x(), corner.y());
        if (submenu.x() < corner.x()) {
            UiPoint left = row.toRoot(0, -1);
            submenu.setBounds((int) Math.max(0, left.x() - submenu.width()), submenu.y(), submenu.width(), submenu.height());
        }
    }

    private void closeSubmenu() {
        if (submenu != null && submenu.root() != null) {
            submenu.root().closePopup(submenu);
        }
        submenu = null;
        submenuRow = null;
    }

    @Override
    protected void onDetach() {
        closeSubmenu();
    }

    private static final class Separator extends Widget {

        @Override
        protected int contentHeight() {
            return 3;
        }

        @Override
        protected void draw(UiCanvas canvas, float partialTick) {
            canvas.fill(2, 1, width() - 2, 2, color("widget_border"));
        }
    }

    private final class Row extends Widget {

        private final ContextEntry entry;

        Row(ContextEntry entry) {
            this.entry = entry;
            if (entry.hint() != null && !entry.hint().isEmpty()) {
                hint(entry.hint());
            }
        }

        private boolean usable() {
            return entry.enabled().getAsBoolean();
        }

        @Override
        protected boolean interactive() {
            return true;
        }

        @Override
        protected void hoverChanged(boolean hovered) {
            if (!hovered || root() == null) {
                return;
            }
            if (entry.isSubmenu() && usable()) {
                openSubmenu(this);
            } else {
                closeSubmenu();
            }
        }

        @Override
        protected boolean pointerDown(double x, double y, int button) {
            if (button != Keys.LEFT_BUTTON || !usable()) {
                return false;
            }
            if (entry.isSubmenu()) {
                openSubmenu(this);
                return true;
            }
            sound("click");
            root().closePopups();
            entry.action().run();
            return true;
        }

        @Override
        protected int contentWidth() {
            return font().width(entry.label()) + (entry.isSubmenu() ? 22 : 12);
        }

        @Override
        protected int contentHeight() {
            return font().lineHeight() + 4;
        }

        @Override
        protected void draw(UiCanvas canvas, float partialTick) {
            boolean open = submenuRow == this && submenu != null && submenu.root() != null;
            if ((isHovered() || open) && usable()) {
                canvas.fill(0, 0, width(), height(), color("widget_hover"));
            }
            int textColor = color(usable() ? "text" : "text_dim");
            canvas.text(entry.label(), 6, (height() - font().lineHeight()) / 2 + 1, textColor, false);
            if (entry.isSubmenu()) {
                float arrowX = width() - 8;
                float arrowY = height() / 2f;
                canvas.quad(arrowX, arrowY - 2.5f, arrowX, arrowY + 2.5f, arrowX + 3, arrowY, arrowX + 3, arrowY, textColor);
            }
        }
    }
}
