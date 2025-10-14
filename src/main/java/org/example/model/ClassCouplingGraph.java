package org.example.model;

import java.util.HashMap;
import java.util.Map;

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
}