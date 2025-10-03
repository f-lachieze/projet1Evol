package org.example.controller;

// MetricsController.java (Le Cerveau de l'interface)
import org.example.analysis.SourceCodeAnalyzer; // Assurez-vous d'avoir le bon package
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import org.example.analysis.MetricsCollector;
import org.example.model.ClassMetric;
import com.github.javaparser.ast.CompilationUnit;

// Dans MetricsController.java (Section Imports)
import java.util.ArrayList;
import java.util.Collections; // Ajouté pour Collections
import java.util.Comparator; // Ajouté pour Comparator
import java.util.List;
import java.util.Map;
import java.util.Set; // Ajouté pour Set
import java.util.stream.Collectors; // Ajouté pour Collectors
// ...

import javafx.collections.FXCollections;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.model.MethodMetric;

import java.io.File;

import org.example.model.CallGraph; // <-- NOUVEL IMPORT
import org.example.analysis.MethodCallVisitor; // <-- NOUVEL IMPORT


public class MetricsController {

    // ========================================================================
    // CHAMPS FXML (Correspondant aux fx:id dans MetricsView.fxml)
    // ========================================================================

    @FXML private TextField sourcePathField;
    @FXML private GridPane summaryGrid;
    @FXML private TabPane resultsTabPane;

    // Tableaux pour l'onglet "Classes et Méthodes Critiques"
    @FXML private TableView<MethodMetric> topMethodsTable;
    @FXML private TableView<ClassMetric> topAttributesTable; // Ajouté (pour topAttributesTable)
    @FXML private TableView<ClassMetric> intersectingClassesTable; // Ajouté (pour intersectingClassesTable)

    // Éléments pour l'onglet "Filtrage par Nombre de Méthodes (X)"
    @FXML private TextField xValueField;
    @FXML private TableView<ClassMetric> filteredClassesTable; // Ajouté (pour filteredClassesTable)

    // NOUVEAUX ÉLÉMENTS pour l'affichage du Graphe d'Appel
    @FXML private Tab callGraphTab; // Le nouvel onglet
    @FXML private TextArea callGraphTextArea; // Le nouveau composant d'affichage



    private SourceCodeAnalyzer analyzer = new SourceCodeAnalyzer();
    private MetricsCollector collector = new MetricsCollector(); // <-- Initialisation
    private Map<String, ClassMetric> currentMetrics; // Pour stocker les résultats

    private CallGraph callGraph = new CallGraph();

    // ========================================================================
    // LOGIQUE DE L'APPLICATION
    // ========================================================================



