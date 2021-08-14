package org.screamingsandals.idea;

import com.google.gson.Gson;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.jvm.annotation.JvmAnnotationEnumFieldValue;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.intellij.patterns.PlatformPatterns.*;
import static com.intellij.patterns.PsiJavaPatterns.psiLiteral;
import static com.intellij.patterns.PsiJavaPatterns.psiMethod;

public class MinecraftTypesCompletionContributor extends CompletionContributor {
    private static final String ANNOTATION = "org.screamingsandals.lib.utils.annotations.ide.CustomAutocompletion";
    private static final String ARCTIC_DATA_URL = "https://raw.githubusercontent.com/Articdive/ArticData/master/Articdata/1.17.1/1_17_1_${category}.json";

    private final Map<String, List<LookupElement>> minecraftTypesCollections = new HashMap<>();
    private final Gson gson = new Gson();

    public MinecraftTypesCompletionContributor() {
        addNewMinecraftType("MATERIAL", "items");
        addNewMinecraftType("ENCHANTMENT", "enchantments");
        addNewMinecraftType("POTION_EFFECT", "potion_effects");
        addNewMinecraftType("POTION", "potions");
        addNewMinecraftType("EQUIPMENT_SLOT", List.of("main_hand", "off_hand", "boots", "leggings", "chestplate", "helmet"));
        addNewMinecraftType("FIREWORK_EFFECT", List.of("small", "large", "star", "burst", "creeper"));
        addNewMinecraftType("ENTITY_TYPE", "entities");
        //addNewMinecraftType("DAMAGE_CAUSE");
        addNewMinecraftType("ATTRIBUTE_TYPE", "attributes");
        addNewMinecraftType("GAME_MODE", List.of("survival", "creative", "adventure", "spectator"));

        var pattern = psiElement(JavaTokenType.STRING_LITERAL)
                .withParent(
                        psiLiteral().methodCallParameter(
                                psiMethod()
                                        .withAnnotation(ANNOTATION)
                        )
                );

        extend(CompletionType.BASIC, pattern, new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                var f = parameters.getOriginalPosition();
                if (f == null) {
                    return;
                }

                do {
                    f = f.getParent();
                    if (f == null) {
                        return;
                    }
                } while (!(f instanceof PsiMethodCallExpression));

                var method = ((PsiMethodCallExpression) f).resolveMethod();
                if (method == null) {
                    return;
                }
                var annotated = method.getAnnotation(ANNOTATION);
                if (annotated == null) {
                    return;
                }

                var attribute = annotated.findAttribute("value");
                if (attribute == null) {
                    return;
                }
                var value = attribute.getAttributeValue();
                if (!(value instanceof JvmAnnotationEnumFieldValue)) {
                    return;
                }
                var name = ((JvmAnnotationEnumFieldValue) value).getFieldName();

                var list = minecraftTypesCollections.get(name);
                if (list != null && !list.isEmpty()) {
                    result.addAllElements(list);
                    result.stopHere();
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void addNewMinecraftType(String enumFieldName, String category) {
        try {
            minecraftTypesCollections.put(enumFieldName, (List<LookupElement>) gson
                    .fromJson(new InputStreamReader(new URL(ARCTIC_DATA_URL.replace("${category}", category)).openStream()), Map.class)
                    .keySet()
                    .stream()
                    .map(LookupElementBuilder::create)
                    .collect(Collectors.toList())
            );
        } catch (Throwable exception) {
            exception.printStackTrace();
        }
    }

    private void addNewMinecraftType(String enumFieldName, List<String> staticNames) {
        minecraftTypesCollections.put(enumFieldName, staticNames.stream().map(LookupElementBuilder::create).collect(Collectors.toList()));
    }
}
