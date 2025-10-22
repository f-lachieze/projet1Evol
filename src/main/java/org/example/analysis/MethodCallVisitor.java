// Dans MethodCallVisitor.java

package org.example.analysis;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import org.example.model.CallGraph;

public class MethodCallVisitor extends VoidVisitorAdapter<CallGraph> {

    private String currentMethodSignature;

    // Helper pour obtenir la signature qualifiée complète de la méthode (l'appelant)
    private String getMethodSignature(MethodDeclaration n) {
        try {
            // Utilise le solveur pour obtenir la VRAIE signature
            return n.resolve().getQualifiedSignature();
        } catch (Exception e) {
            // Fallback si le solveur échoue (ex: méthode privée dans une classe anonyme)
            String className = n.findAncestor(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class)
                    .map(c -> c.getFullyQualifiedName().orElse(c.getNameAsString()))
                    .orElse("UnknownClass");
            return className + "." + n.getSignature().asString();
        }
    }

    @Override
    public void visit(MethodDeclaration n, CallGraph collector) {
        currentMethodSignature = getMethodSignature(n);
        super.visit(n, collector);
        currentMethodSignature = null;
    }

    // C'EST ICI LA CORRECTION MAJEURE
    @Override
    public void visit(MethodCallExpr n, CallGraph collector) {
        super.visit(n, collector);

        if (currentMethodSignature != null) {
            try {
                // 1. Demander au Symbol Solver : "Quelle est cette méthode ?"
                ResolvedMethodDeclaration resolvedMethod = n.resolve();

                // 2. Il nous donne la VRAIE signature de l'appelé
                String calleeSignature = resolvedMethod.getQualifiedSignature();

                // 3. Ajouter l'appel correct au graphe
                collector.addCall(currentMethodSignature, calleeSignature);

            } catch (UnsolvedSymbolException e) {
                // Le solveur n'a pas pu trouver la méthode (ex: librairie externe non incluse)
                System.err.println("WARN: Impossible de résoudre l'appel : " + n.getNameAsString() +
                        " (dans " + currentMethodSignature + "). Raison: " + e.getMessage());
            } catch (Exception e) {
                // Autre erreur
                System.err.println("WARN: Erreur en résolvant " + n.getNameAsString() + ": " + e.getClass().getSimpleName());
            }
        }
    }
}