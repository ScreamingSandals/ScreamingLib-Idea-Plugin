package org.screamingsandals.idea.inspections;

import com.intellij.codeInspection.reference.EntryPoint;
import com.intellij.codeInspection.reference.RefElement;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.PsiElement;
import org.jdom.Element;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScreamingLibAnnotationEntryPoint extends EntryPoint {
    @Override
    @NotNull
    @Nls
    public String getDisplayName() {
        return "ScreamingLib Entry Point";
    }

    @Override
    public String @Nullable [] getIgnoreAnnotations() {
        return new String[]{
                "org.screamingsandals.lib.utils.annotations.methods.OnDisable",
                "org.screamingsandals.lib.utils.annotations.methods.OnEnable",
                "org.screamingsandals.lib.utils.annotations.methods.OnPostEnable",
                "org.screamingsandals.lib.utils.annotations.methods.OnPreDisable",
                "org.screamingsandals.lib.utils.annotations.methods.Provider",
                "org.screamingsandals.lib.utils.annotations.methods.ShouldRunControllable",
                "org.screamingsandals.lib.utils.annotations.methods.Plugin",
                "org.screamingsandals.lib.event.OnEvent"
        };
    }

    @Override
    public boolean isEntryPoint(@NotNull RefElement refElement, @NotNull PsiElement psiElement) {
        return false;
    }

    @Override
    public boolean isEntryPoint(@NotNull PsiElement psiElement) {
        return false;
    }

    @Override
    public boolean isSelected() {
        return false;
    }

    @Override
    public void setSelected(boolean selected) {
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
    }
}
