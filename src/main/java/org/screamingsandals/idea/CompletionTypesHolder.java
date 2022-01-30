package org.screamingsandals.idea;

import com.google.gson.Gson;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.screamingsandals.idea.components.AutocompletionTypes;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class CompletionTypesHolder {
    public static final String ANNOTATION = "org.screamingsandals.lib.utils.annotations.ide.CustomAutocompletion";
    private static final String ARCTIC_DATA_URL = "https://raw.githubusercontent.com/Articdive/ArticData/1.18.1/1_18_1_${category}.json";
    @Getter
    private static final CompletionTypesHolder instance = new CompletionTypesHolder();

    @Getter
    private final Map<String, List<LookupElement>> minecraftTypesCollections = new HashMap<>();
    private final Gson gson = new Gson();
    private final AutocompletionTypes types;
    private final CountDownLatch latch = new CountDownLatch(1);

    public CompletionTypesHolder() {
        types = ApplicationManager.getApplication().getService(AutocompletionTypes.class);

        if (!"1.18.1".equals(types.version)) {
            types.ids = new HashMap<>();
            types.version = "1.18.1";
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(null, "Preparing minecraft types autocompletion") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                double base = 0.052631579;
                indicator.setFraction(0);
                addNewMinecraftType(indicator, "MATERIAL", "items");
                indicator.setFraction(base);
                addNewMinecraftType(indicator, "ENCHANTMENT", "enchantments");
                indicator.setFraction(base * 2);
                addNewMinecraftType(indicator, "POTION_EFFECT", "potion_effects");
                indicator.setFraction(base * 3);
                addNewMinecraftType(indicator, "POTION", "potions");
                indicator.setFraction(base * 4);
                addNewMinecraftType(indicator, "EQUIPMENT_SLOT", List.of("main_hand", "off_hand", "boots", "leggings", "chestplate", "helmet"));
                indicator.setFraction(base * 5);
                addNewMinecraftType(indicator, "FIREWORK_EFFECT", List.of("small", "large", "star", "burst", "creeper"));
                indicator.setFraction(base * 6);
                addNewMinecraftType(indicator, "ENTITY_TYPE", "entities");
                //indicator.setFraction(base * 7);
                //addNewMinecraftType("DAMAGE_CAUSE");
                indicator.setFraction(base * 8);
                addNewMinecraftType(indicator, "ATTRIBUTE_TYPE", "attributes");
                indicator.setFraction(base * 9);
                addNewMinecraftType(indicator, "GAME_MODE", List.of("survival", "creative", "adventure", "spectator"));
                //indicator.setFraction(base * 10);
                //addNewMinecraftType(indicator, "INVENTORY_TYPE");
                indicator.setFraction(base * 11);
                addNewMinecraftType(indicator, "ENTITY_POSE", List.of("standing", "fall_flying", "sleeping", "swimming", "spin_attack", "sneaking", "dying", "long_jumping"));
                indicator.setFraction(base * 12);
                addNewMinecraftType(indicator, "DIFFICULTY", List.of("peaceful", "easy", "normal", "hard"));
                indicator.setFraction(base * 13);
                addNewMinecraftType(indicator, "DIMENSION", "dimension_types");
                indicator.setFraction(base * 14);
                addNewMinecraftType(indicator, "BLOCK", "blocks"); // TODO: block states autocompletion
                //indicator.setFraction(base * 15);
                //addNewMinecraftType(indicator, "GAME_RULE");
                indicator.setFraction(base * 16);
                addNewMinecraftType(indicator, "WEATHER", List.of("downfall", "clear"));
                indicator.setFraction(base * 17);
                addNewMinecraftType(indicator, "PARTICLE_TYPE", "particles");
                indicator.setFraction(base * 18);
                addNewMinecraftType(indicator, "SOUND", "sounds");
                indicator.setFraction(base * 19);
                addNewMinecraftType(indicator, "SOUND_SOURCE", List.of("master", "music", "record", "weather", "block", "hostile", "neutral", "player", "ambient", "voice"));

                indicator.setFraction(0.99);
                indicator.setText("Registering autocompletion");

                latch.countDown();
            }
        });
    }

    public void whenPrepared(Runnable runnable) {
        ProgressManager.getInstance().run(new Task.Backgroundable(null, "Registering minecraft types autocompletion") {

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runnable.run();
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
