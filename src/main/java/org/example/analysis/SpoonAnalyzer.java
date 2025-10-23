package org.example.analysis;

import spoon.Launcher;
import spoon.reflect.CtModel;
import org.example.model.CallGraph;

public class SpoonAnalyzer {

    public CallGraph analyzeProject(String sourceDirectoryPath) {
        Launcher spoonLauncher = new Launcher();

        // Add the source code directories (Spoon handles main/java, test/java well)
        spoonLauncher.addInputResource(sourceDirectoryPath);

        // Configure Spoon (important for type resolution)
        spoonLauncher.getEnvironment().setAutoImports(true); // Automatically import necessary classes
        spoonLauncher.getEnvironment().setNoClasspath(false); // Use classpath to resolve external types if available

        // Build the Spoon model of the code
        System.out.println("Spoon: Building code model...");
        CtModel model = spoonLauncher.buildModel();
        System.out.println("Spoon: Model built.");

        // Create and run your processor
        SpoonCallGraphProcessor processor = new SpoonCallGraphProcessor();
        System.out.println("Spoon: Running Call Graph processor...");
        model.processWith(processor);
        System.out.println("Spoon: Processor finished.");

        // Get the resulting CallGraph
        return processor.getCallGraph();
    }

    // Example main for testing
    public static void main(String[] args) {
        String projectPath = "/Users/florianlachieze/Desktop/rendu tp1pt2 evoL/les apps testés/tetris"; // Example path
        SpoonAnalyzer analyzer = new SpoonAnalyzer();
        CallGraph graph = analyzer.analyzeProject(projectPath);
        System.out.println(graph.toString()); // Print the resulting graph
    }
}