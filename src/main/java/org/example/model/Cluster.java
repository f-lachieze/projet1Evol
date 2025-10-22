// Créez ce nouveau fichier (ex: dans org.example.model)
package org.example.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Représente un nœud dans le dendrogramme de clustering.
 * Soit une feuille (une classe) ou un nœud (regroupement de deux clusters).
 */
public class Cluster {

    private final Set<String> classes;
    private final Cluster leftChild;
    private final Cluster rightChild;
    // Le "height" est le niveau de couplage auquel ce cluster a été formé
    private final double height;

    /**
     * Constructeur pour une FEUILLE (une classe unique).
     */
    public Cluster(String className) {
        this.classes = new HashSet<>();
        this.classes.add(className);
        this.leftChild = null;
        this.rightChild = null;
        this.height = 0.0; // Une classe seule a un couplage de 0
    }

    /**
     * Constructeur pour un NŒUD (une fusion de deux clusters).
     */
    public Cluster(Cluster left, Cluster right, double coupling) {
        this.classes = new HashSet<>(left.getClasses());
        this.classes.addAll(right.getClasses());
        this.leftChild = left;
        this.rightChild = right;
        this.height = coupling;
    }

    public Set<String> getClasses() {
        return classes;
    }

    public Cluster getLeftChild() {
        return leftChild;
    }

    public Cluster getRightChild() {
        return rightChild;
    }

    public boolean isLeaf() {
        return this.leftChild == null && this.rightChild == null;
    }

    // Retourne le nom de la classe si c'est une feuille
    public String getClassName() {
        if (isLeaf()) {
            return classes.iterator().next();
        }
        return null;
    }

    @Override
    public String toString() {
        if (isLeaf()) {
            return "Cluster[" + getClassName() + "]";
        }
        return "Cluster[size=" + classes.size() + ", height=" + String.format("%.3f", height) + "]";
    }
}