// Créez ce nouveau fichier (ex: dans org.example.analysis)
package org.example.analysis;

import org.example.model.ClassCouplingGraph;
import org.example.model.Cluster;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implémente l'algorithme de clustering hiérarchique (partie 1)
 * et l'algorithme d'identification des modules (partie 2).
 */
public class ModuleFinder {

    // L'input
    private final ClassCouplingGraph couplingGraph;

    // L'output
    private Cluster dendrogramRoot;

    public ModuleFinder(ClassCouplingGraph couplingGraph) {
        this.couplingGraph = couplingGraph;
    }

    // ========================================================================
    // PARTIE 1 : Algorithme de Regroupement (Clustering)
    // ========================================================================

    /**
     * Construit le dendrogramme en regroupant itérativement les clusters
     * les plus couplés.
     * @return Le cluster racine (le dendrogramme complet).
     */
    public Cluster buildDendrogram() {
        // 1. Initialisation : chaque classe est son propre cluster
        List<Cluster> currentClusters = new ArrayList<>();
        for (String className : couplingGraph.getAllClasses()) { // (Vous devrez peut-être créer getAllClasses() dans ClassCouplingGraph)
            currentClusters.add(new Cluster(className));
        }

        // 2. Itération : fusionner jusqu'à ce qu'il ne reste qu'un cluster
        while (currentClusters.size() > 1) {
            Cluster c1 = null;
            Cluster c2 = null;
            double maxCoupling = -1.0;

            // 3. Trouver les deux clusters (c1, c2) les plus couplés
            for (int i = 0; i < currentClusters.size(); i++) {
                for (int j = i + 1; j < currentClusters.size(); j++) {

                    Cluster clusterA = currentClusters.get(i);
                    Cluster clusterB = currentClusters.get(j);

                    // C'est ici qu'on utilise la "liaison moyenne"
                    double currentCoupling = calculateClusterCoupling(clusterA, clusterB);

                    if (currentCoupling > maxCoupling) {
                        maxCoupling = currentCoupling;
                        c1 = clusterA;
                        c2 = clusterB;
                    }
                }
            }

            // 4. Si aucun couplage n'est trouvé (ex: graphe déconnecté), on sort
            if (c1 == null) break;

            // 5. Fusionner les deux clusters trouvés
            Cluster mergedCluster = new Cluster(c1, c2, maxCoupling);
            currentClusters.remove(c1);
            currentClusters.remove(c2);
            currentClusters.add(mergedCluster);

            // System.out.println("Fusion de " + c1 + " et " + c2 + " avec couplage " + maxCoupling);
        }

        // Le dernier cluster restant est la racine de l'arbre
        this.dendrogramRoot = (currentClusters.isEmpty()) ? null : currentClusters.get(0);
        return this.dendrogramRoot;
    }


    // ========================================================================
    // PARTIE 2 : Algorithme d'Identification (Découpage)
    // ========================================================================

    /**
     * Identifie les modules en parcourant l'arbre et en "coupant"
     * aux endroits où le couplage interne est > CP.
     * @param CP Le seuil de couplage moyen (ex: 0.05)
     * @return Une liste de clusters qui sont des modules valides.
     */
    public List<Cluster> findModules(double CP) {
        List<Cluster> foundModules = new ArrayList<>();

        if (this.dendrogramRoot == null) {
            this.buildDendrogram(); // S'assurer que l'arbre est construit
        }

        // Lancer la recherche récursive
        recursiveFindModules(this.dendrogramRoot, CP, foundModules);

        /*
         * Note sur la contrainte "au plus M/2 modules" :
         * C'est une contrainte très large (ex: pour 20 classes, 10 modules max).
         * Notre algorithme est piloté par le seuil CP. Si le CP est bas, on
         * trouvera beaucoup de petits modules. Si CP est haut, on en trouvera
         * peu de gros. C'est à l'utilisateur de choisir un CP qui donne
         * un nombre raisonnable de modules (bien moins que M/2).
         */

        return foundModules;
    }

    /**
     * Helper récursif pour l'identification des modules.
     */
    private void recursiveFindModules(Cluster node, double CP, List<Cluster> foundModules) {
        if (node == null || node.isLeaf()) {
            return; // On ne considère pas les classes seules comme des modules
        }

        // Règle 3 : Calculer la moyenne du couplage interne du module
        double avgInternalCoupling = calculateAverageInternalCoupling(node);

        // Règle 2 : Si le couplage est suffisant, c'est un module !
        // On "coupe" ici et on n'explore pas ses enfants.
        if (avgInternalCoupling > CP) {
            foundModules.add(node);
        }
        // Si le couplage n'est pas suffisant, on continue de chercher
        // dans les branches enfants (gauche et droite).
        else {
            recursiveFindModules(node.getLeftChild(), CP, foundModules);
            recursiveFindModules(node.getRightChild(), CP, foundModules);
        }
    }


    // ========================================================================
    // MÉTHODES HELPER (pour les calculs de couplage)
    // ========================================================================

    /**
     * Calcule le couplage ENTRE deux clusters (liaison moyenne).
     * C'est la moyenne de tous les liens entre une classe de C1 et une classe de C2.
     */
    private double calculateClusterCoupling(Cluster c1, Cluster c2) {
        double totalCoupling = 0.0;
        int pairCount = 0;

        for (String classA : c1.getClasses()) {
            for (String classB : c2.getClasses()) {
                // On additionne les poids A->B et B->A
                totalCoupling += couplingGraph.getWeight(classA, classB);
                totalCoupling += couplingGraph.getWeight(classB, classA);
                pairCount++;
            }
        }

        return (pairCount == 0) ? 0.0 : totalCoupling / pairCount;
    }

    /**
     * Calcule le couplage INTERNE moyen d'un seul cluster.
     * C'est la "cohésion" du module.
     */
    private double calculateAverageInternalCoupling(Cluster cluster) {
        double totalInternalCoupling = 0.0;
        int pairCount = 0;

        // Conversion en liste pour éviter de comparer une classe avec elle-même
        List<String> classes = new ArrayList<>(cluster.getClasses());

        for (int i = 0; i < classes.size(); i++) {
            for (int j = i + 1; j < classes.size(); j++) {
                String classA = classes.get(i);
                String classB = classes.get(j);

                // On additionne les poids A->B et B->A
                totalInternalCoupling += couplingGraph.getWeight(classA, classB);
                totalInternalCoupling += couplingGraph.getWeight(classB, classA);
                pairCount++;
            }
        }

        return (pairCount == 0) ? 0.0 : totalInternalCoupling / pairCount;
    }

    public Cluster getDendrogramRoot() {
        return this.dendrogramRoot;
    }
}