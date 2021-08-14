package org.screamingsandals.idea.inspections;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import static com.intellij.patterns.PsiJavaPatterns.psiMethod;

public class AlternativeMethodAvailableInspection extends AbstractBaseJavaLocalInspectionTool {
    private static final Logger LOG = Logger.getInstance("#org.screamingsandals.idea.inspections.AlternativeMethodAvailableInspection");
    private static final String ANNOTATION = "org.screamingsandals.lib.utils.annotations.ide.OfMethodAlternative";

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        var methodPattern =
                psiMethod()
                        .withAnnotation(ANNOTATION);

        return new JavaElementVisitor() {
            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                var method = expression.resolveMethod();
                if (methodPattern.accepts(method)) {
                    var annotation = method.getAnnotation(ANNOTATION);
                    if (annotation == null) {
                        return;
                    }
                    var classAttribute =  annotation.findAttributeValue("value");
                    var methodNameAttribute = annotation.findAttribute("methodName");
                    if (!(classAttribute instanceof PsiClassObjectAccessExpression) || methodNameAttribute == null) {
                        return;
                    }
                    var methodNameVal = methodNameAttribute.getAttributeValue();
                    if (!(methodNameVal instanceof JvmAnnotationConstantValue)) {
                        return;
                    }
                    var theClass = ((PsiClassObjectAccessExpression) classAttribute).getOperand().getType();
                    var theMethodName = ((JvmAnnotationConstantValue) methodNameVal).getConstantValue();
                    if (!(theClass instanceof PsiClassType)) {
                        return;
                    }

                    var theReferencedClass = ((PsiClassType) theClass).resolve();
                    if (theReferencedClass == null) {
                        return;
                    }

                    holder.registerProblem(expression, theReferencedClass.getName() + "." + theMethodName + " should be used instead of " + method.getContainingClass().getName() + "." + method.getName(), new AlternativeMethodQuickFix(
                            SmartPointerManager.createPointer(theReferencedClass),
                            String.valueOf(theMethodName)
                    ));
                }
            }
        };
    }

    @Data
    private static class AlternativeMethodQuickFix implements LocalQuickFix {
        private final SmartPsiElementPointer<PsiClass> theClass;
        private final String theMethodName;

        @Override
        @IntentionName
        public @NotNull String getName() {
            return "Replace with " + theClass.getElement().getName() + "." + theMethodName + "()";
        }

        @Override
        @IntentionFamilyName
        public @NotNull String getFamilyName() {
            return getName();
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            try {
                var clazz = theClass.getElement();
                if (clazz == null) {
                    return;
                }

                var expression = descriptor.getPsiElement();
                var facade = JavaPsiFacade.getInstance(project);
                var factory = facade.getElementFactory();
                var args = ((PsiMethodCallExpression) expression).getArgumentList().getExpressions();

                var expr = (PsiMethodCallExpression) factory.createExpressionFromText("a." + theMethodName + "(" + String.join(", ", "b".repeat(args.length).split("")) + ")", null);

                expr.getMethodExpression().getQualifierExpression().replace(factory.createReferenceExpression(clazz));

                // copy old arguments
                for (var i = 0; i < args.length; i++) {
                    expr.getArgumentList().getExpressions()[i].replace(args[i]);
                }

                expression.replace(expr);
            } catch (IncorrectOperationException e) {
                LOG.error(e);
            }
        }
    }
}
