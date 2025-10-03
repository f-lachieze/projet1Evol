// MethodCallVisitor.java
package org.example.analysis;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.example.model.CallGraph;

/**
 * Visiteur dédié à la recherche des appels de méthodes dans l'AST
 * pour construire le Graphe d'Appel.
 */
public class MethodCallVisitor extends VoidVisitorAdapter<CallGraph> {

    // Variable d'instance pour garder la signature de la méthode actuellement visitée (l'appelant)
    private String currentMethodSignature;

    // Définition simple et complète d'une méthode pour l'identification
    private String getMethodSignature(MethodDeclaration n) {
        // Pour un graphe d'appel basique, on utilise: FullClassName.methodName
        String className = n.findAncestor(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class)
                .map(c -> c.getFullyQualifiedName().orElse(c.getNameAsString()))
                .orElse("UnknownClass");

        return className + "." + n.getSignature().asString();
    }

    // 1. Visite chaque déclaration de méthode pour en définir la signature (Caller)
    @Override
    public void visit(MethodDeclaration n, CallGraph collector) {
        // Enregistrer la signature de la méthode courante
        currentMethodSignature = getMethodSignature(n);

        // Continuer la visite à l'intérieur du corps de la méthode
        super.visit(n, collector);

        // Nettoyer après avoir quitté la méthode
        currentMethodSignature = null;
    }

    // 2. Visite chaque appel de méthode à l'intérieur du corps (Callee)
    @Override
    public void visit(MethodCallExpr n, CallGraph collector) {
        super.visit(n, collector); // Important : visiter récursivement

        // On ne traite les appels que si nous sommes à l'intérieur d'une méthode déclarée
        if (currentMethodSignature != null) {
            String calleeName = n.getNameAsString();

            // L'approximation pour le Callee est le point le plus difficile sans Symbol Solving.
            // Nous allons utiliser une simplification qui fonctionne pour la plupart des cas simples :

            // a) Le nom de la méthode : calleeName

            // b) L'objet receveur (scope) pour déterminer la classe
            String calleeClass;

            if (n.getScope().isPresent()) {
                // Si l'appel a un scope (ex: objet.methode()), on utilise le type du scope
                calleeClass = n.getScope().get().toString(); // Ceci donne la variable (ex: "client")
                // ATTENTION: n.getScope().get().toString() donne souvent le nom de la variable (client.acheter())
                // Pour une implémentation sans symbol solving, il est difficile de déterminer le *type* exact de 'client'.
                // Pour simplifier l'exercice :
                if (n.getScope().get().isNameExpr()) {
                    calleeClass = n.getScope().get().asNameExpr().getNameAsString();
                } else {
                    calleeClass = n.getScope().get().toString();
                }

            } else {
                // Si pas de scope (ex: methode() ou this.methode()), c'est un appel à l'intérieur de la classe courante
                calleeClass = currentMethodSignature.substring(0, currentMethodSignature.lastIndexOf('.'));
            }

            // Pour l'exercice, on approxime la signature de l'appelé comme suit:
            String approximatedCalleeSignature = calleeClass + "." + calleeName + "(...)";

            // Ajouter l'appel au graphe
            collector.addCall(currentMethodSignature, approximatedCalleeSignature);
        }
    }
}