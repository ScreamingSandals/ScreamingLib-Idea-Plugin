package org.screamingsandals.idea;

import com.intellij.codeInsight.completion.*;
import com.intellij.lang.jvm.annotation.JvmAnnotationEnumFieldValue;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiMethod;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;


public class KotlinMinecraftTypesCompletionContributor extends CompletionContributor {

    public KotlinMinecraftTypesCompletionContributor() {
        CompletionTypesHolder.getInstance().whenPrepared(() -> {
                // TODO: simplify this and filter by annotation in the pattern and not just in the code
                var pattern = PlatformPatterns
                        .psiElement(KtTokens.REGULAR_STRING_PART)
                        .withParent(
                                PlatformPatterns.psiElement(KtLiteralStringTemplateEntry.class)
                                        .withParent(
                                                PlatformPatterns.psiElement(KtStringTemplateExpression.class)
                                                        .withParent(
                                                                PlatformPatterns.psiElement(KtValueArgument.class)
                                                                        .withParent(
                                                                                PlatformPatterns.psiElement(KtValueArgumentList.class)
                                                                                        .withParent(
                                                                                                PlatformPatterns.psiElement(KtCallExpression.class)
                                                                                        )
                                                                        )
                                                        )
                                        )
                        );

                KotlinMinecraftTypesCompletionContributor.this.extend(CompletionType.BASIC, pattern, new CompletionProvider<>() {
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
                        } while (!(f instanceof KtCallExpression));

                        var exp = (KtCallExpression) f;
                        var callee = exp.getCalleeExpression();
                        if (callee == null) {
                            return;
                        }
                        var references = callee.getReferences();
                        if (references.length < 1) {
                            return;
                        }
                        var reference = references[0];
                        reference.getElement(); // idea, you what?
                        var method = reference.resolve();
                        if (!(method instanceof PsiMethod)) {
                            return;
                        }
                        var annotated = ((PsiMethod) method).getAnnotation(CompletionTypesHolder.ANNOTATION);
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
            });
    }
}
