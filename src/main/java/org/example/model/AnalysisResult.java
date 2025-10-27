// CHEMIN : src/main/java/org/example/model/AnalysisResult.java

package org.example.model; // <-- Vérifie cette ligne

import org.example.model.CallGraph; // Assure-toi d'avoir les imports nécessaires
import org.example.model.ClassMetric;
import java.util.Map;

// Vérifie que c'est bien 'public class AnalysisResult'
public class AnalysisResult {
    public final CallGraph javaParserGraph;
    public final CallGraph spoonGraph;
    public final Map<String, ClassMetric> metrics;
    public final boolean jpSuccess;
    public final boolean spoonSuccess;

    // Le constructeur
    public AnalysisResult(CallGraph jpGraph, CallGraph spGraph, Map<String, ClassMetric> metrics, boolean jpOk, boolean spOk) {
        this.javaParserGraph = jpGraph;
        this.spoonGraph = spGraph;
        this.metrics = metrics;
        this.jpSuccess = jpOk;
        this.spoonSuccess = spOk;
    }
}