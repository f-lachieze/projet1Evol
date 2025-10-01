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

        // Colonne 3 : Ajoutons la Complexité Cyclomatique pour une meilleure information
        TableColumn<MethodMetric, Integer> ccCol = new TableColumn<>("Complexité Cyclomatique");
        ccCol.setCellValueFactory(new PropertyValueFactory<>("cyclomaticComplexity"));
        ccCol.setPrefWidth(200);

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

// Dans MetricsController.java

// ...

    // Ajoutez cette méthode utilitaire dans MetricsController
    private int calculateTotalAppLoc(Map<String, ClassMetric> classMetrics) {
        // Le total de LoC de chaque ClassMetric a déjà été mis à jour dans le MetricsCollector
        // lors de l'ajout de chaque méthode.

        return classMetrics.values().stream()
                .mapToInt(ClassMetric::getTotalLoc)
                .sum();
    }

    /**
     * Met à jour le GridPane 'summaryGrid' avec les métriques globales.
     */
    private void updateSummary(Map<String, ClassMetric> classMetrics) {
        summaryGrid.getChildren().clear(); // Nettoyer la grille avant l'ajout

        // 1. Métrique : Nombre total de classes
        int totalClasses = classMetrics.size();

        // 2. Métrique : LoC totale de l'application
        int totalLoc = calculateTotalAppLoc(classMetrics);

        // --- Affichage dans la grille (GridPane) ---

        // Ligne 0 : Nombre de Classes
        summaryGrid.add(new Label("Nombre de classes analysées :"), 0, 0);
        summaryGrid.add(new Label(String.valueOf(totalClasses)), 1, 0);

        // Ligne 1 : LoC Totale
        summaryGrid.add(new Label("LoC Totale de l'application :"), 0, 1);
        summaryGrid.add(new Label(String.valueOf(totalLoc)), 1, 1);

        // Vous ajouterez ici les autres totaux (Total WMC, etc.)
    }

    // Dans MetricsController.java

    @FXML
    private void handleAnalyze(ActionEvent event) {
        String path = sourcePathField.getText();

        if (path == null || path.trim().isEmpty()) {
            showAlert("Erreur", "Veuillez spécifier un chemin source.", Alert.AlertType.ERROR);
            return;
        }

        // 1. Lancer l'analyse (Lecture et Parsing des fichiers)
        boolean success = analyzer.analyze(path);

        if (success) {
            // 2. Collecter et Calculer les métriques
            List<CompilationUnit> asts = analyzer.getCompilationUnits();
            currentMetrics = collector.calculateMetrics(asts);

            // ====================================================================
            // MISE À JOUR DE L'INTERFACE UTILISATEUR
            // ====================================================================

            // 1. Mise à jour de la SYNTHÈSE (Onglet 1)
            updateSummary(currentMetrics);

            // 2. Mise à jour de la TABLEVIEW DES MÉTHODES (Onglet 2 - Gauche)
            List<MethodMetric> allMethods = getAllMethodMetrics(currentMetrics);
            topMethodsTable.setItems(FXCollections.observableArrayList(allMethods));

            // (NOTE : Vous devez encore coder la mise à jour de topAttributesTable en utilisant ClassMetric)

            // 3. Afficher le message de succès
            showAlert("Succès", "Analyse statique terminée ! " + currentMetrics.size() + " classes analysées. Résultats mis à jour.", Alert.AlertType.INFORMATION);

            // Optionnel : Changer l'onglet actif après l'analyse
            resultsTabPane.getSelectionModel().select(0); // Sélectionne l'onglet "Synthèse & Totaux"

        } else {
            showAlert("Échec", "Échec de l'analyse. Vérifiez le chemin.", Alert.AlertType.ERROR);
        }
    }


}