package org.example.model;

import java.util.HashSet;
import java.util.Set;


/** Représente les métriques collectées pour une seule méthode. */
public class MethodMetric {

    // Identifiants (doivent rester final)
    private final String methodName;
    private final String parentClassName;

    // Métriques (doivent être modifiables, donc non-final)
    private int loc; // Lignes de code du corps de la méthode
    private int parameterCount;
    private int cyclomaticComplexity; // Essentiel pour le calcul du WMC

    // NOUVEAU CHAMP : Stocke les noms des attributs de la classe utilisés par cette méthode
    private Set<String> attributesUsed = new HashSet<>();

    /**
     * CONSTRUCTEUR CORRIGÉ : N'accepte que les identifiants (Nom de la méthode et de la classe parente).
     * Les métriques sont définies ensuite via les setters.
     */
    public MethodMetric(String parentClassName, String methodName) { // J'ai inversé pour correspondre à l'ordre dans le collector
        this.parentClassName = parentClassName;
        this.methodName = methodName;
    }

    // --- Getters ---

    public String getMethodName() { return methodName; }
    public String getParentClassName() { return parentClassName; }
    public int getLoc() { return loc; }
    public int getParameterCount() { return parameterCount; }
    public int getCyclomaticComplexity() { return cyclomaticComplexity; } // Ajouté

    // Getter
    public Set<String> getAttributesUsed() {
        return attributesUsed;
    }

    // --- Setters (Nécessaires pour le MetricsCollector) ---

    public void setLoc(int loc) {
        this.loc = loc;
    }

    public void setParameterCount(int parameterCount) {
        this.parameterCount = parameterCount;
    }

    public void setCyclomaticComplexity(int cyclomaticComplexity) {
        this.cyclomaticComplexity = cyclomaticComplexity;
    }

    // Setter pour ajouter l'attribut (utilisé par le Visitor)
    public void addAttributeUsed(String attributeName) {
        this.attributesUsed.add(attributeName);
    }

}