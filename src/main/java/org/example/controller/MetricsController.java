package org.example.controller;

// MetricsController.java (Le Cerveau de l'interface)
import javafx.scene.Group;
import org.example.analysis.SourceCodeAnalyzer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.concurrent.Task;

import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import org.example.analysis.MetricsCollector;
import org.example.model.ClassMetric;
import com.github.javaparser.ast.CompilationUnit;

// Dans MetricsController.java (Section Imports)
import java.util.*;
import java.util.stream.Collectors; // Ajouté pour Collectors
import java.io.File;
// ...

import javafx.collections.FXCollections;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;

import org.example.model.MethodMetric;

// Imports GraphStream
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxDefaultView;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.fx_viewer.FxViewPanel;
// Dans la section des imports de MetricsController.java


import org.example.analysis.CouplingCalculator;
import org.example.model.ClassCouplingGraph;


import org.example.model.CallGraph;
import org.example.analysis.MethodCallVisitor;


import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import java.util.Map;




public class MetricsController {

    // ========================================================================
    // CHAMPS FXML (Correspondant aux fx:id dans MetricsView.fxml)
    // ========================================================================

    @FXML
    private TextField sourcePathField;
    @FXML
    private GridPane summaryGrid;
    @FXML
    private TabPane resultsTabPane;

    // Tableaux pour l'onglet "Classes et Méthodes Critiques"
    @FXML
    private TableView<MethodMetric> topMethodsTable;
    @FXML
    private TableView<ClassMetric> topAttributesTable; // Ajouté (pour topAttributesTable)
    @FXML
    private TableView<ClassMetric> intersectingClassesTable; // Ajouté (pour intersectingClassesTable)

    // Éléments pour l'onglet "Filtrage par Nombre de Méthodes (X)"
    @FXML
    private TextField xValueField;
    @FXML
    private TableView<ClassMetric> filteredClassesTable; // Ajouté (pour filteredClassesTable)

    // NOUVEAUX ÉLÉMENTS pour l'affichage du Graphe d'Appel
    @FXML
    private Tab callGraphTab; // Le nouvel onglet
    @FXML
    private TextArea callGraphTextArea; // Le nouveau composant d'affichage

    // NOUVEAU pour la visualisation GraphStream
    @FXML
    private Tab callGraphVisuelTab; // L'onglet lui-même
    @FXML
    private AnchorPane graphPane;

    @FXML
    private AnchorPane couplingGraphPane; // Le nouveau conteneur

    @FXML private Slider couplingSlider; // Nouveau
    @FXML private Label couplingThresholdLabel; // Nouveau

    private ClassCouplingGraph currentCouplingGraph;
    private final CouplingCalculator couplingCalculator = new CouplingCalculator(); // Le calculateur


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

        TableColumn<ClassMetric, Double> interTccCol = new TableColumn<>("TCC");
        interTccCol.setCellValueFactory(new PropertyValueFactory<>("tcc"));
        interTccCol.setPrefWidth(80);
        interTccCol.setCellFactory(tccCol.getCellFactory()); // Réutilise le formatage

        // Déclarez une nouvelle colonne ATFD (Integer)
        TableColumn<ClassMetric, Integer> interAtfdCol = new TableColumn<>("ATFD");

        interAtfdCol.setCellValueFactory(new PropertyValueFactory<>("atfd"));
        interAtfdCol.setPrefWidth(80);

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

