package org.example.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Représente le graphe de couplage pondéré entre les classes.
 * La structure est une carte où la clé est une classe source,
 * et la valeur est une autre carte contenant les classes cibles et leur poids de couplage.
 */
public class ClassCouplingGraph {

    // Map<SourceClass, Map<TargetClass, CouplingWeight>>
    private final Map<String, Map<String, Double>> graph = new HashMap<>();

    public void addCoupling(String classA, String classB, double weight) {
        graph.computeIfAbsent(classA, k -> new HashMap<>()).put(classB, weight);
    }

    public Map<String, Map<String, Double>> getGraph() {
        return graph;
    }

    /**
     * Retourne un ensemble de toutes les classes uniques (appelants et appelés)
     * présentes dans le graphe de couplage.
     */
    public Set<String> getAllClasses() {
        Set<String> allClasses = new HashSet<>();

        // Parcourt tous les appelants (les clés principales)
        for (Map.Entry<String, Map<String, Double>> entry : graph.entrySet()) {
            allClasses.add(entry.getKey());

            // Parcourt tous les appelés pour cet appelant
            allClasses.addAll(entry.getValue().keySet());
        }

        return allClasses;
    }


    /**
     * Retourne le poids de couplage (la métrique) d'une classe A vers une classe B.
     * Si aucun couplage n'existe dans ce sens, retourne 0.0.
     *
     * @param callerClass La classe A (appelant)
     * @param calleeClass La classe B (appelé)
     * @return Le poids du couplage (un double), ou 0.0 si inexistant.
     */
    public double getWeight(String callerClass, String calleeClass) {
        // 1. Vérifie si la classe A (callerClass) a des appels sortants
        Map<String, Double> targets = graph.get(callerClass);

        if (targets == null) {
            return 0.0; // Cette classe n'appelle personne
        }

        // 2. Vérifie si elle appelle la classe B (calleeClass)
        // getOrDefault est parfait ici : il retourne le poids s'il existe, ou 0.0 sinon.
        return targets.getOrDefault(calleeClass, 0.0);
    }




}