    @FXML
    public void initialize() {

        // ========================================================================
        // 0. Déclaration des colonnes TCC (pour les réutiliser dans tous les tableaux)
        // ========================================================================
        // TCC est un Double et doit être formaté
        TableColumn<ClassMetric, Double> tccCol = new TableColumn<>("TCC");
        tccCol.setCellValueFactory(new PropertyValueFactory<>("tcc")); // Utilise le getter getTcc()
        tccCol.setPrefWidth(80);
        // Formater la valeur pour afficher seulement 3 décimales (ex: 0.500)
        tccCol.setCellFactory(column -> new TableCell<ClassMetric, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.3f", item));
                }
            }
        });

        // ========================================================================
        // 1. Initialisation de la TableView Top Methods (Méthodes critiques - Q1.1, #12)
        // ========================================================================
        TableColumn<MethodMetric, String> classNameCol = new TableColumn<>("Classe");
        classNameCol.setCellValueFactory(new PropertyValueFactory<>("parentClassName"));
        classNameCol.setPrefWidth(250);

        TableColumn<MethodMetric, Integer> locCol = new TableColumn<>("LoC");
        locCol.setCellValueFactory(new PropertyValueFactory<>("loc"));
        locCol.setPrefWidth(100);

        TableColumn<MethodMetric, Integer> ccCol = new TableColumn<>("CC");
        ccCol.setCellValueFactory(new PropertyValueFactory<>("cyclomaticComplexity"));
        ccCol.setPrefWidth(100);

        topMethodsTable.getColumns().clear();
        topMethodsTable.getColumns().addAll(classNameCol, locCol, ccCol);


        // ========================================================================
        // 2. Initialisation de la TableView Top Attributes (Classes Top 10% - Q1.1, #8, #9)
        // ========================================================================
        TableColumn<ClassMetric, String> attrClassNameCol = new TableColumn<>("Classe");
        attrClassNameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        attrClassNameCol.setPrefWidth(200);

        TableColumn<ClassMetric, Integer> nbMethodsCol = new TableColumn<>("Nb Méthodes");
        nbMethodsCol.setCellValueFactory(new PropertyValueFactory<>("numberOfMethods"));
        nbMethodsCol.setPrefWidth(100);

        TableColumn<ClassMetric, Integer> nbAttributesCol = new TableColumn<>("Nb Attributs");
        nbAttributesCol.setCellValueFactory(new PropertyValueFactory<>("numberOfAttributes"));
        nbAttributesCol.setPrefWidth(100);

        TableColumn<ClassMetric, Integer> wmcCol = new TableColumn<>("WMC");
        wmcCol.setCellValueFactory(new PropertyValueFactory<>("wmc"));
        wmcCol.setPrefWidth(80);

        // Déclarez une nouvelle colonne ATFD (Integer)
        TableColumn<ClassMetric, Integer> atfdCol = new TableColumn<>("ATFD");
        atfdCol.setCellValueFactory(new PropertyValueFactory<>("atfd")); // Utilise getAtfd()
        atfdCol.setPrefWidth(80);


        topAttributesTable.getColumns().clear();
        // AJOUT DE TCC ici
        topAttributesTable.getColumns().addAll(attrClassNameCol, nbMethodsCol, nbAttributesCol, wmcCol, tccCol, atfdCol);


        // ========================================================================
        // 3. Initialisation de la TableView Intersecting Classes (Intersection - Q1.1, #10)
        // ========================================================================
        TableColumn<ClassMetric, String> interClassNameCol = new TableColumn<>("Classe");
        interClassNameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        interClassNameCol.setPrefWidth(250);

        TableColumn<ClassMetric, Integer> interNbMethodsCol = new TableColumn<>("Méthodes");
        interNbMethodsCol.setCellValueFactory(new PropertyValueFactory<>("numberOfMethods"));
        interNbMethodsCol.setPrefWidth(100);

        TableColumn<ClassMetric, Integer> interNbAttributesCol = new TableColumn<>("Attributs");
        interNbAttributesCol.setCellValueFactory(new PropertyValueFactory<>("numberOfAttributes"));
        interNbAttributesCol.setPrefWidth(100);

        TableColumn<ClassMetric, Integer> interWmcCol = new TableColumn<>("WMC");
        interWmcCol.setCellValueFactory(new PropertyValueFactory<>("wmc"));
        interWmcCol.setPrefWidth(80);




        // Pour TCC dans ce tableau, on réutilise la définition tccCol mais en créant une nouvelle instance
        // pour éviter les problèmes si le FXML gère mal la réutilisation d'une colonne (bonne pratique)
        TableColumn<ClassMetric, Double> interTccCol = new TableColumn<>("TCC");
        interTccCol.setCellValueFactory(new PropertyValueFactory<>("tcc"));
        interTccCol.setPrefWidth(80);
        interTccCol.setCellFactory(tccCol.getCellFactory()); // Réutilise le formatage

        // Déclarez une nouvelle colonne ATFD (Integer)
        TableColumn<ClassMetric, Integer> interAtfdCol = new TableColumn<>("ATFD");
        // CORRECTION ICI : Utilisez la variable interAtfdCol !
        interAtfdCol.setCellValueFactory(new PropertyValueFactory<>("atfd")); // <-- CORRIGÉ
        interAtfdCol.setPrefWidth(80); // <-- CORRIGÉ

        intersectingClassesTable.getColumns().clear();
        // AJOUT DE TCC ici
        intersectingClassesTable.getColumns().addAll(interClassNameCol, interNbMethodsCol, interNbAttributesCol, interWmcCol, interTccCol, interAtfdCol);


        // ========================================================================
        // 4. Initialisation de la TableView Filtered Classes (Filtrage X - Q1.1, #11)
        // ========================================================================
        TableColumn<ClassMetric, String> filterClassNameCol = new TableColumn<>("Classe");
        filterClassNameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        filterClassNameCol.setPrefWidth(300);

        TableColumn<ClassMetric, Integer> filterNbMethodsCol = new TableColumn<>("Nb Méthodes");
        filterNbMethodsCol.setCellValueFactory(new PropertyValueFactory<>("numberOfMethods"));
        filterNbMethodsCol.setPrefWidth(150);

        TableColumn<ClassMetric, Integer> filterWmcCol = new TableColumn<>("WMC");
        filterWmcCol.setCellValueFactory(new PropertyValueFactory<>("wmc"));
        filterWmcCol.setPrefWidth(80);

        // TCC pour ce tableau
        TableColumn<ClassMetric, Double> filterTccCol = new TableColumn<>("TCC");
        filterTccCol.setCellValueFactory(new PropertyValueFactory<>("tcc"));
        filterTccCol.setPrefWidth(80);
        filterTccCol.setCellFactory(tccCol.getCellFactory()); // Réutilise le formatage

        // Déclarez une nouvelle colonne ATFD (Integer)
        TableColumn<ClassMetric, Integer> filterAtfdCol = new TableColumn<>("ATFD");
        // CORRECTION ICI : Utilisez la variable filterAtfdCol !
        filterAtfdCol.setCellValueFactory(new PropertyValueFactory<>("atfd")); // <-- CORRIGÉ
        filterAtfdCol.setPrefWidth(80); // <-- CORRIGÉ

        filteredClassesTable.getColumns().clear();
        // AJOUT DE TCC ici
        filteredClassesTable.getColumns().addAll(filterClassNameCol, filterNbMethodsCol, filterWmcCol, filterTccCol, filterAtfdCol);
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
    // Dans MetricsController.java

    /**
     * Gère l'action du bouton "Filtrer" dans l'onglet "Filtrage par Nombre de Méthodes (X)".
     * Implémente Q1.1, #11.
     */
    @FXML
    private void handleFilterByX(ActionEvent event) {
        String xText = xValueField.getText();

        // Vérification : S'assurer que l'analyse a été effectuée avant de filtrer.
        if (currentMetrics == null || currentMetrics.isEmpty()) {
            showAlert("Erreur de pré-condition", "Veuillez analyser le code source (bouton Analyser) avant de filtrer.", Alert.AlertType.WARNING);
            return;
        }

        try {
            int xValue = Integer.parseInt(xText);

            if (xValue < 0) {
                showAlert("Erreur de valeur", "X doit être supérieur ou égal à zéro.", Alert.AlertType.WARNING);
                return;
            }

            // --- Logique Q1.1, #11 : Filtrage des classes avec plus de X méthodes ---

            // 1. Récupérer toutes les ClassMetric
            // 2. Filtrer celles dont le nombre de méthodes est strictement supérieur à X
            List<ClassMetric> filteredClasses = currentMetrics.values().stream()
                    .filter(c -> c.getNumberOfMethods() > xValue)
                    .collect(Collectors.toList());

            // 3. Mettre à jour le tableau du dernier onglet
            filteredClassesTable.setItems(FXCollections.observableArrayList(filteredClasses));

            // Mise à jour de l'interface et déplacement vers l'onglet
            showAlert("Filtrage Terminé", filteredClasses.size() + " classes trouvées avec plus de " + xValue + " méthodes.", Alert.AlertType.INFORMATION);
            resultsTabPane.getSelectionModel().select(2); // Sélectionne l'onglet "Filtrage par Nombre de Méthodes (X)"

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

    // Calcule le nombre total de méthodes (TP Q1.1, #3)
    private int calculateTotalMethods(Map<String, ClassMetric> classMetrics) {
        return classMetrics.values().stream()
                .mapToInt(ClassMetric::getNumberOfMethods)
                .sum();
    }

    // Calcule le nombre total de packages (TP Q1.1, #4)
    private int calculateTotalPackages(Map<String, ClassMetric> classMetrics) {
        // Utilise le getter getPackageName() que nous avons créé dans ClassMetric
        return (int) classMetrics.values().stream()
                .map(ClassMetric::getPackageName)
                .filter(pkg -> !pkg.isEmpty())
                .distinct() // Compte uniquement les packages uniques
                .count();
    }

    // Calcule le nombre total d'attributs (pour la moyenne)
    private int calculateTotalAttributes(Map<String, ClassMetric> classMetrics) {
        return classMetrics.values().stream()
                .mapToInt(ClassMetric::getNumberOfAttributes)
                .sum();
    }

    // Calcule le nombre maximal de paramètres (TP Q1.1, #13)
    private int calculateMaxParameters(List<MethodMetric> allMethods) {
        return allMethods.stream()
                .mapToInt(MethodMetric::getParameterCount) // Assurez-vous d'avoir getParameterCount()
                .max()
                .orElse(0);
    }





    /**
     * Logique générique pour obtenir les N% premiers éléments triés.
     */
    private <T> List<T> getTopNPercent(List<T> items, Comparator<T> comparator, double percent) {
        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        // Calculer le nombre d'éléments à retenir (arrondi au supérieur, minimum 1)
        int limit = (int) Math.ceil(items.size() * percent);

        // S'assurer qu'au moins 1 élément est retourné si la liste n'est pas vide
        limit = Math.max(1, limit); // <--- AJOUTER CETTE LIGNE

        // Créer une copie pour ne pas modifier la liste originale
        List<T> sortedItems = new ArrayList<>(items);

        // Trier la liste par ordre décroissant (reversed)
        sortedItems.sort(comparator.reversed());

        // Retourner le Top N%
        return sortedItems.subList(0, Math.min(limit, sortedItems.size()));
    }


    // Q1.1, #8 : Top 10% des classes par Nombre de Méthodes
    private List<ClassMetric> getTop10PercentByMethods(Map<String, ClassMetric> classMetrics) {
        List<ClassMetric> allClasses = new ArrayList<>(classMetrics.values());
        return getTopNPercent(allClasses, Comparator.comparing(ClassMetric::getNumberOfMethods), 0.10);
    }

    // Q1.1, #9 : Top 10% des classes par Nombre d'Attributs
    private List<ClassMetric> getTop10PercentByAttributes(Map<String, ClassMetric> classMetrics) {
        List<ClassMetric> allClasses = new ArrayList<>(classMetrics.values());
        return getTopNPercent(allClasses, Comparator.comparing(ClassMetric::getNumberOfAttributes), 0.10);
    }

    // Q1.1, #10 : Classes dans l'intersection des deux catégories précédentes
    private List<ClassMetric> getIntersection(List<ClassMetric> topMethods, List<ClassMetric> topAttributes) {
        // Utiliser le nom complet de la classe pour identifier l'intersection
        Set<String> methodClassNames = topMethods.stream()
                .map(ClassMetric::getFullName)
                .collect(Collectors.toSet());

        // Filtrer la deuxième liste pour ne garder que les éléments dont le nom est dans l'ensemble de la première
        return topAttributes.stream()
                .filter(c -> methodClassNames.contains(c.getFullName()))
                .collect(Collectors.toList());
    }

    // Q1.1, #12 : Top 10% des méthodes par LoC (pour toutes les méthodes de l'application)
    private List<MethodMetric> getTop10PercentMethodsByLoc(List<MethodMetric> allMethods) {
        return getTopNPercent(allMethods, Comparator.comparing(MethodMetric::getLoc), 0.10);
    }

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
    // Dans MetricsController.java

    /**
     * Met à jour le GridPane 'summaryGrid' avec les métriques globales du TP.
     */
    private void updateSummary(Map<String, ClassMetric> classMetrics) {
        summaryGrid.getChildren().clear(); // Nettoyer la grille avant l'ajout

        // --- Calcul des totaux ---
        int totalClasses = classMetrics.size(); // (TP Q1.1, #1)
        int totalLoc = calculateTotalAppLoc(classMetrics); // (TP Q1.1, #2)
        int totalMethods = calculateTotalMethods(classMetrics); // (TP Q1.1, #3)
        int totalPackages = calculateTotalPackages(classMetrics); // (TP Q1.1, #4)
        int totalAttributes = calculateTotalAttributes(classMetrics);

        // Nécessaire pour les métriques basées sur les méthodes
        List<MethodMetric> allMethods = getAllMethodMetrics(classMetrics);
        int maxParameters = calculateMaxParameters(allMethods); // (TP Q1.1, #13)

        // --- Calcul des moyennes ---
        double avgMethodsPerClass = (totalClasses > 0) ? (double) totalMethods / totalClasses : 0.0; // (TP Q1.1, #5)
        double avgLocPerMethod = (totalMethods > 0) ? (double) totalLoc / totalMethods : 0.0; // (TP Q1.1, #6)
        double avgAttributesPerClass = (totalClasses > 0) ? (double) totalAttributes / totalClasses : 0.0; // (TP Q1.1, #7)

        // --- Affichage dans la grille (GridPane) ---
        int row = 0;

        // Totaux
        summaryGrid.add(new Label("1. Nombre de classes analysées :"), 0, row);
        summaryGrid.add(new Label(String.valueOf(totalClasses)), 1, row++);

        summaryGrid.add(new Label("2. LoC Totale de l'application :"), 0, row);
        summaryGrid.add(new Label(String.valueOf(totalLoc)), 1, row++);

        summaryGrid.add(new Label("3. Nombre total de méthodes :"), 0, row);
        summaryGrid.add(new Label(String.valueOf(totalMethods)), 1, row++);

        summaryGrid.add(new Label("4. Nombre total de packages :"), 0, row);
        summaryGrid.add(new Label(String.valueOf(totalPackages)), 1, row++);

        row++; // Saut de ligne pour la clarté

        // Moyennes
        summaryGrid.add(new Label("5. Moy. méthodes par classe :"), 0, row);
        summaryGrid.add(new Label(String.format("%.2f", avgMethodsPerClass)), 1, row++);

        summaryGrid.add(new Label("6. Moy. LoC par méthode :"), 0, row);
        summaryGrid.add(new Label(String.format("%.2f", avgLocPerMethod)), 1, row++);

        summaryGrid.add(new Label("7. Moy. attributs par classe :"), 0, row);
        summaryGrid.add(new Label(String.format("%.2f", avgAttributesPerClass)), 1, row++);

        row++; // Saut de ligne

        // Métrique spécifique
        summaryGrid.add(new Label("13. Nombre maximal de paramètres :"), 0, row);
        summaryGrid.add(new Label(String.valueOf(maxParameters)), 1, row++);
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

            // --- Calcul des listes de classement Top 10% ---

            // Liste complète de toutes les méthodes (nécessaire pour le classement)
            List<MethodMetric> allMethods = getAllMethodMetrics(currentMetrics);

            // Q1.1, #8 : Classes Top 10% par Nombre de Méthodes
            List<ClassMetric> topMethodsClasses = getTop10PercentByMethods(currentMetrics);

            // Q1.1, #9 : Classes Top 10% par Nombre d'Attributs
            List<ClassMetric> topAttributesClasses = getTop10PercentByAttributes(currentMetrics);

            // Q1.1, #10 : Intersection
            List<ClassMetric> intersectingClasses = getIntersection(topMethodsClasses, topAttributesClasses);

            // Q1.1, #12 : Méthodes Top 10% par LoC
            List<MethodMetric> topMethodsByLoc = getTop10PercentMethodsByLoc(allMethods);

            // --------------------------------------------------------------------

            // 2. Mise à jour de la TABLEVIEW DES MÉTHODES (topMethodsTable - Gauche)
            // Affiche le classement Q1.1, #12 (Top 10% des méthodes par LoC)
            topMethodsTable.setItems(FXCollections.observableArrayList(topMethodsByLoc));

            // 2.1. NOUVEAU : Construire le Graphe d'Appel
            callGraph = new CallGraph(); // Réinitialiser
            MethodCallVisitor callVisitor = new MethodCallVisitor();

            // Exécuter le visiteur sur chaque unité de compilation
            for (CompilationUnit cu : asts) {
                callVisitor.visit(cu, callGraph);
            }


            // 3. Mise à jour de la TABLEVIEW DES CLASSES (topAttributesTable - Droite)
            // Affiche le classement Q1.1, #9 (Top 10% des classes par Nombre d'Attributs)
            // NOTE: On choisit d'afficher l'un des deux classements Top 10% de classes ici.
            topAttributesTable.setItems(FXCollections.observableArrayList(topAttributesClasses));

            // 4. Mise à jour de la TABLEVIEW D'INTERSECTION (intersectingClassesTable - Bas)
            // Affiche le résultat Q1.1, #10
            intersectingClassesTable.setItems(FXCollections.observableArrayList(intersectingClasses));

            // NOUVEAU : Mise à jour de l'affichage du Graphe d'Appel (Question 2.2)
            callGraphTextArea.setText(callGraph.toString());


            // 5. Afficher le message de succès et changer d'onglet
            showAlert("Succès", "Analyse statique terminée ! " + currentMetrics.size() + " classes analysées. Résultats mis à jour.", Alert.AlertType.INFORMATION);

            // Sélectionne l'onglet "Synthèse & Totaux"
            resultsTabPane.getSelectionModel().select(0);

        } else {
            showAlert("Échec", "Échec de l'analyse. Vérifiez le chemin.", Alert.AlertType.ERROR);
        }
    }


}