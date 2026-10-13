package com.sarbondhu.app.ui.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

public class FertilizerFormViewModelTest {

    @Test
    public void splitCropsTrimsAndDropsEmptyParts() {
        assertEquals(Arrays.asList("ধান", "গম", "ভুট্টা"),
                FertilizerFormViewModel.splitCrops(" ধান, গম ,, ভুট্টা ,"));
    }

    @Test
    public void splitCropsOfBlankIsEmpty() {
        assertTrue(FertilizerFormViewModel.splitCrops("   ").isEmpty());
    }
}
