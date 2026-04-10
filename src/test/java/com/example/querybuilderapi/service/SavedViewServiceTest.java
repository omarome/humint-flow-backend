package com.example.querybuilderapi.service;

import com.example.querybuilderapi.model.SavedView;
import com.example.querybuilderapi.repository.SavedViewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SavedViewServiceTest {

    @Mock
    private SavedViewRepository savedViewRepository;

    @InjectMocks
    private SavedViewService savedViewService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void saveView_ValidInput_ShouldSave() {
        String name = "Test View";
        String queryJson = "{\"combinator\":\"and\",\"rules\":[{\"field\":\"age\",\"operator\":\">\",\"value\":25}]}";
        SavedView expectedView = SavedView.builder().name(name).queryJson(queryJson).build();

        when(savedViewRepository.save(any(SavedView.class))).thenReturn(expectedView);

        SavedView result = savedViewService.saveView(name, queryJson, "TEAM_MEMBER");

        assertNotNull(result);
        assertEquals(name, result.getName());
        verify(savedViewRepository, times(1)).save(any(SavedView.class));
    }

    @Test
    void saveView_EmptyName_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            savedViewService.saveView("", "{}", null)
        );
    }

    @Test
    void saveView_LongName_ShouldThrowException() {
        String longName = "a".repeat(101);
        assertThrows(IllegalArgumentException.class, () ->
            savedViewService.saveView(longName, "{}", null)
        );
    }

    @Test
    void saveView_DangerousChars_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            savedViewService.saveView("My View; DROP TABLE users", "{}", null)
        );
        assertThrows(IllegalArgumentException.class, () ->
            savedViewService.saveView("My View' OR 1=1", "{}", null)
        );
    }

    @Test
    void saveView_SqlKeywords_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            savedViewService.saveView("SELECT everything", "{\"rules\":[{\"f\":\"v\"}]}", null)
        );
    }

    @Test
    void saveView_EmptyRules_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            savedViewService.saveView("Empty Rules", "{\"rules\":[]}", null)
        );
        assertThrows(IllegalArgumentException.class, () ->
            savedViewService.saveView("Null Query", null, null)
        );
    }

    @Test
    void deleteView_ExistingId_ShouldDelete() {
        Long id = 1L;
        when(savedViewRepository.existsById(id)).thenReturn(true);

        assertDoesNotThrow(() -> savedViewService.deleteView(id));
        verify(savedViewRepository, times(1)).deleteById(id);
    }

    @Test
    void deleteView_NonExistingId_ShouldThrowException() {
        Long id = 999L;
        when(savedViewRepository.existsById(id)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
            savedViewService.deleteView(id)
        );
        verify(savedViewRepository, never()).deleteById(anyLong());
    }
}
