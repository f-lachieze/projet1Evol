// CallGraph.java
package org.example.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Représente le Graphe d'Appel de l'application.
 * Les clés sont les méthodes appelantes (Callers),
 * les valeurs sont l'ensemble des méthodes appelées (Callees).
 * Les méthodes sont stockées sous leur forme complète (ex: com.package.Class.method(int,String)).
 */
public class CallGraph {
    // Map: Key = Caller Method Full Signature, Value = Set of Callee Method Signatures
    private final Map<String, Set<String>> graph;

    public CallGraph() {
        this.graph = new HashMap<>();
    }

    /**
     * Ajoute un appel entre deux méthodes.
     * @param callerSignature La signature complète de la méthode appelante.
     * @param calleeSignature La signature complète de la méthode appelée.
     */
    public void addCall(String callerSignature, String calleeSignature) {
        // Initialise l'ensemble des appelés si la clé (l'appelant) n'existe pas
        graph.computeIfAbsent(callerSignature, k -> new HashSet<>()).add(calleeSignature);
    }

    public Map<String, Set<String>> getGraph() {
        return graph;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Call Graph:\n");
        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(" calls:\n");
            for (String callee : entry.getValue()) {
                sb.append("    -> ").append(callee).append("\n");
            }
        }
        return sb.toString();
    }




}