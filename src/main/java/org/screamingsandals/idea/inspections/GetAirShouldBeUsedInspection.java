package org.screamingsandals.idea.inspections;

import com.intellij.codeInspection.*;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.patterns.PsiJavaPatterns.psiMethod;

public class GetAirShouldBeUsedInspection extends AbstractBaseJavaLocalInspectionTool {
    private static final Logger LOG = Logger.getInstance("#org.screamingsandals.idea.inspections.GetAirShouldBeUsedInspection");

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        var methodPattern =
                psiMethod()
                        .withName("of")
                        .definedInClass("org.screamingsandals.lib.material.MaterialHolder");

        var prohibitedNames = List.of("air", "minecraft:air");
        var quickFix = new AirQuickFix();

        return new JavaElementVisitor() {
            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                var method = expression.resolveMethod();
                if (methodPattern.accepts(method) && expression.getArgumentList().getExpressionCount() > 0 && expression.getArgumentList().getExpressions()[0] instanceof PsiLiteralExpression) {
                    var literal = (PsiLiteralExpression) expression.getArgumentList().getExpressions()[0];
                    if (literal.getText().startsWith("\"") && literal.getText().endsWith("\"") && prohibitedNames.contains(literal.getText().substring(1, literal.getText().length() - 1).toLowerCase())) {
                        holder.registerProblem(expression, "MaterialMapping.getAir() should be used instead of this", quickFix);
                    }
                }
            }
        };
    }

    private static class AirQuickFix implements LocalQuickFix {

        @Override
        @IntentionName
        public @NotNull String getName() {
            return "Replace with MaterialMapping.getAir()";
        }

        @Override
        @IntentionFamilyName
        public @NotNull String getFamilyName() {
            return getName();
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            try {
                var expression = descriptor.getPsiElement();
                var facade = JavaPsiFacade.getInstance(project);
                var factory = facade.getElementFactory();
                var clazz = facade.findClass("org.screamingsandals.lib.material.MaterialMapping", GlobalSearchScope.allScope(project));
                if (clazz != null) {
                    var expr = (PsiMethodCallExpression) factory.createExpressionFromText("a.getAir()", null);

                    expr.getMethodExpression().getQualifierExpression().replace(factory.createReferenceExpression(clazz));

                    expression.replace(expr);
                }
            } catch (IncorrectOperationException e) {
                LOG.error(e);
            }
        }
    }
}
