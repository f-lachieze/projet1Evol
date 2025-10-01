package org.example.model;

import java.util.ArrayList;
import java.util.List;

/** Représente les métriques collectées pour une seule classe de l'application. */
public class ClassMetric {

    // On utilise le nom qualifié complet (package.Classe) comme identifiant unique
    private final String fullName;

    // Métriques
    private int totalLoc;
    private int numberOfMethods;
    private int numberOfAttributes;
    private int wmc; // Weighted Methods per Class (Complexité Cyclomatique Totale)
    private double tcc; // Tight Class Cohesion (Cohésion de Classe)

    // Contient les métriques détaillées de chaque méthode
    private final List<MethodMetric> methodMetrics = new ArrayList<>();

    /**
     * CONSTRUCTEUR CORRIGÉ pour correspondre à l'appel dans MetricsCollector.
     * Le MetricsCollector fournit le nom qualifié complet ou le nom simple.
     */
    public ClassMetric(String fullName) {
        this.fullName = fullName;
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
}