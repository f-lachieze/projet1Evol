// Dans org.example.analysis.CouplingCalculator.java

package org.example.analysis;

import org.example.model.CallGraph;
import org.example.model.ClassCouplingGraph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Calcule le graphe de couplage pondéré entre les classes,
 * basé sur le graphe d'appel des méthodes.
 */
public class CouplingCalculator {

    /**
     * Calcule le graphe de couplage pondéré.
     * @param callGraph Le graphe d'appel (méthode -> méthodes)
     * @return Un ClassCouplingGraph (Classe -> Classe -> Poids)
     */
    public ClassCouplingGraph calculate(CallGraph callGraph) {

        // --- Étape A: Compter les appels bruts entre les classes ---

        // Map<ClasseAppelante, Map<ClasseAppelée, NombreAppels>>
        Map<String, Map<String, Integer>> rawCounts = new HashMap<>();
        int totalInterClassCalls = 0; // Le DÉNOMINATEUR

        for (Map.Entry<String, Set<String>> entry : callGraph.getGraph().entrySet()) {
            String callerMethod = entry.getKey();
            String callerClass = getClassFromMethodSignature(callerMethod);

            // Si on n'a pas pu extraire la classe, on ignore
            if (callerClass == null) continue;

            for (String calleeMethod : entry.getValue()) {
                String calleeClass = getClassFromMethodSignature(calleeMethod);
                if (calleeClass == null) continue;

                // On ne compte QUE les appels inter-classes
                if (!callerClass.equals(calleeClass)) {

                    // Incrémente le compteur pour (ClasseA -> ClasseB)
                    rawCounts.computeIfAbsent(callerClass, k -> new HashMap<>())
                            .merge(calleeClass, 1, Integer::sum);

                    // Incrémente le dénominateur total
                    totalInterClassCalls++;
                }
            }
        }

        // --- Étape B: Calculer la métrique (le poids) ---

        ClassCouplingGraph couplingGraph = new ClassCouplingGraph();

        // Si le dénominateur est 0 (aucun appel inter-classe), on retourne un graphe vide.
        if (totalInterClassCalls == 0) {
            return couplingGraph;
        }

        for (Map.Entry<String, Map<String, Integer>> entry : rawCounts.entrySet()) {
            String callerClass = entry.getKey();
            for (Map.Entry<String, Integer> targetEntry : entry.getValue().entrySet()) {
                String calleeClass = targetEntry.getKey();
                int count = targetEntry.getValue(); // Numérateur

                // La formule de l'exercice : Numérateur / Dénominateur
                double weight = (double) count / totalInterClassCalls;

                // Ajouter au graphe final
                couplingGraph.addCoupling(callerClass, calleeClass, weight);
            }
        }

        return couplingGraph;
    }

    /**
     * Extrait le nom de la classe d'une signature de méthode complète.
     * Ex: "com.app.Service.methodeA(...)" -> "com.app.Service"
     * Ex: "client.methodeB(...)" -> "client" (Cas du bug)
     */
    private String getClassFromMethodSignature(String methodSignature) {
        int lastDot = methodSignature.lastIndexOf('.');

        // Gère le cas où on n'a pas de '.' (peu probable) ou si la signature est invalide
        if (lastDot == -1 || lastDot == 0) {
            return null;
        }

        // On vérifie s'il y a une parenthèse après le dernier point
        int parenthesis = methodSignature.indexOf('(', lastDot);
        if (parenthesis != -1) {
            // Le nom de la classe est tout ce qui précède le dernier point
            return methodSignature.substring(0, lastDot);
        }

        // Cas étrange (ex: "client.methodeB"), on suppose que "client" est la "classe"
        // Ceci est une approximation due au bug du Symbol Solver
        if (methodSignature.lastIndexOf('.', lastDot -1) == -1) {
            return methodSignature.substring(0, lastDot);
        }

        return null; // Format non reconnu
    }
}