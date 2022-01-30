package org.screamingsandals.idea;

import com.intellij.codeInsight.completion.*;
import com.intellij.lang.jvm.annotation.JvmAnnotationEnumFieldValue;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import static com.intellij.patterns.PsiJavaPatterns.*;

public class JavaMinecraftTypesCompletionContributor extends CompletionContributor {

    public JavaMinecraftTypesCompletionContributor() {
        CompletionTypesHolder.getInstance().whenPrepared(() -> {

                var pattern =
                        psiElement(JavaTokenType.STRING_LITERAL)
                        .withParent(
                                psiLiteral().methodCallParameter(
                                        psiMethod()
                                                .withAnnotation(CompletionTypesHolder.ANNOTATION)
                                )
                        );

                JavaMinecraftTypesCompletionContributor.this.extend(CompletionType.BASIC, pattern, new CompletionProvider<>() {
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
                        var annotated = method.getAnnotation(CompletionTypesHolder.ANNOTATION);
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

                        var list = CompletionTypesHolder.getInstance().getMinecraftTypesCollections().get(name);
                        if (list != null && !list.isEmpty()) {
                            result.addAllElements(list);
                            result.stopHere();
                        }
                    }
                });

                var adventurePattern = psiElement(JavaTokenType.STRING_LITERAL)
                        .withParent(
                                psiLiteral().methodCallParameter(
                                        psiMethod()
                                                .withName("key")
                                                .withParameterCount(1)
                                                .withParent(
                                                        psiClass()
                                                                .withQualifiedName("net.kyori.adventure.key.Key")
                                                )
                                )
                        );

                JavaMinecraftTypesCompletionContributor.this.extend(CompletionType.BASIC, adventurePattern, new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                        var list = CompletionTypesHolder.getInstance().getMinecraftTypesCollections().get("SOUND");
                        if (list != null && !list.isEmpty()) {
                            result.addAllElements(list);
                            result.stopHere();
                        }
                    }
                });
            });
    }
}
