package org.screamingsandals.idea;

import com.google.gson.Gson;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.jvm.annotation.JvmAnnotationEnumFieldValue;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.screamingsandals.idea.components.AutocompletionTypes;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.intellij.patterns.PlatformPatterns.*;
import static com.intellij.patterns.PsiJavaPatterns.psiLiteral;
import static com.intellij.patterns.PsiJavaPatterns.psiMethod;

public class MinecraftTypesCompletionContributor extends CompletionContributor {
    private static final String ANNOTATION = "org.screamingsandals.lib.utils.annotations.ide.CustomAutocompletion";
    private static final String ARCTIC_DATA_URL = "https://raw.githubusercontent.com/Articdive/ArticData/1.17.1/1_17_1_${category}.json";

    private final Map<String, List<LookupElement>> minecraftTypesCollections = new HashMap<>();
    private final Gson gson = new Gson();
    private final AutocompletionTypes types;

    public MinecraftTypesCompletionContributor() {
        types = ApplicationManager.getApplication().getService(AutocompletionTypes.class);

        if (!"1.17.1".equals(types.version)) {
            types.ids = new HashMap<>();
            types.version = "1.17.1";
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(null, "Preparing minecraft types autocompletion") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setFraction(0);
                addNewMinecraftType(indicator, "MATERIAL", "items");
                indicator.setFraction(0.1);
                addNewMinecraftType(indicator, "ENCHANTMENT", "enchantments");
                indicator.setFraction(0.2);
                addNewMinecraftType(indicator, "POTION_EFFECT", "potion_effects");
                indicator.setFraction(0.3);
                addNewMinecraftType(indicator, "POTION", "potions");
                indicator.setFraction(0.4);
                addNewMinecraftType(indicator, "EQUIPMENT_SLOT", List.of("main_hand", "off_hand", "boots", "leggings", "chestplate", "helmet"));
                indicator.setFraction(0.5);
                addNewMinecraftType(indicator, "FIREWORK_EFFECT", List.of("small", "large", "star", "burst", "creeper"));
                indicator.setFraction(0.6);
                addNewMinecraftType(indicator, "ENTITY_TYPE", "entities");
                //indicator.setFraction(0.7);
                //addNewMinecraftType("DAMAGE_CAUSE");
                indicator.setFraction(0.8);
                addNewMinecraftType(indicator, "ATTRIBUTE_TYPE", "attributes");
                indicator.setFraction(0.9);
                addNewMinecraftType(indicator, "GAME_MODE", List.of("survival", "creative", "adventure", "spectator"));

                indicator.setFraction(0.99);
                indicator.setText("Registering autocompletion");

                var pattern = psiElement(JavaTokenType.STRING_LITERAL)
                        .withParent(
                                psiLiteral().methodCallParameter(
                                        psiMethod()
                                                .withAnnotation(ANNOTATION)
                                )
                        );

                MinecraftTypesCompletionContributor.this.extend(CompletionType.BASIC, pattern, new CompletionProvider<>() {
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
        });
    }

    @SuppressWarnings("unchecked")
    private void addNewMinecraftType(ProgressIndicator indicator, String enumFieldName, String category) {
        try {
            if (types.ids.containsKey(enumFieldName)) {
                indicator.setText("Loading " + enumFieldName);
                minecraftTypesCollections.put(enumFieldName, types.ids
                        .get(enumFieldName)
                        .stream()
                        .map(LookupElementBuilder::create)
                        .collect(Collectors.toList())
                );
            } else {
                indicator.setText("Downloading " + enumFieldName);
                var strings = gson
                        .fromJson(new InputStreamReader(new URL(ARCTIC_DATA_URL.replace("${category}", category)).openStream()), Map.class)
                        .keySet();

                minecraftTypesCollections.put(enumFieldName, (List<LookupElement>) strings
                        .stream()
                        .map(LookupElementBuilder::create)
                        .collect(Collectors.toList())
                );

                types.ids.put(enumFieldName, new ArrayList<String>(strings));
            }
        } catch (Throwable exception) {
            exception.printStackTrace();
        }
    }

    private void addNewMinecraftType(ProgressIndicator indicator, String enumFieldName, List<String> staticNames) {
        indicator.setText("Loading " + enumFieldName);
        minecraftTypesCollections.put(enumFieldName, staticNames.stream().map(LookupElementBuilder::create).collect(Collectors.toList()));
    }
}
