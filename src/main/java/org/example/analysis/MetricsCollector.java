package org.example.analysis;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.*;

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
    public Map<String, ClassMetric> calculateMetrics(List<CompilationUnit> compilationUnits) {
        classMetrics.clear(); // Réinitialiser pour chaque nouvelle analyse

        for (CompilationUnit cu : compilationUnits) {

            // Trouver toutes les déclarations de classes ou d'interfaces dans le fichier
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classNode -> {

                // CORRECTION 1 & 2 : Logique pour ignorer les types imbriqués (classes internes, anonymes, etc.)
                if (classNode.getFullyQualifiedName().isEmpty()) {
                    // C'est probablement une classe anonyme, locale, ou imbriquée. On l'ignore.
                    return;
                }

                // Dans MetricsCollector.java, méthode calculateMetrics

// ...
                // 1. Initialiser ClassMetric et collecter les attributs d'instance pour le TCC
                String classFullName = classNode.getFullyQualifiedName().get();
                ClassMetric classMetric = new ClassMetric(classFullName, cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse(""));

                Set<String> instanceAttributeNames = new HashSet<>();

                // Calcul du nombre total d'attributs (COMPTEUR)
                final int totalAttributes = classNode.getFields().stream()
                        .mapToInt(field -> field.getVariables().size())
                        .sum();

                // Attribution ici, après le calcul et avant la boucle TCC
                classMetric.setNumberOfAttributes(totalAttributes);


                // Collecte des noms d'attributs d'instance pour le TCC (COHÉSION)
                classNode.getFields().forEach(field -> {

                    // LIGNE SUPPRIMÉE : totalAttributes += field.getVariables().size();

                    // Collecte les noms des attributs d'instance pour le calcul du TCC
                    if (!field.isStatic()) {
                        field.getVariables().forEach(var -> {
                            instanceAttributeNames.add(var.getNameAsString());
                        });
                    }
                });
                // ... (Le reste de votre méthode continue ici)

                classMetric.setNumberOfAttributes(totalAttributes);


                // 2. Traitement des méthodes
                classNode.getMethods().forEach(method -> {
                    // Calculer les métriques de méthode (LoC, CC, et collecte TCC)
                    MethodMetric metric = calculateMethodMetric(method, classFullName, instanceAttributeNames);

                    // Ajouter la métrique à la classe (met à jour le WMC, LoC total, etc.)
                    classMetric.addMethodMetric(metric);
                });

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
            MethodBodyVisitor tccVisitor = new MethodBodyVisitor(instanceAttributeNames);
            tccVisitor.visit(n.getBody().get(), metric);
        }

        return metric;
    }


    /**
     * Visiteur pour détecter l'accès aux attributs (FieldAccessExpr) dans le corps d'une méthode.
     */
    private static class MethodBodyVisitor extends VoidVisitorAdapter<MethodMetric> {
        private final Set<String> instanceAttributeNames; // Attributs de la classe parente

        public MethodBodyVisitor(Set<String> instanceAttributeNames) {
            this.instanceAttributeNames = instanceAttributeNames;
        }

        @Override
        public void visit(FieldAccessExpr n, MethodMetric collector) {
            super.visit(n, collector);

            // 1. Détecter l'accès direct (ex: this.attribut)
            String fieldName = n.getNameAsString();

            if (instanceAttributeNames.contains(fieldName)) {
                // C'est un attribut d'instance de la classe parente
                collector.addAttributeUsed(fieldName);
            }
        }
    }

    // CORRECTION : La classe ClassVisitor et la méthode calculateClassLoC ont été supprimées
    // car elles étaient redondantes ou inutilisées.
}