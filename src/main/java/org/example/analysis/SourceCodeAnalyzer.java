package org.example.analysis;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Classe responsable de la lecture des fichiers sources et de la construction
 * de l'Abstract Syntax Tree (AST) pour chaque fichier.
 */
public class SourceCodeAnalyzer {

    // Liste pour stocker l'AST de chaque fichier analysé.
    private final List<CompilationUnit> compilationUnits;

    public SourceCodeAnalyzer() {
        this.compilationUnits = new ArrayList<>();
    }

    /**
     * Parcourt le répertoire source et construit l'AST pour chaque fichier Java.
     * @param sourceDirPath Le chemin du répertoire source de l'application.
     * @return true si l'analyse a réussi, false sinon.
     */
    public boolean analyze(String sourceDirPath) {
        System.out.println("Début de l'analyse du répertoire : " + sourceDirPath);
        File sourceDir = new File(sourceDirPath);

        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            System.err.println("Erreur : Le chemin spécifié n'existe pas ou n'est pas un répertoire.");
            return false;
        }

        // Appel récursif pour parcourir tous les fichiers dans le répertoire et ses sous-répertoires
        processDirectory(sourceDir);

        System.out.println("Analyse terminée. Nombre d'unités de compilation (ASTs) trouvées : " +
                this.compilationUnits.size());
        return !this.compilationUnits.isEmpty();
    }

    /**
     * Méthode récursive pour traiter les répertoires et fichiers.
     */
    private void processDirectory(File dir) {
        // Liste les fichiers et dossiers dans le répertoire
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isDirectory()) {
                // Si c'est un répertoire, on continue la récursion
                processDirectory(file);
            } else if (file.getName().endsWith(".java")) {
                // Si c'est un fichier Java, on le parse et on ajoute son AST
                parseJavaFile(file);
            }
        }
    }

    /**
     * Parse un fichier Java pour obtenir son AST (CompilationUnit).
     */
    private void parseJavaFile(File file) {
        try {
            // StaticJavaParser s'occupe de la lecture et du parsing du fichier
            CompilationUnit cu = StaticJavaParser.parse(file);
            this.compilationUnits.add(cu);
            System.out.println("  -> Fichier parsé : " + file.getAbsolutePath());
        } catch (FileNotFoundException e) {
            System.err.println("Erreur de lecture du fichier " + file.getAbsolutePath() + " : " + e.getMessage());
        }
    }

    /**
     * Getter pour accéder aux résultats (les ASTs).
     */
    public List<CompilationUnit> getCompilationUnits() {
        return compilationUnits;
    }

    // --- Exemple d'utilisation dans la méthode main ---
    public static void main(String[] args) {
        // REMPLACEZ PAR LE CHEMIN DE VOTRE APPLICATION SOURCE
        String appPath = "/chemin/vers/votre/application/source";

        // Assurez-vous d'avoir un répertoire de test si vous ne fournissez pas d'argument
        if (args.length > 0) {
            appPath = args[0];
        } else {
            System.out.println("Utilisation : Veuillez fournir le chemin du répertoire source en argument.");
            System.out.println("Utilisation du chemin par défaut pour le test : " + appPath);
        }

        SourceCodeAnalyzer analyzer = new SourceCodeAnalyzer();
        boolean success = analyzer.analyze(appPath);

        if (success) {
            System.out.println("\nL'analyse statique peut commencer en utilisant ces ASTs :");
            // Maintenant, vous pouvez passer cette liste à la prochaine étape (le Collecteur de Métriques)
            // for (CompilationUnit cu : analyzer.getCompilationUnits()) {
            //     System.out.println(cu.getPrimaryType().map(t -> t.getNameAsString()).orElse("Sans Type Primaire"));
            // }
        }
    }
}
