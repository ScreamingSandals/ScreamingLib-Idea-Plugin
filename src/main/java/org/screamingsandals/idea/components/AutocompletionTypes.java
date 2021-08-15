package org.screamingsandals.idea.components;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@State(
        name = "slib_minecraft_autocompletion_types",
        storages = {
                @Storage("slibMinecraftAutocompletionTypes.xml")
        }
)
public class AutocompletionTypes implements PersistentStateComponent<AutocompletionTypes> {
  public String version;
  public Map<String, List<String>> ids;

  public AutocompletionTypes getState() {
    return this;
  }

  public void loadState(@NotNull AutocompletionTypes state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}