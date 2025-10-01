package org.example.model;

/** Représente les métriques collectées pour une seule méthode. */
public class MethodMetric {

    // Identifiants (doivent rester final)
    private final String methodName;
    private final String parentClassName;

    // Métriques (doivent être modifiables, donc non-final)
    private int loc; // Lignes de code du corps de la méthode
    private int parameterCount;
    private int cyclomaticComplexity; // Essentiel pour le calcul du WMC

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
}