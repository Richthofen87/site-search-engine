package ru.vladimirsazonov.SiteSearchEngine.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vladimirsazonov.SiteSearchEngine.services.morphology.MorphologyServiceImpl;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MorphologyServiceImplTest {
    String text = """
                Повторное появление леопарда в Осетии позволяет предположить,
                что леопард постоянно обитает в некоторых районах Северного
                Кавказа""";
    MorphologyServiceImpl morphologyServiceImpl = new MorphologyServiceImpl();

    @Test
    void getLemmasWordFormsTest_ifDoesNotThrowsException_returnValidSet() {
        Set<String> stringSet = new HashSet<>(Arrays.asList("леопардом", "леопарде", "леопардами", "леопардам",
                "леопарду", "леопардах", "леопардов", "леопарда", "леопарды", "леопард"));
        assertEquals(stringSet, morphologyServiceImpl.getLemmasWordForms("леопард"));
    }

    @Test
    void getLemmasSetTest_ifListOfLemmasIsNotEmpty_returnValidSet() {
        Set<String> stringSet = new HashSet<>(Arrays.asList("повторный", "появление", "леопард", "осетия",
                "позволять", "предположить", "постоянно", "обитать", "район", "северный", "кавказ"));
       assertEquals(stringSet, morphologyServiceImpl.getLemmasSet(text));
    }

    @Test
    void getLemmasSetTest_ifListOfLemmasIsEmpty_returnEmptySet() {
        assertEquals(Set.of(), morphologyServiceImpl.getLemmasSet(""));
    }

    @Test
    void getLemmasListTest_ifParamIsNotBlank_returnValidList() throws Exception {
        List<String> lemmas = Arrays.asList("повторный", "появление", "леопард", "осетия",
                "позволять", "предположить", "леопард", "постоянно", "обитать", "район", "северный", "кавказ");
        Method method = morphologyServiceImpl.getClass().getDeclaredMethod("getLemmasList", String.class);
        method.setAccessible(true);
        assertEquals(lemmas, method.invoke(morphologyServiceImpl, text));
    }

    @Test
    void getLemmasListTest_ifParamIsBlank_returnEmptyList() throws Exception {
        Method method = morphologyServiceImpl.getClass().getDeclaredMethod("getLemmasList", String.class);
        method.setAccessible(true);
        assertEquals(List.of(), method.invoke(morphologyServiceImpl, "   "));
    }
}