package com.mcon152.recipeshare.service;

import com.mcon152.recipeshare.Recipe;
import com.mcon152.recipeshare.repository.RecipeRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Assignment: Implement all TODOs using Mockito features covered in class:
 *  - @Mock, @InjectMocks, @Captor, @ExtendWith(MockitoExtension.class)
 *  - Stubbing: thenReturn / thenAnswer / thenThrow
 *  - Verifications: verify(...), times/never/atLeast..., verifyNoMoreInteractions
 *  - InOrder (where meaningful)
 *  - Void stubbing: doNothing / doThrow (use deleteById for this)
 *  - Matchers: any(), eq(), argThat()
 *  - ArgumentCaptor
 *  - (Optional) Spy demo if you introduce a small helper in tests
 *
 * NOTE: This is a pure unit test. Do NOT start a Spring context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecipeService (Mockito) — Assignment Skeleton")
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @InjectMocks
    private RecipeServiceImpl recipeService; // CUT implements RecipeService

    @Captor
    private ArgumentCaptor<Recipe> recipeCaptor;

    // --- Helpers for sample data ---

    private Recipe newRecipeNoId() {
        return new Recipe(
                null,
                "Chocolate Cake",
                "Moist chocolate cake",
                "flour, eggs, cocoa",
                "mix, bake",
                8
        );
    }

    private Recipe savedRecipe(long id) {
        return new Recipe(
                id,
                "Chocolate Cake",
                "Moist chocolate cake",
                "flour, eggs, cocoa",
                "mix, bake",
                8
        );
    }

    // ------------------ addRecipe ------------------

    @Nested
    @DisplayName("addRecipe(Recipe)")
    class AddRecipe {

        @Test
        @DisplayName("returns saved entity (thenReturn) and calls repository.save once")
        void returnsSaved_andSavesOnce() {
            Recipe input = newRecipeNoId();
            Recipe saved = savedRecipe(1L);

            when(recipeRepository.save(any(Recipe.class))).thenReturn(saved);

            Recipe out = recipeService.addRecipe(input);
            assertEquals(1L, out.getId());
            assertEquals(saved, out);

            verify(recipeRepository).save(any(Recipe.class));
            verifyNoMoreInteractions(recipeRepository);
        }

        @Test
        @DisplayName("assigns ID dynamically (thenAnswer) and captures argument")
        void assignsId_thenAnswer_andCaptures() {
            when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> {
                Recipe r = inv.getArgument(0);
                return new Recipe(1L, r.getTitle(), r.getDescription(),
                        r.getIngredients(), r.getInstructions(), r.getServings());
            });

            Recipe out = recipeService.addRecipe(newRecipeNoId());
            assertEquals(1L, out.getId());

            verify(recipeRepository).save(recipeCaptor.capture());
            Recipe sent = recipeCaptor.getValue();
            assertNull(sent.getId()); // before persistence
            assertEquals("Chocolate Cake", sent.getTitle());
        }

        @Test
        @DisplayName("propagates repository failure (thenThrow)")
        void propagatesRepositoryFailure() {
            when(recipeRepository.save(any())).thenThrow(new IllegalStateException("DB down"));
            assertThrows(IllegalStateException.class, () -> recipeService.addRecipe(newRecipeNoId()));
        }
    }

    // ------------------ getAllRecipes ------------------

    @Nested
    @DisplayName("getAllRecipes()")
    class GetAllRecipes {

        @Test
        @DisplayName("returns list from repository")
        void returnsList() {
            List<Recipe> mockRecipes = List.of(
                    savedRecipe(1L),
                    savedRecipe(2L)
            );
            when(recipeRepository.findAll()).thenReturn(mockRecipes);

            List<Recipe> result = recipeService.getAllRecipes();

            assertEquals(2, result.size());
            assertEquals(mockRecipes, result);

            verify(recipeRepository).findAll();
            verifyNoMoreInteractions(recipeRepository);
        }
    }

    // ------------------ getRecipeById ------------------

    @Nested
    @DisplayName("getRecipeById(long)")
    class GetById {

        @Test
        @DisplayName("returns Optional.present when found")
        void present() {
            Recipe saved = savedRecipe(1L);
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(saved));

            Optional<Recipe> result = recipeService.getRecipeById(1L);

            assertTrue(result.isPresent());
            assertEquals(saved, result.get());

            verify(recipeRepository).findById(1L);
            verifyNoMoreInteractions(recipeRepository);
        }
    }

    // ------------------ deleteRecipe ------------------

    @Nested
    @DisplayName("deleteRecipe(long)")
    class DeleteRecipe {

        @Test
        @DisplayName("returns true when entity existed")
        void returnsTrue_whenExists() {
            long id = 1L;
            when(recipeRepository.existsById(id)).thenReturn(true);
            doNothing().when(recipeRepository).deleteById(id);

            boolean result = recipeService.deleteRecipe(id);

            assertTrue(result);

            InOrder inOrder = inOrder(recipeRepository);
            inOrder.verify(recipeRepository).existsById(id);
            inOrder.verify(recipeRepository).deleteById(id);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("returns false when missing (never deletes)")
        void returnsFalse_whenMissing() {
            long id = 2L;
            when(recipeRepository.existsById(id)).thenReturn(false);

            boolean result = recipeService.deleteRecipe(id);

            assertFalse(result);

            verify(recipeRepository).existsById(id);
            verify(recipeRepository, never()).deleteById(anyLong());
            verifyNoMoreInteractions(recipeRepository);
        }

        @Test
        @DisplayName("propagates delete error (doThrow)")
        void propagatesDeleteError() {
            long id = 3L;
            when(recipeRepository.existsById(id)).thenReturn(true);
            doThrow(new RuntimeException("Delete failed"))
                    .when(recipeRepository).deleteById(id);

            assertThrows(RuntimeException.class, () -> recipeService.deleteRecipe(id));

            verify(recipeRepository).existsById(id);
            verify(recipeRepository).deleteById(id);
            verifyNoMoreInteractions(recipeRepository);
        }
    }

    // ------------------ updateRecipe ------------------

    @Nested
    @DisplayName("updateRecipe(long, Recipe)")
    class UpdateRecipe {

        @Test
        @DisplayName("returns updated entity when exists")
        void returnsUpdated_whenExists() {
            long id = 1L;
            Recipe existing = savedRecipe(id);
            Recipe updates = new Recipe(null, "Updated Cake", "Yummier cake", "flour, sugar", "bake", 10);
            Recipe updatedSaved = new Recipe(id, "Updated Cake", "Yummier cake", "flour, sugar", "bake", 10);

            when(recipeRepository.findById(id)).thenReturn(Optional.of(existing));
            when(recipeRepository.save(any(Recipe.class))).thenReturn(updatedSaved);

            Optional<Recipe> result = recipeService.updateRecipe(id, updates);

            assertTrue(result.isPresent());
            assertEquals("Updated Cake", result.get().getTitle());

            verify(recipeRepository).findById(id);
            verify(recipeRepository).save(recipeCaptor.capture());
            Recipe captured = recipeCaptor.getValue();
            assertEquals("Updated Cake", captured.getTitle());
            verifyNoMoreInteractions(recipeRepository);
        }

        @Test
        @DisplayName("returns empty when entity missing")
        void returnsEmpty_whenMissing() {
            long id = 2L;
            when(recipeRepository.findById(id)).thenReturn(Optional.empty());

            Optional<Recipe> result = recipeService.updateRecipe(id, newRecipeNoId());

            assertTrue(result.isEmpty());

            verify(recipeRepository).findById(id);
            verify(recipeRepository, never()).save(any());
            verifyNoMoreInteractions(recipeRepository);
        }
    }

    // ------------------ patchRecipe ------------------

    @Nested
    @DisplayName("patchRecipe(long, Recipe)")
    class PatchRecipe {

        @Test
        @DisplayName("applies only non-null fields (argThat)")
        void appliesNonNullFields_only() {
            long id = 1L;
            Recipe existing = savedRecipe(id);
            Recipe patch = new Recipe(null, "New Title", null, null, null, 0);

            when(recipeRepository.findById(id)).thenReturn(Optional.of(existing));
            when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

            Optional<Recipe> result = recipeService.patchRecipe(id, patch);

            assertTrue(result.isPresent());
            verify(recipeRepository).save(argThat(r ->
                    r.getTitle().equals("New Title") &&
                            r.getDescription().equals(existing.getDescription()) &&
                            r.getIngredients().equals(existing.getIngredients())
            ));
        }

        @Test
        @DisplayName("returns empty when entity missing")
        void returnsEmpty_whenMissing() {
            long id = 2L;
            when(recipeRepository.findById(id)).thenReturn(Optional.empty());

            Optional<Recipe> result = recipeService.patchRecipe(id, newRecipeNoId());

            assertTrue(result.isEmpty());

            verify(recipeRepository).findById(id);
            verify(recipeRepository, never()).save(any());
            verifyNoMoreInteractions(recipeRepository);
        }
    }

    // ------------------ extra practice ------------------

    @Nested
    @DisplayName("Advanced stubbing & verification")
    class Advanced {

        @Test
        @DisplayName("consecutive stubs on existsById (true, false)")
        void consecutiveStubs_existsById() {
            when(recipeRepository.existsById(1L)).thenReturn(true, false);

            assertTrue(recipeRepository.existsById(1L));
            assertFalse(recipeRepository.existsById(1L));

            verify(recipeRepository, times(2)).existsById(1L);
            verifyNoMoreInteractions(recipeRepository);
        }
    }
}
