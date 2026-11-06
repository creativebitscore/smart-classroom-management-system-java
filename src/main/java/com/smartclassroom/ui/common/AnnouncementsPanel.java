package com.smartclassroom.ui.common;

import com.smartclassroom.model.Announcement;
import com.smartclassroom.model.Role;
import com.smartclassroom.model.User;
import com.smartclassroom.service.AnnouncementService;
import com.smartclassroom.ui.theme.RoundedPanel;
import com.smartclassroom.ui.theme.Theme;
import com.smartclassroom.ui.theme.UiFactory;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Announcements for every role. Admin and faculty can post; students and faculty can only read. */
public class AnnouncementsPanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final transient User user;
    private final transient AnnouncementService service = new AnnouncementService();
    private final AppTableModel model = new AppTableModel(new String[]{"Posted", "Title", "For", "Posted by"});
    private final JTable table = UiFactory.table(model);
    private final JTextArea detail = new JTextArea();
    private final transient List<Announcement> items = new ArrayList<>();

    public AnnouncementsPanel(User user) {
        super(new BorderLayout(0, 16));
        this.user = user;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);
        titles.add(UiFactory.title("Announcements"), BorderLayout.NORTH);
        titles.add(UiFactory.muted("Latest notices from the college"), BorderLayout.SOUTH);
        header.add(titles, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        if (user.getRole() != Role.STUDENT) {
            JButton post = UiFactory.primaryButton("+ New Announcement");
            post.addActionListener(e -> postNew());
            JButton delete = UiFactory.dangerButton("Delete");
            delete.addActionListener(e -> deleteSelected());
            buttons.add(post);
            buttons.add(delete);
        }
        JButton refresh = UiFactory.secondaryButton("Refresh");
        refresh.addActionListener(e -> refresh());
        buttons.add(refresh);
        header.add(buttons, BorderLayout.EAST);

        detail.setEditable(false);
        detail.setLineWrap(true);
        detail.setWrapStyleWord(true);
        detail.setFont(Theme.FONT_BASE);
        detail.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        detail.setText("Select an announcement to read it.");

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        card.add(UiFactory.scroll(table), BorderLayout.CENTER);
        var detailScroll = UiFactory.scroll(detail);
        detailScroll.setPreferredSize(new Dimension(10, 130));
        card.add(detailScroll, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);
        add(card, BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            Announcement selected = selected();
            if (selected != null) {
                detail.setText(selected.getTitle() + "\n\n" + selected.getMessage());
                detail.setCaretPosition(0);
            }
        });
    }

    private Announcement selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : items.get(table.convertRowIndexToModel(row));
    }

    private void postNew() {
        Window owner = javax.swing.SwingUtilities.getWindowAncestor(this);
        if (new AnnouncementDialog(owner).showDialog()) {
            UiFactory.success(this, "Announcement published.");
            refresh();
        }
    }

    private void deleteSelected() {
        Announcement selected = selected();
        if (selected == null) {
            UiFactory.warning(this, "Please select an announcement first.");
            return;
        }
        if (!UiFactory.confirm(this, "Delete the announcement \"" + selected.getTitle() + "\"?", "Confirm delete")) {
            return;
        }
        try {
            service.delete(user, selected);
            UiFactory.success(this, "The announcement was deleted.");
            refresh();
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }

    @Override
    public void refresh() {
        try {
            items.clear();
            items.addAll(service.getVisibleTo(user));
            model.setRowCount(0);
            for (Announcement a : items) {
                model.addRow(new Object[]{a.getCreatedAt().format(WHEN), a.getTitle(), a.getAudience(), a.getPostedByName()});
            }
            detail.setText(items.isEmpty() ? "There are no announcements yet." : "Select an announcement to read it.");
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }

    /** The "new announcement" form. */
    private final class AnnouncementDialog extends FormDialog {
        private static final long serialVersionUID = 1L;
        private final JTextField title = form.addField("Title", new JTextField(30));
        private final JComboBox<Announcement.Audience> audience =
                form.addField("Visible to", new JComboBox<>(Announcement.Audience.values()));
        private final JTextArea message = new JTextArea(6, 30);

        AnnouncementDialog(Window owner) {
            super(owner, "New announcement");
            message.setLineWrap(true);
            message.setWrapStyleWord(true);
            form.addField("Message", UiFactory.scroll(message));
        }

        @Override
        protected void save() {
            service.post(user, title.getText(), message.getText(), (Announcement.Audience) audience.getSelectedItem());
        }
    }
}
