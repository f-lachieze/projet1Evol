package org.example.analysis;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.example.model.CallGraph;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

// Imports à ajouter en haut de SourceCodeAnalyzer.java


/**
 * Classe responsable de la lecture des fichiers sources et de la construction
 * de l'Abstract Syntax Tree (AST) pour chaque fichier.
 */
public class SourceCodeAnalyzer {

    // Liste pour stocker l'AST de chaque fichier analysé.
    private final List<CompilationUnit> compilationUnits;

    private JavaSymbolSolver symbolSolver;

    public SourceCodeAnalyzer() {
        this.compilationUnits = new ArrayList<>();
    }



    /**
     * Parcourt le répertoire source et construit l'AST pour chaque fichier Java.
     * @param sourceDirPath Le chemin du répertoire source de l'application.
     * @return true si l'analyse a réussi, false sinon.
     */
    public boolean analyze(String sourceDirPath) {
        System.out.println("Configuration du Symbol Solver...");
        // ÉTAPE CRUCIALE : Configurer le solveur AVANT de parser
        setupSymbolSolver(sourceDirPath);

        System.out.println("Début de l'analyse du répertoire : " + sourceDirPath);
        File sourceDir = new File(sourceDirPath);
        // ... (le reste de la méthode 'analyze' est identique)
        // ... (elle appelle processDirectory)
        processDirectory(sourceDir);
        // ...
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

            // for (CompilationUnit cu : analyzer.getCompilationUnits()) {
            //     System.out.println(cu.getPrimaryType().map(t -> t.getNameAsString()).orElse("Sans Type Primaire"));
            // }
        }


    }

    public CallGraph generateCallGraph(String sourceDirPath) {
        this.compilationUnits.clear();
        boolean parsingSuccess = this.analyze(sourceDirPath); // <-- Cette méthode configure maintenant le solveur

        CallGraph callGraph = new CallGraph();
        if (!parsingSuccess) return callGraph;

        System.out.println("Début de la visite (avec Symbol Solver)...");
        MethodCallVisitor visitor = new MethodCallVisitor();
        for (CompilationUnit cu : this.compilationUnits) {
            visitor.visit(cu, callGraph);
        }

        System.out.println("Visite terminée.");
        return callGraph;
    }

    // MÉTHODE MANQUANTE À AJOUTER DANS SourceCodeAnalyzer.java

    // Dans SourceCodeAnalyzer.java

    /**
     * Configure le Symbol Solver pour qu'il trouve les types.
     * @param sourceDirPath Le chemin vers le code source à analyser (le répertoire racine du projet).
     */
    private void setupSymbolSolver(String sourceDirPath) {
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();

        // 1. Lui dire de regarder les classes de base de Java (String, Object, etc.)
        // Il est bon de l'ajouter en premier.
        typeSolver.add(new ReflectionTypeSolver());

        // 2. Lui dire de regarder le code source du projet analysé
        // de manière intelligente, en cherchant les racines "main/java" et "test/java"
        File sourceDir = new File(sourceDirPath);

        // Structure Maven standard
        File mainJava = new File(sourceDir, "main/java");
        if (mainJava.exists() && mainJava.isDirectory()) {
            System.out.println("Ajout du TypeSolver pour : " + mainJava.getAbsolutePath());
            typeSolver.add(new JavaParserTypeSolver(mainJava));
        }

        File testJava = new File(sourceDir, "test/java");
        if (testJava.exists() && testJava.isDirectory()) {
            System.out.println("Ajout du TypeSolver pour : " + testJava.getAbsolutePath());
            typeSolver.add(new JavaParserTypeSolver(testJava));
        }

        // 3. Fallback : si on ne trouve pas de structure standard (ex: projet simple)
        // On ajoute le répertoire racine, mais seulement si on n'a pas trouvé les autres.
        if (!mainJava.exists() && !testJava.exists()) {
            System.out.println("Structure standard non trouvée, ajout du TypeSolver pour : " + sourceDir.getAbsolutePath());
            typeSolver.add(new JavaParserTypeSolver(sourceDir));
        }

        // 4. Créer le solveur et le lier à JavaParser
        this.symbolSolver = new JavaSymbolSolver(typeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
    }
}
