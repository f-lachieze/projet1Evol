package org.example.analysis;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.model.ClassMetric; // Assurez-vous que ce chemin est correct
import org.example.model.MethodMetric; // Assurez-vous que ce chemin est correct

/**
 * Collecte et calcule les métriques à partir de l'arbre syntaxique abstrait (AST).
 */
public class MetricsCollector {

    // Contient les résultats finaux des métriques par classe
    private Map<String, ClassMetric> classMetrics = new HashMap<>();

    /**
     * Point d'entrée pour le calcul des métriques.
     * @param compilationUnits La liste des ASTs à analyser.
     * @return Une Map contenant les métriques calculées pour chaque classe.
     */
    public Map<String, ClassMetric> calculateMetrics(List<CompilationUnit> compilationUnits) {
        classMetrics.clear(); // Réinitialiser pour chaque nouvelle analyse

        for (CompilationUnit cu : compilationUnits) {
            // Un visiteur pour parcourir l'AST de chaque unité de compilation
            new ClassVisitor().visit(cu, null);
        }

        return classMetrics;
    }


    // =========================================================================
    // Visiteur JavaParser pour parcourir l'AST
    // =========================================================================

    private class ClassVisitor extends VoidVisitorAdapter<Void> {

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            if (!n.isInterface()) {
                String className = n.getFullyQualifiedName().orElse(n.getNameAsString());

                // 1. Création de l'objet Metric (Constructeur corrigé)
                ClassMetric classMetric = new ClassMetric(className); // Maintenant ceci fonctionne

                // 2. Calcul des métriques de classe directes (seul attributeCount est encore nécessaire ici)
                classMetric.setNumberOfAttributes(n.getFields().size()); // Ligne 36
                // La LoC totale et le nombre de méthodes sont maintenant calculés via addMethodMetric.
                // classMetric.setLoc(calculateClassLoC(n)); // Cette ligne doit être supprimée
                // classMetric.setNumberOfMethods(n.getMethods().size()); // Cette ligne doit être supprimée

                // 3. Visiter les méthodes pour les métriques de méthode
                for (MethodDeclaration method : n.getMethods()) {
                    MethodMetric methodMetric = calculateMethodMetrics(method, className);
                    classMetric.addMethodMetric(methodMetric); // Cette ligne met à jour WMC, LoC et nombre de méthodes
                }

                classMetrics.put(className, classMetric);
            }
            super.visit(n, arg);
        }
    }

    // =========================================================================
    // Méthodes de Calcul des Métriques
    // =========================================================================

    /**
     * Calcule la LoC (Lines of Code) de la classe (nombre de lignes entre le début et la fin).
     */
    private int calculateClassLoC(ClassOrInterfaceDeclaration n) {
        // Simple calcul basé sur les positions de début et fin dans le fichier source
        return n.getEnd().get().line - n.getBegin().get().line + 1;
    }

    /**
     * Calcule les métriques pour une seule méthode (LoC, WMC simple).
     */
    // Dans MetricsCollector.java

    private MethodMetric calculateMethodMetrics(MethodDeclaration n, String className) {
        // L'appel au constructeur fonctionne maintenant !
        MethodMetric metric = new MethodMetric(className, n.getNameAsString());

        // 1. Calcul des LoC et attribution via Setter
        int methodLoC = n.getEnd().get().line - n.getBegin().get().line + 1;
        metric.setLoc(methodLoC); // Utilisation du Setter

        // 2. Calcul du nombre de paramètres et attribution via Setter
        int paramCount = n.getParameters().size();
        metric.setParameterCount(paramCount); // Utilisation du Setter

        // 3. Calcul de la Complexité Cyclomatique et attribution via Setter
        int complexity = n.findAll(Statement.class, s -> {
            return s.isIfStmt() || s.isWhileStmt() || s.isForStmt() || s.isSwitchStmt();
        }).size();

        metric.setCyclomaticComplexity(complexity + 1); // Utilisation du Setter (+1 pour la complexité de base)

        return metric;
    }

    // Vous devrez ajouter ici des méthodes pour WMC, TCC, ATFD, etc.

}