        // Dans MetricsController.java, à la fin de la méthode initialize()

// --- Initialisation du Slider de couplage ---
        couplingSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double threshold = newVal.doubleValue();
            couplingThresholdLabel.setText(String.format("%.2f", threshold));
            // Si un graphe a déjà été calculé, on le redessine avec le nouveau seuil
            if (currentCouplingGraph != null) {
                displayCouplingGraph(currentCouplingGraph);
            }
        });

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

    // Dans MetricsController.java

    @FXML
    private void handleAnalyze(ActionEvent event) {
        String path = sourcePathField.getText();

        if (path == null || path.trim().isEmpty()) {
            showAlert("Erreur", "Veuillez spécifier un chemin source.", Alert.AlertType.ERROR);
            return;
        }

        // Affiche une boîte de dialogue pour faire patienter l'utilisateur
        Alert waitAlert = new Alert(Alert.AlertType.INFORMATION);
        waitAlert.setTitle("Analyse en cours");
        waitAlert.setHeaderText("L'analyse du code source a commencé...");
        waitAlert.setContentText("Cette opération peut prendre quelques instants. Veuillez patienter.");
        waitAlert.show();

        // Crée une tâche d'arrière-plan pour ne pas bloquer l'interface
        Task<Boolean> analysisTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                // --- DÉBUT DU TRAVAIL EN ARRIÈRE-PLAN ---

                // 1. Lancer l'analyse (Lecture et Parsing des fichiers)
                boolean success = analyzer.analyze(path);
                if (!success) {
                    return false; // L'analyse a échoué
                }

                // 2. Collecter et Calculer les métriques de classe
                List<CompilationUnit> asts = analyzer.getCompilationUnits();
                currentMetrics = collector.calculateMetrics(asts);

                // 3. Construire le Graphe d'Appel
                callGraph = new CallGraph();
                MethodCallVisitor callVisitor = new MethodCallVisitor();
                for (CompilationUnit cu : asts) {
                    callVisitor.visit(cu, callGraph);
                }

                return true; // L'analyse a réussi
            }
        };

        // --- GESTION DE LA FIN DE LA TÂCHE (S'exécute sur le thread de l'UI) ---

        // Lorsque la tâche réussit
        analysisTask.setOnSucceeded(e -> {
            waitAlert.close(); // Ferme la boîte d'attente

            // ====================================================================
            // MISE À JOUR DE TOUTE L'INTERFACE UTILISATEUR
            // ====================================================================

            // 1. Mise à jour de la SYNTHÈSE (Onglet 1)
            updateSummary(currentMetrics);

            // 2. Calcul et mise à jour des classements Top 10%
            List<MethodMetric> allMethods = getAllMethodMetrics(currentMetrics);
            List<MethodMetric> topMethodsByLoc = getTop10PercentMethodsByLoc(allMethods);
            topMethodsTable.setItems(FXCollections.observableArrayList(topMethodsByLoc));

            List<ClassMetric> topMethodsClasses = getTop10PercentByMethods(currentMetrics);
            List<ClassMetric> topAttributesClasses = getTop10PercentByAttributes(currentMetrics);
            topAttributesTable.setItems(FXCollections.observableArrayList(topAttributesClasses));

            List<ClassMetric> intersectingClasses = getIntersection(topMethodsClasses, topAttributesClasses);
            intersectingClassesTable.setItems(FXCollections.observableArrayList(intersectingClasses));

            // 3. Mise à jour de l'affichage du Graphe d'Appel (Texte et Visuel)
            if (callGraphTextArea != null) {
                callGraphTextArea.setText(callGraph.toString());
            }
            if (graphPane != null) {
                displayCallGraph(callGraph);
            }

            ClassCouplingGraph couplingGraph = couplingCalculator.calculate(callGraph);


            // NOUVEAU : Calculer et stocker le graphe de couplage
            currentCouplingGraph = couplingCalculator.calculate(callGraph);
            // Afficher le graphe initialement avec un seuil de 0
            displayCouplingGraph(currentCouplingGraph);

            // 4. Afficher le message de succès final
            showAlert("Succès", "Analyse statique terminée ! " + currentMetrics.size() + " classes analysées.", Alert.AlertType.INFORMATION);

            // Sélectionne le premier onglet pour montrer les résultats de synthèse
            resultsTabPane.getSelectionModel().select(0);
        });

        // Lorsque la tâche échoue
        analysisTask.setOnFailed(e -> {
            waitAlert.close();
            showAlert("Erreur", "Une erreur inattendue est survenue pendant l'analyse.", Alert.AlertType.ERROR);
            analysisTask.getException().printStackTrace(); // Affiche l'erreur dans la console pour le débogage
        });

        // Démarrer la tâche dans un nouveau thread
        new Thread(analysisTask).start();
    }

    // Dans MetricsController.java

    /**
     * Construit un graphe GraphStream à partir de l'objet CallGraph et l'affiche dans le Pane JavaFX.
     * NÉCESSITE les dépendances GraphStream (gs-core et gs-ui-fx).
     */


    // Dans MetricsController.java
    private void displayCallGraph(CallGraph callGraph) {
        System.out.println("--- DÉBUT DU DÉBOGAGE : displayCallGraph ---");

        // VÉRIFICATION DE SÉCURITÉ : Ne pas afficher un graphe trop grand
        if (callGraph.getGraph().size() > 500) { // Limite de 500 nœuds, ajustez si besoin
            System.out.println("Graphe d'appel trop grand (" + callGraph.getGraph().size() + " nœuds) pour être affiché.");
            graphPane.getChildren().add(new Label("Le graphe d'appel est trop grand pour être affiché visuellement."));
            return;
        }

        // ----- VÉRIFICATION 1 : Le conteneur FXML est-il bien lié ? -----
        if (graphPane == null) {
            System.err.println("ERREUR CRITIQUE : La variable 'graphPane' est null. Vérifiez que votre AnchorPane a bien l'attribut fx:id=\"graphPane\" dans le fichier FXML.");
            return;
        }
        System.out.println("1. Le conteneur 'graphPane' est correctement lié. OK.");

        // ----- VÉRIFICATION 2 : Le graphe d'appel contient-il des données ? -----
        System.out.println("2. Le graphe d'appel contient " + callGraph.getGraph().size() + " méthodes appelantes.");
        if (callGraph.getGraph().isEmpty()) {
            System.out.println("   Le graphe est vide. Rien à afficher.");
            graphPane.getChildren().clear();
            graphPane.getChildren().add(new Label("Aucune relation d'appel n'a été trouvée."));
            return;
        }

        try {
            // 0. Configuration essentielle pour le rendu JavaFX de GraphStream
            System.setProperty("org.graphstream.ui", "javafx"); // Updated property for JavaFX rendering

            // 1. Nettoyer le conteneur et vérifier si le graphe est vide
            graphPane.getChildren().clear();
            if (callGraph.getGraph().isEmpty()) {
                graphPane.getChildren().add(new Label("Aucune relation d'appel trouvée."));
                return;
            }

            // 2. Créer l'objet GraphStream
            Graph graph = new SingleGraph("CallGraph");
            graph.setAttribute("ui.stylesheet", "node { fill-color: #ADD8E6; text-size: 10; } edge { arrow-shape: arrow; }");

            // 3. Remplir le graphe avec les données
            for (Map.Entry<String, Set<String>> entry : callGraph.getGraph().entrySet()) {
                String caller = entry.getKey();
                String callerId = caller.replaceAll("[^a-zA-Z0-9_]", "_");
                if (graph.getNode(callerId) == null) {
                    graph.addNode(callerId).setAttribute("ui.label", caller.substring(caller.lastIndexOf('.') + 1));
                }
                for (String callee : entry.getValue()) {
                    String calleeId = callee.replaceAll("[^a-zA-Z0-9_]", "_");
                    if (graph.getNode(calleeId) == null) {
                        graph.addNode(calleeId).setAttribute("ui.label", callee.substring(callee.lastIndexOf('.') + 1));
                    }
                    String edgeId = callerId + "_to_" + calleeId;
                    if (graph.getEdge(edgeId) == null) {
                        graph.addEdge(edgeId, callerId, calleeId, true);
                    }
                }
            }

            // 4. Créer et intégrer l'afficheur JavaFX
            Viewer viewer = new FxViewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
            viewer.enableAutoLayout();

            // Use addDefaultView to create a JavaFX-compatible view
            FxViewPanel viewPanel = (FxViewPanel) viewer.addDefaultView(false); // false to avoid opening a new window

            // Lier la taille du panneau à celle du conteneur
            viewPanel.prefWidthProperty().bind(graphPane.widthProperty());
            viewPanel.prefHeightProperty().bind(graphPane.heightProperty());

            // Ajouter le panneau à la scène JavaFX
            graphPane.getChildren().add(viewPanel);

        } catch (Exception e) {
            System.err.println("ERREUR INATTENDUE pendant la création du graphe visuel :");
            e.printStackTrace(); // Affiche l'erreur complète dans la console
        }
    }


    // Dans MetricsController.java

    // Dans MetricsController.java

    // Dans MetricsController.java

    private void displayCouplingGraph(ClassCouplingGraph couplingGraph) {
        // 0. Configurer le moteur de rendu JavaFX
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.fx_viewer.FxViewer");
        couplingGraphPane.getChildren().clear();

        if (couplingGraph.getGraph().isEmpty()) {
            couplingGraphPane.getChildren().add(new Label("Aucun couplage inter-classe trouvé."));
            return;
        }

        // 2. Créer le graphe GraphStream
        Graph graph = new SingleGraph("CouplingGraph");
        graph.setAttribute("ui.stylesheet", "node { size: 15px; fill-color: #F7D794; text-size: 12; } " +
                "edge { text-size: 10; fill-color: #777; arrow-shape: arrow; }");

        // 3. Remplir le graphe (votre logique est correcte)
        for (Map.Entry<String, Map<String, Double>> entry : couplingGraph.getGraph().entrySet()) {
            String sourceClass = entry.getKey();
            if (graph.getNode(sourceClass) == null) {
                graph.addNode(sourceClass).setAttribute("ui.label", sourceClass.substring(sourceClass.lastIndexOf('.') + 1));
            }
            for (Map.Entry<String, Double> targetEntry : entry.getValue().entrySet()) {
                String targetClass = targetEntry.getKey();
                double weight = targetEntry.getValue();
                if (graph.getNode(targetClass) == null) {
                    graph.addNode(targetClass).setAttribute("ui.label", targetClass.substring(targetClass.lastIndexOf('.') + 1));
                }
                String edgeId = sourceClass + "->" + targetClass;
                if (graph.getEdge(edgeId) == null) {
                    graph.addEdge(edgeId, sourceClass, targetClass, true)
                            .setAttribute("ui.label", String.format("%.3f", weight));
                }
            }
        }

        // 4. Créer le visualiseur
        Viewer viewer = new FxViewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        viewer.enableAutoLayout();

        // 5. Obtenir le panneau d'affichage (LA CORRECTION FINALE EST ICI)
        // On caste directement le résultat de viewer.addView en FxViewPanel.
        FxViewPanel viewPanel = (FxViewPanel) viewer.addDefaultView(false);

        // 6. Lier la taille et ajouter le panneau à l'interface
        viewPanel.prefWidthProperty().bind(couplingGraphPane.widthProperty());
        viewPanel.prefHeightProperty().bind(couplingGraphPane.heightProperty());
        couplingGraphPane.getChildren().add(viewPanel);
    }
}
