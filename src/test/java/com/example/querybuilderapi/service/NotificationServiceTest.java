package com.example.querybuilderapi.service;

import com.example.querybuilderapi.model.Notification;
import com.example.querybuilderapi.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createNotification_ShouldSaveAndReturn() {
        String title = "Test Title";
        String message = "Test Message";
        String type = "info";

        Notification mockNotification = new Notification();
        mockNotification.setTitle(title);
        mockNotification.setMessage(message);
        mockNotification.setType(type);
        mockNotification.setIsRead(false);

        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

        Notification result = notificationService.createNotification(title, message, type);

        assertNotNull(result);
        assertEquals(title, result.getTitle());
        assertEquals(message, result.getMessage());
        assertEquals(type, result.getType());
        assertFalse(result.getIsRead());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void getAllNotifications_ShouldReturnList() {
        List<Notification> expected = Arrays.asList(new Notification(), new Notification());
        when(notificationRepository.findAllByOrderByTimestampDesc()).thenReturn(expected);

        List<Notification> result = notificationService.getAllNotifications();

        assertEquals(2, result.size());
        verify(notificationRepository, times(1)).findAllByOrderByTimestampDesc();
    }

    @Test
    void markAsRead_ExistingId_ShouldUpdate() {
        Long id = 1L;
        Notification notification = new Notification();
        notification.setId(id);
        notification.setIsRead(false);

        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(id);

        assertTrue(notification.getIsRead());
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    void markAllAsRead_ShouldUpdateUnreadOnly() {
        Notification readNotif = new Notification();
        readNotif.setIsRead(true);

        Notification unreadNotif1 = new Notification();
        unreadNotif1.setIsRead(false);

        Notification unreadNotif2 = new Notification();
        unreadNotif2.setIsRead(false);

        when(notificationRepository.findAll()).thenReturn(Arrays.asList(readNotif, unreadNotif1, unreadNotif2));

        notificationService.markAllAsRead();

        assertTrue(unreadNotif1.getIsRead());
        assertTrue(unreadNotif2.getIsRead());
        verify(notificationRepository, times(1)).saveAll(anyList());
    }

    @Test
    void deleteNotification_ShouldCallRepository() {
        Long id = 1L;
        notificationService.deleteNotification(id);
        verify(notificationRepository, times(1)).deleteById(id);
    }

    @Test
    void deleteAllNotifications_ShouldCallRepository() {
        notificationService.deleteAllNotifications();
        verify(notificationRepository, times(1)).deleteAll();
    }
}
