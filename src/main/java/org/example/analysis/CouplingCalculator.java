// Dans org.example.analysis/CouplingCalculator.java
package org.example.analysis;

import org.example.model.CallGraph;
import org.example.model.ClassCouplingGraph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CouplingCalculator {

    /**
     * Calcule le graphe de couplage pondéré à partir d'un graphe d'appel.
     * @param callGraph Le graphe d'appel (méthode -> méthodes).
     * @return Le graphe de couplage (classe -> classes).
     */
    public ClassCouplingGraph calculate(CallGraph callGraph) {
        // Étape 1: Compter le nombre de relations (appels) brutes entre chaque paire de classes.
        // Map<SourceClass, Map<TargetClass, NumberOfCalls>>
        Map<String, Map<String, Integer>> rawCouplingCounts = new HashMap<>();
        int totalInterClassCalls = 0; // Le dénominateur de la formule

        for (Map.Entry<String, Set<String>> entry : callGraph.getGraph().entrySet()) {
            String callerMethod = entry.getKey();
            String callerClass = extractClassName(callerMethod);

            for (String calleeMethod : entry.getValue()) {
                String calleeClass = extractClassName(calleeMethod);

                // On ne compte que les appels INTER-CLASSES (entre classes différentes)
                if (callerClass != null && calleeClass != null && !callerClass.equals(calleeClass)) {
                    // Incrémenter le nombre d'appels entre la classe A et la classe B
                    rawCouplingCounts
                            .computeIfAbsent(callerClass, k -> new HashMap<>())
                            .merge(calleeClass, 1, Integer::sum);

                    totalInterClassCalls++; // Incrémenter le nombre total d'appels inter-classes
                }
            }
        }

        // Étape 2: Calculer le poids final en utilisant la formule du TP. [cite: 34]
        ClassCouplingGraph couplingGraph = new ClassCouplingGraph();
        if (totalInterClassCalls > 0) {
            for (Map.Entry<String, Map<String, Integer>> entry : rawCouplingCounts.entrySet()) {
                String sourceClass = entry.getKey();
                for (Map.Entry<String, Integer> targetEntry : entry.getValue().entrySet()) {
                    String targetClass = targetEntry.getKey();
                    int count = targetEntry.getValue();

                    // Formule : Couplage(A,B) = Compte(A,B) / Total
                    double weight = (double) count / totalInterClassCalls;

                    couplingGraph.addCoupling(sourceClass, targetClass, weight);
                }
            }
        }

        return couplingGraph;
    }

    /**
     * Extrait le nom complet de la classe à partir de la signature complète d'une méthode.
     * Ex: "org.example.MaClasse.maMethode(...)" -> "org.example.MaClasse"
     */
    private String extractClassName(String methodSignature) {
        int lastDot = methodSignature.lastIndexOf('.');
        if (lastDot == -1) {
            return null; // Pas un nom de classe valide
        }
        String potentialClassName = methodSignature.substring(0, lastDot);
        // Pour les appels comme "objet.methode(...)", le nom de la classe est l'objet lui-même
        // C'est une approximation, mais c'est ce que notre graphe d'appel nous donne
        return potentialClassName;
    }
}