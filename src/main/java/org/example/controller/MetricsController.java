package org.example.controller;

// MetricsController.java (Le Cerveau de l'interface)
import org.example.analysis.SourceCodeAnalyzer; // Assurez-vous d'avoir le bon package
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox; // Ajout pour VBox
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import org.example.analysis.MetricsCollector;
import org.example.model.ClassMetric;
import com.github.javaparser.ast.CompilationUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javafx.collections.FXCollections;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.model.MethodMetric;

import java.io.File;

public class MetricsController {

    // ========================================================================
    // CHAMPS FXML (Correspondant aux fx:id dans MetricsView.fxml)
    // ========================================================================

    @FXML private TextField sourcePathField;
    @FXML private GridPane summaryGrid;
    @FXML private TabPane resultsTabPane;

    // Tableaux pour l'onglet "Classes et Méthodes Critiques"
    @FXML private TableView<MethodMetric> topMethodsTable;
    @FXML private TableView topAttributesTable; // Ajouté (pour topAttributesTable)
    @FXML private TableView intersectingClassesTable; // Ajouté (pour intersectingClassesTable)

    // Éléments pour l'onglet "Filtrage par Nombre de Méthodes (X)"
    @FXML private TextField xValueField;
    @FXML private TableView filteredClassesTable; // Ajouté (pour filteredClassesTable)

    private SourceCodeAnalyzer analyzer = new SourceCodeAnalyzer();
    private MetricsCollector collector = new MetricsCollector(); // <-- Initialisation
    private Map<String, ClassMetric> currentMetrics; // Pour stocker les résultats

    // ========================================================================
    // LOGIQUE DE L'APPLICATION
    // ========================================================================



    @FXML
    public void initialize() {
        // --- Initialisation de la TableView Top Methods ---

        // 1. Créer la colonne du Nom de la Classe
        TableColumn<MethodMetric, String> classNameCol = new TableColumn<>("Classe");
        // 'getParentClassName' doit correspondre EXACTEMENT au getter de MethodMetric (getParentClassName())
        classNameCol.setCellValueFactory(new PropertyValueFactory<>("parentClassName"));
        classNameCol.setPrefWidth(250);

        // 2. Créer la colonne du Nombre de Méthodes (ici, on affichera la LoC de la méthode)
        TableColumn<MethodMetric, Integer> locCol = new TableColumn<>("LoC");
        // 'loc' doit correspondre EXACTEMENT au getter de MethodMetric (getLoc())
        locCol.setCellValueFactory(new PropertyValueFactory<>("loc"));
        locCol.setPrefWidth(150);

        // 3. Ajouter les colonnes (si elles n'ont pas été ajoutées dans le FXML)
        // Si vous les avez déclarées dans le FXML, assurez-vous que les types correspondent.

        // Si la TableView n'a pas de colonnes FXML, utilisez ceci:
        // topMethodsTable.getColumns().add(classNameCol);
        // topMethodsTable.getColumns().add(locCol);

        // Si les colonnes existent déjà dans le FXML, il faut modifier leur fx:id dans FXML
        // et les utiliser ici (ce qui est plus propre). Pour l'instant, faisons au plus simple:

        // Nettoyer les colonnes définies dans le FXML et les remplacer par les nôtres :
        topMethodsTable.getColumns().clear();
        topMethodsTable.getColumns().addAll(classNameCol, locCol);
    }


    // ========================================================================
    // GESTIONNAIRES D'ÉVÉNEMENTS (Correspondant aux onAction="#handle...")
    // ========================================================================

    @FXML
    private void handleBrowse(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Sélectionner le répertoire source");

        // Utiliser la Stage de l'application si possible, sinon créer une Stage temporaire
        Stage currentStage = (Stage) ((Control) event.getSource()).getScene().getWindow();
        File selectedDirectory = chooser.showDialog(currentStage);

        if (selectedDirectory != null) {
            sourcePathField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    @FXML
    private void handleAnalyze(ActionEvent event) {
        String path = sourcePathField.getText();

        // ... vérifications de chemin ...

        // 1. Lancer l'analyse (Lecture et Parsing des fichiers)
        boolean success = analyzer.analyze(path);

        if (success) {
            // 2. Collecter et Calculer les métriques
            List<CompilationUnit> asts = analyzer.getCompilationUnits();

            // ************ Appel au MetricsCollector ************
            currentMetrics = collector.calculateMetrics(asts);

            // 1. Récupérer toutes les métriques de méthodes
            List<MethodMetric> allMethods = getAllMethodMetrics(currentMetrics);

            // 2. Transformer la liste en ObservableList pour JavaFX
            topMethodsTable.setItems(FXCollections.observableArrayList(allMethods));

            showAlert("Succès", "Analyse statique terminée ! " + currentMetrics.size() + " classes analysées.", Alert.AlertType.INFORMATION);
        } else {
            showAlert("Échec", "Échec de l'analyse. Vérifiez le chemin.", Alert.AlertType.ERROR);
        }
    }

    /**
     * Gère l'action du bouton "Filtrer" (associé à onAction="#handleFilterByX").
     * C'est la méthode qui manquait et qui causait l'erreur.
     */
    @FXML
    private void handleFilterByX(ActionEvent event) {
        String xText = xValueField.getText();

        try {
            int xValue = Integer.parseInt(xText);
            // Ici, vous implémenterez la logique pour filtrer et afficher les résultats
            System.out.println("Filtrage lancé pour X > " + xValue);
            // updateFilteredTable(xValue);

        } catch (NumberFormatException e) {
            showAlert("Erreur de format", "Veuillez entrer un nombre entier valide pour X.", Alert.AlertType.WARNING);
        }
    }


    // ========================================================================
    // MÉTHODES UTILITAIRES
    // ========================================================================

    // Méthode utilitaire pour afficher les messages d'alerte
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private List<MethodMetric> getAllMethodMetrics(Map<String, ClassMetric> classMetrics) {
        List<MethodMetric> allMethods = new ArrayList<>();
        for (ClassMetric classMetric : classMetrics.values()) {
            allMethods.addAll(classMetric.getMethodMetrics());
        }
        return allMethods;
    }

    // private void updateUI(MetricsResults results) {
    //     updateSummary(results);
    //     updateRankings(results);
    //     // ... autres mises à jour
    // }
}