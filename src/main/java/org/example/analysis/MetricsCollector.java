package org.example.analysis;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import com.github.javaparser.ast.expr.Expression; // Pour le type du scope (objet.attribut)
import com.github.javaparser.ast.expr.NameExpr;    // Pour la vérification asNameExpr()

import java.util.*;

import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.SuperExpr;


import org.example.model.ClassMetric;
import org.example.model.MethodMetric;

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
    // Dans MetricsCollector.java

    public Map<String, ClassMetric> calculateMetrics(List<CompilationUnit> compilationUnits) {
        classMetrics.clear(); // Réinitialiser pour chaque nouvelle analyse

        for (CompilationUnit cu : compilationUnits) {

            // Trouver toutes les déclarations de classes ou d'interfaces dans le fichier
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classNode -> {

                // Logique pour ignorer les types imbriqués (si getFullyQualifiedName est vide)
                if (classNode.getFullyQualifiedName().isEmpty()) {
                    return;
                }

                // 1. Initialiser ClassMetric et collecter les attributs
                String classFullName = classNode.getFullyQualifiedName().get();
                ClassMetric classMetric = new ClassMetric(classFullName, cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse(""));

                Set<String> instanceAttributeNames = new HashSet<>();

                // Calcul du nombre total d'attributs (COMPTEUR)
                final int totalAttributes = classNode.getFields().stream()
                        .mapToInt(field -> field.getVariables().size())
                        .sum();

                // Attribution du nombre d'attributs (Première assignation conservée)
                classMetric.setNumberOfAttributes(totalAttributes);


                // Collecte des noms d'attributs d'instance pour le TCC (COHÉSION)
                classNode.getFields().forEach(field -> {
                    // Collecte les noms des attributs d'instance pour le calcul du TCC
                    if (!field.isStatic()) {
                        field.getVariables().forEach(var -> {
                            instanceAttributeNames.add(var.getNameAsString());
                        });
                    }
                });

                // Initialisation de la variable ATFD pour l'agrégation (Utilisation d'un tableau pour la mutabilité)
                final int[] classAtfd = {0};


                // 2. Traitement des méthodes
                classNode.getMethods().forEach(method -> {
                    // L'appel doit passer classFullName pour que le visiteur ATFD fonctionne
                    MethodMetric metric = calculateMethodMetric(method, classFullName, instanceAttributeNames);

                    // Ajouter la métrique à la classe (met à jour le WMC, LoC total, etc.)
                    classMetric.addMethodMetric(metric);

                    // NOUVEAU : Agrégation de l'ATFD pour la classe
                    classAtfd[0] += metric.getForeignDataAccesses();
                });

                // Définir la métrique ATFD totale de la classe
                classMetric.setAtfd(classAtfd[0]);

                // 3. Calcul des métriques de classe finales (TCC)
                classMetric.calculateTCC();

                // 4. Stocker la métrique de classe
                classMetrics.put(classFullName, classMetric);
            });
        }

        return classMetrics;
    }


    // =========================================================================
    // Méthodes de Calcul des Métriques
    // =========================================================================

    /**
     * Calcule les métriques pour une seule méthode (LoC, CC, collecte TCC).
     */
    private MethodMetric calculateMethodMetric(MethodDeclaration n, String className, Set<String> instanceAttributeNames) {
        // L'appel au constructeur fonctionne
        MethodMetric metric = new MethodMetric(className, n.getNameAsString());

        // 1. Calcul des LoC
        int methodLoC = 0;
        if (n.getEnd().isPresent() && n.getBegin().isPresent()) {
            methodLoC = n.getEnd().get().line - n.getBegin().get().line + 1;
        }
        metric.setLoc(methodLoC);

        // 2. Calcul du nombre de paramètres
        int paramCount = n.getParameters().size();
        metric.setParameterCount(paramCount);

        // 3. Calcul de la Complexité Cyclomatique (CC)
        int complexity = n.findAll(Statement.class, s -> {
            return s.isIfStmt() || s.isWhileStmt() || s.isForStmt() || s.isSwitchStmt();
        }).size();

        metric.setCyclomaticComplexity(complexity + 1);

        // 4. Analyse TCC : Détection de l'accès aux attributs
        if (n.getBody().isPresent()) {
            MethodBodyVisitor tccVisitor = new MethodBodyVisitor(instanceAttributeNames, className);
            tccVisitor.visit(n.getBody().get(), metric);
        }

        return metric;
    }


    /**
     * Visiteur pour détecter l'accès aux attributs (FieldAccessExpr) dans le corps d'une méthode.
     */
    private static class MethodBodyVisitor extends VoidVisitorAdapter<MethodMetric> {
        private final Set<String> instanceAttributeNames; // Attributs de la classe parente
        private final String currentClassName;
        public MethodBodyVisitor(Set<String> instanceAttributeNames, String currentClassName) {
            this.instanceAttributeNames = instanceAttributeNames;
            this.currentClassName = currentClassName;
        }

        // Dans MetricsCollector.java, classe MethodBodyVisitor

        @Override
        public void visit(FieldAccessExpr n, MethodMetric collector) {
            super.visit(n, collector);

            String fieldName = n.getNameAsString();

            // 1. Logique TCC (Accès aux attributs locaux)
            if (instanceAttributeNames.contains(fieldName)) {
                collector.addAttributeUsed(fieldName);
                return;
            }

            // 2. Logique ATFD (Accès aux attributs étrangers)
            Expression scope = n.getScope(); // Pas d'Optional si version ancienne
            if (scope != null) { // Vérification explicite de null
                // Exclure 'this.attribut' (accès local implicite)
                if (scope instanceof ThisExpr) {
                    return;
                }

                // Exclure 'super.attribut' (héritage)
                if (scope instanceof SuperExpr) {
                    return;
                }

                // Si l'expression est un NameExpr (ex: 'objet.attribut'), c'est un accès potentiel
                if (scope instanceof NameExpr) {
                    String scopeName = ((NameExpr) scope).getNameAsString();
                    if (!scopeName.equals(currentClassName)) {
                        collector.incrementForeignDataAccesses();
                    }
                }
            }
        }
    }

    // CORRECTION : La classe ClassVisitor et la méthode calculateClassLoC ont été supprimées
    // car elles étaient redondantes ou inutilisées.
}
