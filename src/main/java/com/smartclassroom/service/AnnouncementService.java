package com.smartclassroom.service;

import com.smartclassroom.dao.AnnouncementDao;
import com.smartclassroom.model.Announcement;
import com.smartclassroom.model.Role;
import com.smartclassroom.model.User;
import com.smartclassroom.service.exception.AccessDeniedException;
import com.smartclassroom.service.exception.ValidationException;
import com.smartclassroom.util.Validator;

import java.util.List;

/** Announcements: admin and faculty post, everyone reads what is meant for their role. */
public class AnnouncementService {
    private final AnnouncementDao announcementDao;

    public AnnouncementService() {
        this(new AnnouncementDao());
    }

    public AnnouncementService(AnnouncementDao announcementDao) {
        this.announcementDao = announcementDao;
    }

    public List<Announcement> getVisibleTo(User user) {
        return announcementDao.findVisibleTo(user.getRole());
    }

    /** The newest few announcements, for the dashboard. */
    public List<Announcement> getRecent(User user, int limit) {
        List<Announcement> all = getVisibleTo(user);
        return all.size() <= limit ? all : all.subList(0, limit);
    }

    public void post(User actor, String title, String message, Announcement.Audience audience) {
        AccessControl.requireRole(actor, Role.ADMIN, Role.FACULTY);
        Announcement a = new Announcement();
        a.setTitle(Validator.requireText(title, "Title", 150));
        a.setMessage(Validator.requireText(message, "Message", 2000));
        a.setAudience(Validator.requireNotNull(audience, "an audience"));
        a.setPostedBy(actor.getUserId());
        announcementDao.insert(a);
    }

    /** Admin may delete any announcement; faculty only their own. */
    public void delete(User actor, Announcement announcement) {
        AccessControl.requireRole(actor, Role.ADMIN, Role.FACULTY);
        if (announcement == null) {
            throw new ValidationException("Select an announcement to delete.");
        }
        if (actor.getRole() != Role.ADMIN && announcement.getPostedBy() != actor.getUserId()) {
            throw new AccessDeniedException("You can only delete announcements that you posted.");
        }
        announcementDao.delete(announcement.getAnnouncementId());
    }
}
