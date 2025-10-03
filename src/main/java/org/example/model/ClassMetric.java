package org.example.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Représente les métriques collectées pour une seule classe de l'application. */
public class ClassMetric {

    // On utilise le nom qualifié complet (package.Classe) comme identifiant unique
    private final String fullName;

    String packageName;
    // Métriques
    private int totalLoc;
    private int numberOfMethods;
    private int numberOfAttributes;

    private int wmc = 0;

    private double tcc = 0.0;

    // Contient les métriques détaillées de chaque méthode
    private final List<MethodMetric> methodMetrics = new ArrayList<>();

    /**
     * CONSTRUCTEUR CORRIGÉ pour correspondre à l'appel dans MetricsCollector.
     * Le MetricsCollector fournit le nom qualifié complet ou le nom simple.
     */
    public ClassMetric(String fullName, String packageName) {
        this.fullName = fullName;
        this.packageName = packageName;

        this.wmc = 0;
        this.numberOfMethods = 0;
        this.totalLoc = 0;
    }

    // --- Méthodes d'ajout et de calcul pour le MetricsCollector ---

    // Simplifie l'ajout et maintient les totaux à jour
    public void addMethodMetric(MethodMetric metric) {
        this.methodMetrics.add(metric);

        // Mise à jour automatique des totaux lors de l'ajout d'une méthode
        this.numberOfMethods = this.methodMetrics.size();
        this.totalLoc += metric.getLoc();
        this.wmc += metric.getCyclomaticComplexity(); // WMC = Somme des complexités cyclomatiques
    }

    // --- Getters et Setters (pour le MetricsCollector et JavaFX TableViews) ---

    public String getFullName() {
        return fullName;
    }

    // Extrait le nom simple de la classe (utile pour l'affichage)
    public String getClassName() {
        int lastDot = fullName.lastIndexOf('.');
        return lastDot == -1 ? fullName : fullName.substring(lastDot + 1);
    }

    // Extrait le nom du package (utile pour l'affichage)
    public String getPackageName() {
        int lastDot = fullName.lastIndexOf('.');
        return lastDot == -1 ? "" : fullName.substring(0, lastDot);
    }

    // Getters pour les métriques de base
    public int getTotalLoc() { return totalLoc; }
    public int getNumberOfMethods() { return numberOfMethods; }
    public int getNumberOfAttributes() { return numberOfAttributes; }
    public int getWmc() { return wmc; }
    public double getTcc() { return tcc; }
    public List<MethodMetric> getMethodMetrics() { return methodMetrics; }




    // Setters pour les métriques qui ne sont pas mises à jour automatiquement
    public void setNumberOfAttributes(int numberOfAttributes) {
        this.numberOfAttributes = numberOfAttributes;
    }

    public void setTcc(double tcc) {
        this.tcc = tcc;
    }

    // Dans ClassMetric.java

// ...

    /**
     * Calcule la métrique Tight Class Cohesion (TCC)
     * TCC = (Nombre de paires de méthodes connectées * 2) / (M * (M - 1))
     * où M est le nombre de méthodes et une paire est connectée si les deux méthodes
     * utilisent au moins un attribut d'instance commun.
     */
    public void calculateTCC() {
        int M = methodMetrics.size();

        // TCC est indéfini ou 1.0 si M < 2
        if (M < 2) {
            this.tcc = 1.0;
            return;
        }

        int connectedPairs = 0;

        // Calculer le nombre de paires connectées
        for (int i = 0; i < M; i++) {
            for (int j = i + 1; j < M; j++) {
                MethodMetric m1 = methodMetrics.get(i);
                MethodMetric m2 = methodMetrics.get(j);

                // Vérifier s'il y a un attribut commun (Intersection des ensembles d'attributs)
                Set<String> intersection = new HashSet<>(m1.getAttributesUsed());
                intersection.retainAll(m2.getAttributesUsed()); // Intersection = attributs communs

                if (!intersection.isEmpty()) {
                    connectedPairs++; // La paire (m1, m2) est connectée
                }
            }
        }

        // Nombre total de paires de méthodes: M * (M - 1) / 2
        int totalPairs = (M * (M - 1)) / 2;

        // Formule TCC = (DC + IC) / TotalPairs.
        // Ici, nous utilisons l'approximation TCC = DC / TotalPairs
        // Pour être exact, TCC = Nombre total de paires connectées (DC + IC) / TotalPairs
        this.tcc = (double) connectedPairs / totalPairs;
    }
}