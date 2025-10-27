package org.example.controller;

import org.example.model.Cluster;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

// Imports JavaFX nécessaires (à ajouter en haut de votre fichier)
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.TreeItem;

// IMPORTS JAVA UTILES
import java.awt.*;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.example.analysis.*;
import org.example.model.*;


// MetricsController.java (Le Cerveau de l'interface)
import javafx.scene.layout.Pane;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.concurrent.Task;

import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

// Dans MetricsController.java (Section Imports)
import java.util.*;
import java.util.stream.Collectors; // Ajouté pour Collectors
import java.io.File;
// ...

import javafx.collections.FXCollections;
import javafx.scene.control.cell.PropertyValueFactory;

// Imports GraphStream
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.fx_viewer.FxViewPanel;
// Dans la section des imports de MetricsController.java


import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import javafx.scene.control.Alert;


public class MetricsController {

    @FXML
    public RadioButton javaParserRadio;
    @FXML
    public RadioButton spoonRadio;
    // AJOUTEZ CETTE LIGNE
    @FXML
    private ToggleGroup analysisToggleGroup;

    /**
     * Met à jour tous les onglets liés au graphe d'appel (visuel, matrice,
     * couplage, modules, dendrogramme) en utilisant le CallGraph sélectionné
     * (JavaParser ou Spoon) via les boutons radio.
     */
    private void updateGraphViews() {
        System.out.println("Mise à jour des vues de graphe...");

        // 1. Déterminer quel CallGraph utiliser
        CallGraph selectedGraph = null;
        if (spoonRadio != null && spoonRadio.isSelected() && spoonCallGraph != null) {
            System.out.println("Utilisation du graphe Spoon.");
            selectedGraph = spoonCallGraph;
        } else if (javaParserRadio != null && javaParserRadio.isSelected() && javaParserCallGraph != null) {
            System.out.println("Utilisation du graphe JavaParser.");
            selectedGraph = javaParserCallGraph;
        } else {
            // Cas où l'analyse n'a pas encore été faite ou a échoué
            System.out.println("Aucun graphe sélectionné ou disponible.");
            // Optionnel : Vider tous les panneaux d'affichage
            if (graphPane != null) graphPane.getChildren().clear();
            if (matrixPane != null) matrixPane.getChildren().clear();
            if (couplingGraphPane != null) couplingGraphPane.getChildren().clear();
            if (modulesTreeView != null) modulesTreeView.setRoot(null);
            if (dendrogramPane != null) dendrogramPane.getChildren().clear();
            // Vous pourriez ajouter des Labels "Aucune donnée" ici
            return;
        }

        // 2. Mettre à jour les affichages en utilisant le graphe sélectionné

        // Onglet Graphe d'Appel Visuel (GraphStream/Graphviz)
        if (graphPane != null) {
            displayCallGraph(selectedGraph); // Votre méthode existante
        }

        // Onglet Matrice d'Adjacence
        if (matrixPane != null) {
            displayAdjacencyMatrix(selectedGraph); // Votre méthode existante
        }

        // Calculs et affichages dépendants (Couplage, Modules, Dendrogramme)
        try {
            // Recalculer le couplage basé sur le graphe sélectionné
            ClassCouplingGraph selectedCouplingGraph = couplingCalculator.calculate(selectedGraph);

            // === STOCKER LE RÉSULTAT CALCUÉ ICI ===
            this.currentCouplingGraph = selectedCouplingGraph;
            // ======================================

            // Onglet Graphe de Couplage
            if (couplingGraphPane != null) {
                // Passe le résultat fraîchement calculé (et stocké)
                displayCouplingGraph(this.currentCouplingGraph);
            }

            // Onglet Modules et Dendrogramme (utilise le résultat fraîchement calculé)
            if (this.currentCouplingGraph != null && !this.currentCouplingGraph.getGraph().isEmpty()) {
                System.out.println("DEBUG (updateGraphViews): Graphe de couplage trouvé (" + this.currentCouplingGraph.getAllClasses().size() + " classes). Création du ModuleFinder..."); // Garde le debug

                ModuleFinder selectedModuleFinder = new ModuleFinder(this.currentCouplingGraph);
                Cluster root = selectedModuleFinder.buildDendrogram(); // Construit l'arbre
                if (root != null) {
                    System.out.println("DEBUG (updateGraphViews): Dendrogramme construit. Racine: " + root);
                } else {
                    System.err.println("ERREUR (updateGraphViews): buildDendrogram a retourné null !");
                }


                // Onglet Modules (utilise le slider CP actuel)
                if (modulesTreeView != null && cpSlider != null) {
                    // REMPLACER LE COMMENTAIRE PAR CECI :
                    List<Cluster> modules = selectedModuleFinder.findModules(cpSlider.getValue());
                    displayModulesInTree(modules, cpSlider.getValue());
                }

                // Onglet Dendrogramme
                if (dendrogramPane != null) {
                    System.out.println("DEBUG (updateGraphViews): Appel de displayDendrogram..."); // Garde le debug

                    displayDendrogram(selectedModuleFinder);
                }
            } else {
                System.err.println("ERREUR (updateGraphViews): this.currentCouplingGraph est null ou vide. Impossible de calculer modules/dendrogramme."); // Garde le debug
                // Vider les modules et dendrogramme si pas de couplage
                if (modulesTreeView != null) modulesTreeView.setRoot(null);
                if (dendrogramPane != null) dendrogramPane.getChildren().clear();
            }

        } catch (Exception e) {
            // ... (Gestion erreur) ...
            this.currentCouplingGraph = null; // S'assurer qu'il est null en cas d'erreur
        }
    }

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

    // NOUVEAUX CHAMPS À AJOUTER POUR L'ONGLET HYBRIDE
    @FXML
    private BorderPane detailedGraphLayoutPane; // Le conteneur BorderPane

    @FXML
    private TreeView<String> classTreeView; // L'arborescence à gauche

    @FXML
    private AnchorPane detailedGraphPane; // Le panneau d'affichage à droite

    // NOUVEAUX CHAMPS À AJOUTER POUR L'ONGLET MODULES
    @FXML
    private Slider cpSlider;

    @FXML
    private Label cpLabel;

    @FXML
    private Button findModulesButton;

    @FXML
    private TreeView<String> modulesTreeView;

    @FXML
    private AnchorPane dendrogramPane;


    @FXML
    private AnchorPane couplingGraphPane; // Le nouveau conteneur

    @FXML private Slider couplingSlider; // Nouveau
    @FXML private Label couplingThresholdLabel; // Nouveau

    @FXML
    private Button updateCouplingGraphButton;

    @FXML private Button zoomInButton; // Nouveau
    @FXML private Button zoomOutButton; // Nouveau

    // Lier le bouton de l'interface
    @FXML
    private Button genererGrapheBouton;

    // 1. AJOUTER CE NOUVEAU CHAMP FXML
    @FXML
    private AnchorPane matrixPane; // Panneau pour l'onglet 2

    private CallGraph javaParserCallGraph;
    private CallGraph spoonCallGraph;



    private View graphStreamView;

    // Dans MetricsController.java

    @FXML
    private void handleZoomIn(ActionEvent event) {
        if (graphStreamView != null) {
            // La méthodegetCamera() retourne l'objet qui contrôle la vue
            // setViewPercent() ajuste le zoom (valeur < 1.0 = zoom avant)
            graphStreamView.getCamera().setViewPercent(
                    Math.max(0.1, graphStreamView.getCamera().getViewPercent() * 0.8) // Zoom in (80% of current view)
            );
        }
    }

    @FXML
    private void handleZoomOut(ActionEvent event) {
        if (graphStreamView != null) {
            // Valeur > 1.0 = zoom arrière
            graphStreamView.getCamera().setViewPercent(
                    Math.min(1.0, graphStreamView.getCamera().getViewPercent() * 1.2) // Zoom out (120% of current view)
            );
        }
    }

    @FXML
    private void handleUpdateCouplingGraph(ActionEvent event) {
        System.out.println("DEBUG: Clic sur Actualiser Graphe Couplage."); // Debug
        // Vérifie si l'analyse a déjà eu lieu (si currentCouplingGraph existe)
        if (currentCouplingGraph != null) {
            // Appelle simplement la méthode d'affichage existante
            // Elle lira la valeur actuelle du slider
            displayCouplingGraph(currentCouplingGraph);
        } else {
            System.out.println("DEBUG: currentCouplingGraph est null, rien à actualiser.");
            // Optionnel : Afficher un message si l'analyse n'est pas faite ?
            if (couplingGraphPane != null) {
                couplingGraphPane.getChildren().clear();
                couplingGraphPane.getChildren().add(new Label("Veuillez d'abord lancer l'analyse."));
            }
        }
    }

    private ClassCouplingGraph currentCouplingGraph;
    private final CouplingCalculator couplingCalculator = new CouplingCalculator(); // Le calculateur


    private SourceCodeAnalyzer analyzer = new SourceCodeAnalyzer();
    private MetricsCollector collector = new MetricsCollector(); // <-- Initialisation
    private Map<String, ClassMetric> currentMetrics; // Pour stocker les résultats

    private CallGraph callGraph = new CallGraph();

    private ModuleFinder moduleFinder;

    // ==========================================================
    // PLACEZ LA CLASSE INTERNE STATIC ICI
    // ==========================================================
    public static class AnalysisResult {
        public final CallGraph javaParserGraph;
        public final CallGraph spoonGraph;
        public final Map<String, ClassMetric> metrics;
        public final boolean jpSuccess;
        public final boolean spoonSuccess;

        public AnalysisResult(CallGraph jpGraph, CallGraph spGraph, Map<String, ClassMetric> metrics, boolean jpOk, boolean spOk) {
            this.javaParserGraph = jpGraph;
            this.spoonGraph = spGraph;
            this.metrics = metrics;
            this.jpSuccess = jpOk;
            this.spoonSuccess = spOk;
        }
    }

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


// --- Initialisation du Slider de COUPLAGE (Ancien onglet) ---
        if (couplingSlider != null && couplingThresholdLabel != null) { // Ajout de vérifications null
            couplingSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                double threshold = newVal.doubleValue();
                couplingThresholdLabel.setText(String.format("%.2f", threshold));
                // Si un graphe a déjà été calculé, on le redessine avec le nouveau seuil
               // if (currentCouplingGraph != null) {
                    // Optionnel : redessiner le graphe de couplage si vous voulez qu'il réagisse au slider
              //       displayCouplingGraph(currentCouplingGraph);
             //   }
            });
        } // Fin du listener pour couplingSlider


        // --- Initialisation du nouvel onglet MODULES (Exercice 2) ---
        // CE BLOC EST MAINTENANT À L'EXTÉRIEUR ET AU BON ENDROIT
        if (cpSlider != null && cpLabel != null) { // Ajout de vérifications null
            // Lie le label à la valeur du slider (ex: "0.05")
            cpLabel.textProperty().bind(cpSlider.valueProperty().asString("%.2f"));
        }

        if (findModulesButton != null) { // Ajout de vérification null
            // Lie le clic du bouton à une nouvelle méthode
            findModulesButton.setOnAction(this::handleFindModules);
        } // Fin de l'initialisation des modules


        // --- Initialisation de l'arborescence CLASSE (Explorateur) ---
        if (classTreeView != null) {
            classTreeView.getSelectionModel().selectedItemProperty().addListener(
                    (observable, oldValue, newValue) -> {
                        handleClassSelection(newValue);
                    }
            );
        } // Fin de l'initialisation de classTreeView

        // Dans initialize()

// --- Initialisation du Switch JavaParser/Spoon ---
        if (analysisToggleGroup != null) {
            analysisToggleGroup.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> {
                // Si la sélection change (et n'est pas nulle), on met à jour les vues
                if (newToggle != null) {
                    System.out.println("Switch sélectionné : " + ((RadioButton)newToggle).getText()); // Debug

                    // >> VÉRIFIEZ QUE CET APPEL EST BIEN LÀ <<
                    updateGraphViews();
                }
            });
        }

    } // Fin de la méthode initialize()

    /**
     * Appelé lorsqu'un utilisateur clique sur un élément de l'arborescence.
     */
    // Dans MetricsController.java

    /**
     * Appelé lorsqu'un utilisateur clique sur un élément de l'arborescence.
     */
    /**
     * Appelé lorsqu'un utilisateur clique sur un élément de l'arborescence.
     * Filtre et affiche le graphe détaillé en utilisant le CallGraph
     * actuellement sélectionné (JavaParser ou Spoon).
     */
    private void handleClassSelection(TreeItem<String> selectedItem) {
        // 1. Vérifier si c'est bien une classe sélectionnée (et non un package ou la racine)
        if (selectedItem == null ||
                !selectedItem.isLeaf() || // isLeaf() vérifie si c'est une "feuille" (pas un package)
                selectedItem.getParent() == null ||
                selectedItem.getParent().getParent() == null) { // Vérifie que le parent n'est pas la racine invisible

            if (detailedGraphPane != null) detailedGraphPane.getChildren().clear();
            return;
        }

        // --- C'est bien une classe ---

        // 2. Déterminer quel graphe source utiliser (selon les boutons radio)
        CallGraph sourceGraph = null;
        String graphSourceName = ""; // Pour le message de log
        if (spoonRadio != null && spoonRadio.isSelected() && spoonCallGraph != null) {
            sourceGraph = spoonCallGraph;
            graphSourceName = "Spoon";
        } else if (javaParserRadio != null && javaParserRadio.isSelected() && javaParserCallGraph != null) {
            sourceGraph = javaParserCallGraph;
            graphSourceName = "JavaParser";
        }

        // Si aucun graphe source n'est disponible (analyse non faite ou échouée pour la source choisie)
        if (sourceGraph == null) {
            if (detailedGraphPane != null) {
                detailedGraphPane.getChildren().clear();
                // Affiche un message plus précis
                detailedGraphPane.getChildren().add(new Label("Graphe source (" + (spoonRadio.isSelected() ? "Spoon" : "JavaParser") + ") non disponible."));
            }
            System.err.println("handleClassSelection: Graphe source (" + (spoonRadio.isSelected() ? "Spoon" : "JavaParser") + ") est null.");
            return;
        }

        // 3. Reconstruire le nom complet de la classe sélectionnée
        String className = selectedItem.getValue();
        String packageName = selectedItem.getParent().getValue();
        String fullClassName;
        if (packageName.equals("(Default Package)")) {
            fullClassName = className;
        } else {
            fullClassName = packageName + "." + className;
        }

        System.out.println("Génération du graphe détaillé pour : " + fullClassName + " (Source: " + graphSourceName + ")");

        // 4. Filtrer le graphe source sélectionné
        // CORRECTION ICI : Utilise 'sourceGraph' au lieu de 'this.callGraph'
        CallGraph filteredGraph = filterGraphForClass(fullClassName, sourceGraph);

        // 5. Afficher ce nouveau graphe
        displayDetailedGraph(filteredGraph);
    }

    /**
     * Crée un nouveau CallGraph contenant uniquement les appels
     * entrants et sortants de la classe sélectionnée.
     */
    private CallGraph filterGraphForClass(String className, CallGraph fullGraph) {
        CallGraph filtered = new CallGraph();

        for (Map.Entry<String, Set<String>> entry : fullGraph.getGraph().entrySet()) {
            String caller = entry.getKey();

            // 1. Appels SORTANTS (Le caller EST la classe sélectionnée)
            if (caller.startsWith(className)) {
                for (String callee : entry.getValue()) {
                    filtered.addCall(caller, callee);
                }
            }
            // 2. Appels ENTRANTS (Le caller N'EST PAS la classe, mais un callee l'est)
            else {
                for (String callee : entry.getValue()) {
                    if (callee.startsWith(className)) {
                        filtered.addCall(caller, callee);
                    }
                }
            }
        }
        return filtered;
    }

// Imports nécessaires (à vérifier en haut du fichier)

    /**
     * Traduit l'objet CallGraph en un String au format DOT pour Graphviz.
     */
    private String generateDotString(CallGraph callGraph) {
        StringBuilder dot = new StringBuilder();

        // 1. Début du graphe
        dot.append("digraph CallGraph {\n");

        // 2. Réglages globaux (pour un look "carré" et de haut en bas)
        dot.append("  rankdir=TB;\n"); // TB = Top-to-Bottom
        dot.append("  node [shape=box, fontname=\"Arial\", style=filled, fillcolor=\"#ADD8E6\"];\n");
        dot.append("  edge [arrowhead=normal];\n");
        dot.append("\n");

        // 3. Parcourir les données et créer les lignes de la recette
        for (Map.Entry<String, Set<String>> entry : callGraph.getGraph().entrySet()) {

            // Nettoyer le nom du Caller (enlève les parenthèses)
            String caller = entry.getKey().replaceAll("\\(.*\\)", "");

            for (String callee : entry.getValue()) {
                // Nettoyer le nom du Callee
                String calleeClean = callee.replaceAll("\\(.*\\)", "");

                // Écrire la relation : "Caller" -> "Callee";
                dot.append(String.format("  \"%s\" -> \"%s\";\n", caller, calleeClean));
            }
        }

        // 4. Fin du graphe
        dot.append("}\n");

        return dot.toString();
    }


    // Imports nécessaires (à vérifier en haut du fichier)

    /**
     * Prend le String DOT, le sauvegarde en fichier, et exécute Graphviz
     * pour générer une image.
     */
    private File generateGraphImage(String dotString) throws IOException, InterruptedException {

        // 1. Sauvegarder le String dans un fichier temporaire "graph.dot"
        // (Ce fichier sera créé à la racine de votre projet)
        File dotFile = new File("graph.dot");
        try (FileWriter writer = new FileWriter(dotFile)) {
            writer.write(dotString);
        }

        File pngFile = new File("graph.png");

        // 2. Préparer la commande à exécuter (dot -Tpng graph.dot -o graph.png)
        ProcessBuilder pb = new ProcessBuilder(
                "dot",        // La commande (Graphviz doit être dans le PATH)
                "-Tpng",      // Format de sortie PNG
                dotFile.getAbsolutePath(), // Fichier d'entrée
                "-o",         // Fichier de sortie
                pngFile.getAbsolutePath()  // Nom du fichier de sortie
        );

        // 3. Lancer la commande et attendre qu'elle finisse
        System.out.println("Lancement de la commande Graphviz pour le graphe détaillé...");
        Process process = pb.start();
        int exitCode = process.waitFor(); // Attend la fin

        if (exitCode == 0) {
            System.out.println("Image graph.png générée avec succès.");
            return pngFile;
        } else {
            System.err.println("Erreur durant la génération Graphviz. Code de sortie : " + exitCode);
            // Lire l'erreur pour le débogage
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.err.println(line);
                }
            }
            return null;
        }
    }
    /**
     * Affiche un CallGraph dans le panneau "detailedGraphPane".
     * C'est une copie de votre logique Graphviz, mais qui cible un autre panneau.
     */
    private void displayDetailedGraph(CallGraph callGraph) {
        // Vise le bon panneau
        if (detailedGraphPane == null) return;
        detailedGraphPane.getChildren().clear();

        if (callGraph == null || callGraph.getGraph().isEmpty()) {
            detailedGraphPane.getChildren().add(new Label("Aucun appel trouvé pour cette classe."));
            return;
        }

        try {
            // 1. Générer le texte DOT (réutilise la méthode helper existante)
            String dotDefinition = generateDotString(callGraph);

            // 2. Exécuter Graphviz (réutilise la méthode helper existante)
            // Note: On peut écraser le fichier "graph.png", ou utiliser un nom unique
            File imageFile = generateGraphImage(dotDefinition);

            if (imageFile != null && imageFile.exists()) {
                // 3. Charger et afficher l'image
                Image image = new Image(new FileInputStream(imageFile)); // FileInputStream évite le cache
                ImageView imageView = new ImageView(image);
                imageView.setPreserveRatio(true);

                ScrollPane scrollPane = new ScrollPane(imageView);
                scrollPane.setFitToWidth(true);
                scrollPane.setFitToHeight(true);
                scrollPane.prefWidthProperty().bind(detailedGraphPane.widthProperty());
                scrollPane.prefHeightProperty().bind(detailedGraphPane.heightProperty());

                detailedGraphPane.getChildren().add(scrollPane);
            }
        } catch (Exception e) {
            e.printStackTrace();
            detailedGraphPane.getChildren().add(new Label("Erreur de génération du graphe."));
        }
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
// MODIFICATION 1: La Task retourne maintenant AnalysisResult
        Task<AnalysisResult> analysisTask = new Task<AnalysisResult>() {
            @Override
            // MODIFICATION 2: La méthode call() retourne AnalysisResult
            protected AnalysisResult call() throws Exception {
                // --- DÉBUT DU TRAVAIL EN ARRIÈRE-PLAN ---
                System.out.println("Lancement des analyses JavaParser et Spoon...");

                // Variables pour stocker les résultats
                CallGraph jpGraph = null;
                CallGraph spGraph = null;
                Map<String, ClassMetric> calculatedMetrics = null;
                boolean jpOk = false;
                boolean spoonOk = false;

                // --- MODIFICATION 3: Analyse JavaParser ---
                try {
                    System.out.println("Analyse JavaParser en cours...");
                    // 'analyzer' et 'collector' sont vos champs existants
                    jpGraph = analyzer.generateCallGraph(path); // Appelle la méthode qui fait parse+visit

                    // Calcule les métriques SI l'analyse JP a fonctionné
                    if (jpGraph != null && !analyzer.getCompilationUnits().isEmpty()) {
                        calculatedMetrics = collector.calculateMetrics(analyzer.getCompilationUnits());
                        jpOk = true; // JavaParser a réussi
                        System.out.println("Analyse JavaParser terminée.");
                    } else {
                        System.err.println("L'analyse JavaParser n'a pas produit de graphe ou de métriques.");
                        // jpOk reste false
                    }
                } catch (Exception e) {
                    System.err.println("ERREUR durant l'analyse JavaParser:");
                    e.printStackTrace();
                    // jpOk reste false
                }


                // --- MODIFICATION 4: Analyse Spoon ---
                try {
                    System.out.println("Analyse Spoon en cours...");
                    SpoonAnalyzer spoonAnalyzer = new SpoonAnalyzer(); // Crée l'analyseur Spoon
                    spGraph = spoonAnalyzer.analyzeProject(path); // Appelle la méthode d'analyse Spoon

                    // Vérifie si Spoon a retourné un graphe non vide
                    if (spGraph != null && !spGraph.getGraph().isEmpty()) {
                        spoonOk = true; // Spoon a réussi
                        System.out.println("Analyse Spoon terminée.");
                    } else {
                        System.err.println("L'analyse Spoon n'a pas produit de graphe.");
                        // spoonOk reste false
                    }
                } catch (Exception e) {
                    System.err.println("ERREUR durant l'analyse Spoon:");
                    e.printStackTrace();
                    // spoonOk reste false
                }

                // --- MODIFICATION 5: Retourner l'objet AnalysisResult ---
                return new AnalysisResult(jpGraph, spGraph, calculatedMetrics, jpOk, spoonOk);
            }
        }; // Fin de la Task

        // --- GESTION DE LA FIN DE LA TÂCHE (S'exécute sur le thread de l'UI) ---

        // Lorsque la tâche réussit
        analysisTask.setOnSucceeded(e -> {
            waitAlert.close(); // Ferme la boîte d'attente

            // ==========================================================
            // 1. RÉCUPÉRER LES RÉSULTATS DE LA TASK
            // ==========================================================
            AnalysisResult results = analysisTask.getValue(); // <-- C'EST ICI !
            if (results == null) {
                showAlert("Erreur", "L'analyse n'a retourné aucun résultat.", Alert.AlertType.ERROR);
                return;
            }

            // ==========================================================
            // 2. STOCKER LES RÉSULTATS DANS LES VARIABLES DU CONTRÔLEUR
            // ==========================================================
            this.javaParserCallGraph = results.javaParserGraph;
            this.spoonCallGraph = results.spoonGraph;
            this.currentMetrics = results.metrics; // Met à jour les métriques

            // ==============================================================
            // 3. INITIALISER 'this.moduleFinder' ICI !
            // ==============================================================
            // On l'initialise basé sur le graphe JavaParser par défaut (ou Spoon si JP a échoué)
            // pour que le bouton "Identifier" ait une base de travail après l'analyse.
            CallGraph graphForInitialModuleAnalysis = null;
            if (results.jpSuccess && this.javaParserCallGraph != null && !this.javaParserCallGraph.getGraph().isEmpty()) {
                graphForInitialModuleAnalysis = this.javaParserCallGraph;
            } else if (results.spoonSuccess && this.spoonCallGraph != null && !this.spoonCallGraph.getGraph().isEmpty()) {
                graphForInitialModuleAnalysis = this.spoonCallGraph; // Fallback sur Spoon
            }

            if (graphForInitialModuleAnalysis != null) {
                try {
                    ClassCouplingGraph initialCouplingGraph = couplingCalculator.calculate(graphForInitialModuleAnalysis);
                    if (initialCouplingGraph != null && !initialCouplingGraph.getGraph().isEmpty()) {
                        System.out.println("DEBUG: Initialisation de this.moduleFinder...");
                        this.moduleFinder = new ModuleFinder(initialCouplingGraph);
                        this.moduleFinder.buildDendrogram(); // Construire l'arbre maintenant
                    } else {
                        System.err.println("Le graphe de couplage initial est vide, this.moduleFinder non initialisé.");
                        this.moduleFinder = null; // S'assurer qu'il est null
                    }
                } catch(Exception ex) {
                    System.err.println("Erreur lors de l'initialisation du ModuleFinder:");
                    ex.printStackTrace();
                    this.moduleFinder = null;
                }
            } else {
                System.err.println("Aucun CallGraph valide disponible après l'analyse, this.moduleFinder non initialisé.");
                this.moduleFinder = null; // S'assurer qu'il est null
            }
            // ==============================================================
            // Fin de l'initialisation de 'this.moduleFinder'
            // ==============================================================

            // ==========================================================
            // 3. MISE À JOUR DE L'INTERFACE UTILISATEUR
            // ==========================================================

            // Mise à jour de la SYNTHÈSE et des TABLES (si les métriques existent)
            if (this.currentMetrics != null) {
                updateSummary(this.currentMetrics);

                // Calcul et mise à jour des classements Top 10%
                List<MethodMetric> allMethods = getAllMethodMetrics(this.currentMetrics);
                List<MethodMetric> topMethodsByLoc = getTop10PercentMethodsByLoc(allMethods);
                topMethodsTable.setItems(FXCollections.observableArrayList(topMethodsByLoc));

                List<ClassMetric> topMethodsClasses = getTop10PercentByMethods(this.currentMetrics);
                List<ClassMetric> topAttributesClasses = getTop10PercentByAttributes(this.currentMetrics);
                topAttributesTable.setItems(FXCollections.observableArrayList(topAttributesClasses));

                List<ClassMetric> intersectingClasses = getIntersection(topMethodsClasses, topAttributesClasses);
                intersectingClassesTable.setItems(FXCollections.observableArrayList(intersectingClasses));

                // Mise à jour de l'arborescence des classes pour l'explorateur
                if (classTreeView != null) {
                    populateClassTree(this.currentMetrics);
                }
            } else {
                // Optionnel : Vider les tables si les métriques n'ont pas pu être calculées
                System.err.println("Aucune métrique n'a été calculée (échec JavaParser ?)");
                topMethodsTable.getItems().clear();
                topAttributesTable.getItems().clear();
                intersectingClassesTable.getItems().clear();
                if (classTreeView != null) classTreeView.setRoot(null);
                // Vider summaryGrid ?
            }


            // Mise à jour de l'affichage textuel du graphe (peut-être choisir JP ou Spoon ?)
            // Ici, on affiche JavaParser par exemple
            if (callGraphTextArea != null) {
                if (this.javaParserCallGraph != null) {
                    callGraphTextArea.setText(this.javaParserCallGraph.toString());
                } else {
                    callGraphTextArea.setText("Graphe JavaParser non disponible.");
                }
            }

            // ==========================================================
            // 4. APPELER LA MÉTHODE CENTRALE POUR LES AFFICHAGES DE GRAPHES
            // ==========================================================
            // updateGraphViews va lire les boutons radio et utiliser
            // this.javaParserCallGraph ou this.spoonCallGraph en conséquence
            updateGraphViews();


            // ==========================================================
            // 5. MESSAGE FINAL
            // ==========================================================
            String successMessage = "Analyse terminée. ";
            if (results.jpSuccess) successMessage += "JavaParser OK. "; else successMessage += "JavaParser ÉCHEC. ";
            if (results.spoonSuccess) successMessage += "Spoon OK."; else successMessage += "Spoon ÉCHEC.";
            showAlert("Analyse Terminée", successMessage, Alert.AlertType.INFORMATION);

            // Sélectionne le premier onglet
            resultsTabPane.getSelectionModel().select(0);
        }); // Fin de setOnSucceeded

        // Lorsque la tâche échoue
        analysisTask.setOnFailed(e -> {
            waitAlert.close();
            showAlert("Erreur", "Une erreur inattendue est survenue pendant l'analyse.", Alert.AlertType.ERROR);
            analysisTask.getException().printStackTrace(); // Affiche l'erreur dans la console pour le débogage
        });

        // Démarrer la tâche dans un nouveau thread
        new Thread(analysisTask).start();
    }

// DANS MetricsController.java

    /**
     * Appelé lorsque l'utilisateur clique sur le bouton "Identifier les Modules".
     * Utilise le ModuleFinder (déjà calculé) pour trouver et afficher
     * les modules basés sur le seuil CP actuel du slider.
     */
    @FXML
    private void handleFindModules(ActionEvent event) {
        // 1. Vérifier que l'analyse a été faite
        if (this.moduleFinder == null) {
            if (modulesTreeView != null) {
                modulesTreeView.setRoot(new TreeItem<>("Veuillez d'abord lancer l'analyse."));
            }
            return;
        }

        // 2. Récupérer la valeur CP du slider
        double cpValue = cpSlider.getValue();

        // 3. Lancer l'identification (c'est très rapide)
        List<Cluster> modules = this.moduleFinder.findModules(cpValue);

        // 4. Appeler la méthode d'affichage
        displayModulesInTree(modules, cpValue);
    }

    // DANS MetricsController.java

    /**
     * Met à jour le TreeView "modulesTreeView" pour afficher les modules trouvés.
     */
    private void displayModulesInTree(List<Cluster> modules, double cpValue) {
        if (modulesTreeView == null) {
            return;
        }

        // 1. Créer le nœud racine
        TreeItem<String> rootItem = new TreeItem<>("Modules trouvés (Couplage > " + String.format("%.2f", cpValue) + ")");
        rootItem.setExpanded(true);
        modulesTreeView.setRoot(rootItem);

        // 2. Gérer le cas où aucun module n'est trouvé
        if (modules.isEmpty()) {
            rootItem.getChildren().add(new TreeItem<>("Aucun module trouvé pour ce seuil."));
            return;
        }

        // 3. Parcourir les modules et les ajouter à l'arbre
        int moduleCount = 1;
        // Trier les modules par taille (du plus grand au plus petit)
        modules.sort((c1, c2) -> Integer.compare(c2.getClasses().size(), c1.getClasses().size()));

        for (Cluster module : modules) {
            // Nœud principal pour le module
            TreeItem<String> moduleItem = new TreeItem<>(
                    "Module " + (moduleCount++) + " (" + module.getClasses().size() + " classes)"
            );
            moduleItem.setExpanded(true); // L'ouvrir par défaut
            rootItem.getChildren().add(moduleItem);

            // Ajouter chaque classe du module comme une "feuille"
            // (TreeSet pour les trier par ordre alphabétique)
            for (String className : new TreeSet<>(module.getClasses())) {
                moduleItem.getChildren().add(new TreeItem<>(className));
            }
        }
    }


    private void displayCallGraph(CallGraph callGraph) {
        System.out.println("--- DÉBUT DU DÉBOGAGE : displayCallGraph ---");

        // VÉRIFICATION DE SÉCURITÉ : Limite de taille et vérification FXML
        if (graphPane == null) {
            System.err.println("ERREUR CRITIQUE : graphPane est null."); return;
        }
        graphPane.getChildren().clear();

        if (callGraph == null || callGraph.getGraph().isEmpty()) {
            System.out.println("2. Graphe d'appel vide.");
            graphPane.getChildren().add(new Label("Aucune relation d'appel trouvée."));
            return;
        }

        // Si le graphe est trop grand (la vérification est faite ici pour la propreté)
        if (callGraph.getGraph().size() > 500) {
            System.out.println("Graphe d'appel trop grand (>500). Non affiché.");
            graphPane.getChildren().add(new Label("Graphe d'appel trop grand (>500 nœuds) pour affichage."));
            return;
        }

        System.out.println("2. Graphe d'appel contient " + callGraph.getGraph().size() + " appelants.");

        try {
            // 1. Configurer le moteur de rendu CORRECT (Utilisé par FxViewer)
            System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.fx_viewer.FxViewer");

            // 2. Créer l'objet GraphStream
            Graph graph = new SingleGraph("CallGraph");
            // Style : les étiquettes des nœuds doivent être visibles
            String styleSheet =
                    "node {" +
                            "   shape: box; " +          // Change la forme en boîte (carré/rectangle)
                            "   fill-color: #A9D0F5; " +
                            "   stroke-mode: plain; " +  // Bordure simple
                            "   stroke-color: #333; " +
                            "   size-mode: fit; " +      // La taille s'adapte au texte (très important)
                            "   padding: 10px, 5px; " +  // Espace à l'intérieur de la boîte
                            "   text-size: 12; " +
                            "   text-visibility-mode: normal; " +
                            "}" +
                            "edge {" +
                            "   arrow-shape: arrow; " +
                            "   shape: line; " +         // Lignes droites
                            "   fill-color: #555; " +
                            "}" +
                            "node:selected {" +         // Ce qui se passe si on clique
                            "   fill-color: #F4D03F; " +
                            "}";

            graph.setAttribute("ui.stylesheet", styleSheet);

            // Dans MetricsController.java, méthode displayCallGraph

// ... (code pour créer le graphe GraphStream) ...

// 3. Remplir le graphe avec les données (Correction ROBUSTE de l'étiquette des nœuds)
            for (Map.Entry<String, Set<String>> entry : callGraph.getGraph().entrySet()) {
                String caller = entry.getKey(); // Ex: "com.app.ClassA.methodA1(...)"
                String callerId = caller.replaceAll("[^a-zA-Z0-9_]", "_");

                // Correction pour l'étiquette du nœud (Caller)
                if (graph.getNode(callerId) == null) {
                    // Prend la partie après le dernier '.'
                    String potentialLabel = caller.substring(caller.lastIndexOf('.') + 1);
                    // Trouve la première parenthèse ouvrante '('
                    int parenthesisIndex = potentialLabel.indexOf('(');
                    // Le label est la partie AVANT la parenthèse (ou toute la chaîne si pas de parenthèse)
                    String label = (parenthesisIndex != -1) ? potentialLabel.substring(0, parenthesisIndex) : potentialLabel;
                    graph.addNode(callerId).setAttribute("ui.label", label); // Utilise le nom nettoyé
                }

                // Correction pour l'étiquette du nœud (Callee)
                for (String callee : entry.getValue()) {
                    String calleeId = callee.replaceAll("[^a-zA-Z0-9_]", "_");

                    if (graph.getNode(calleeId) == null) {
                        // On nettoie juste les parenthèses, mais on garde le nom complet
                        String label = callee.replaceAll("\\(.*\\)", ""); // Ex: "client.passerCommande"
                        graph.addNode(calleeId).setAttribute("ui.label", label);
                    }

                    // Ajouter l'arête
                    String edgeId = callerId + "_to_" + calleeId;
                    if (graph.getEdge(edgeId) == null) {
                        graph.addEdge(edgeId, callerId, calleeId, true);
                    }
            }
            }

            System.out.println("3. Graphe GraphStream (Appel) rempli avec " + graph.getNodeCount() + " nœuds.");

            // 4. Créer le visualiseur
            Viewer viewer = new FxViewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
            viewer.enableAutoLayout();

            // 5. Obtenir le panneau d'affichage (Méthode correcte pour gs-ui-fx 2.0)
            View view = viewer.addDefaultView(false); // false pour ne pas ouvrir de fenêtre séparée
            this.graphStreamView = view; // Stocker la View pour le zoom

            // Caster la View directement en FxViewPanel
            FxViewPanel viewPanel = (FxViewPanel) view;

            System.out.println("4. Panneau de vue (Appel) créé.");

            // 6. Lier et ajouter
            viewPanel.prefWidthProperty().bind(graphPane.widthProperty());
            viewPanel.prefHeightProperty().bind(graphPane.heightProperty());
            graphPane.getChildren().add(viewPanel);
            System.out.println("5. Panneau (Appel) ajouté à la scène.");

        } catch (Exception e) {
            System.err.println("ERREUR INATTENDUE pendant displayCallGraph :");
            e.printStackTrace();
        }
    }


    // Dans MetricsController.java


    private void displayCouplingGraph(ClassCouplingGraph couplingGraph) {
        // 0. Configurer le moteur de rendu JavaFX
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.fx_viewer.FxViewer");
        couplingGraphPane.getChildren().clear();

        if (couplingGraph == null || couplingGraph.getGraph().isEmpty()) {
            couplingGraphPane.getChildren().add(new Label("Aucun couplage inter-classe à afficher."));
            return;
        }

        // --- NOUVEAU : Récupérer la valeur actuelle du seuil ---
        double threshold = 0.0; // Valeur par défaut si le slider n'existe pas
        if (couplingSlider != null) {
            threshold = couplingSlider.getValue();}
        System.out.println("DEBUG: Affichage du graphe de couplage avec seuil = " + threshold); // Debug

        // 1. Créer le graphe GraphStream
        Graph graph = new SingleGraph("CouplingGraph");
        String styleSheet =
                "node {" +
                        "   shape: box; " +          // Change la forme en boîte (carré/rectangle)
                        "   fill-color: #A9D0F5; " +
                        "   stroke-mode: plain; " +  // Bordure simple
                        "   stroke-color: #333; " +
                        "   size-mode: fit; " +      // La taille s'adapte au texte (très important)
                        "   padding: 10px, 5px; " +  // Espace à l'intérieur de la boîte
                        "   text-size: 12; " +
                        "   text-visibility-mode: normal; " +
                        "}" +
                        "edge {" +
                        "   arrow-shape: arrow; " +
                        "   shape: line; " +         // Lignes droites
                        "   fill-color: #555; " +
                        "}" +
                        "node:selected {" +         // Ce qui se passe si on clique
                        "   fill-color: #F4D03F; " +
                        "}";

        graph.setAttribute("ui.stylesheet", styleSheet);

        // 2. Remplir le graphe (MODIFICATION ICI POUR FILTRER)
        boolean edgeAdded = false; // Pour savoir si on a ajouté au moins une arête
        for (Map.Entry<String, Map<String, Double>> entry : couplingGraph.getGraph().entrySet()) {
            String sourceClass = entry.getKey();

            for (Map.Entry<String, Double> targetEntry : entry.getValue().entrySet()) {
                String targetClass = targetEntry.getKey();
                double weight = targetEntry.getValue();

                // --- FILTRE ICI ---
                // N'ajoute l'arête QUE si son poids est supérieur ou égal au seuil
                if (weight >= threshold) {

                    // Crée les nœuds s'ils n'existent pas (inchangé)
                    if (graph.getNode(sourceClass) == null) {
                        graph.addNode(sourceClass).setAttribute("ui.label", sourceClass.substring(sourceClass.lastIndexOf('.') + 1));
                    }
                    if (graph.getNode(targetClass) == null) {
                        graph.addNode(targetClass).setAttribute("ui.label", targetClass.substring(targetClass.lastIndexOf('.') + 1));
                    }

                    // Ajoute l'arête filtrée (inchangé)
                    String edgeId = sourceClass + "->" + targetClass;
                    if (graph.getEdge(edgeId) == null) {
                        graph.addEdge(edgeId, sourceClass, targetClass, true)
                                .setAttribute("ui.label", String.format("%.3f", weight));
                        edgeAdded = true;
                    }
                } // Fin du if (weight >= threshold)
            }
        } // Fin des boucles

        // Si aucune arête n'a été ajoutée (tout a été filtré), afficher un message
        if (!edgeAdded && graph.getNodeCount() == 0) { // Vérifie aussi s'il n'y a pas de nœuds isolés
            couplingGraphPane.getChildren().add(new Label("Aucun couplage supérieur au seuil de " + String.format("%.2f", threshold)));
            return; // Ne pas afficher de graphe vide
        }

        // 3. Créer le visualiseur (Viewer)
        Viewer viewer = new FxViewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        viewer.enableAutoLayout();

        // 4. Obtenir le panneau d'affichage (LA CORRECTION FINALE EST ICI)
        // On utilise addDefaultView avec 'false' et un nouveau FxGraphRenderer, comme vu dans le code source
        View view = viewer.addDefaultView(false);

        this.graphStreamView= view;

        // Le cast direct vers FxViewPanel est la clé
        FxViewPanel viewPanel = (FxViewPanel) view;


        // 5. Lier la taille et ajouter le panneau à l'interface
        viewPanel.prefWidthProperty().bind(couplingGraphPane.widthProperty());
        viewPanel.prefHeightProperty().bind(couplingGraphPane.heightProperty());
        couplingGraphPane.getChildren().add(viewPanel);
    }

// Dans MetricsController.java

    private void handleGenerateGraph() {
        String sourcePath = sourcePathField.getText();
        // ... (vérification du chemin) ...

        // Vider les DEUX panneaux
        graphPane.getChildren().clear();
        matrixPane.getChildren().clear();
        graphPane.getChildren().add(new Label("Analyse en cours..."));
        matrixPane.getChildren().add(new Label("Analyse en cours..."));

        // Créer la Tâche d'arrière-plan (inchangée)
        Task<CallGraph> analysisTask = new Task<CallGraph>() {
            @Override
            protected CallGraph call() throws Exception {
                SourceCodeAnalyzer analyzer = new SourceCodeAnalyzer();
                 return analyzer.generateCallGraph(sourcePath);

                // NEW:
              //  SpoonAnalyzer spoonAnalyzer = new SpoonAnalyzer();
                //return spoonAnalyzer.analyzeProject(sourcePath);
            }
        };


        analysisTask.setOnSucceeded(event -> {
            CallGraph resultGraph = analysisTask.getValue(); // Récupère le résultat

            // 1. Appeler la première méthode d'affichage
            displayCallGraph(resultGraph);

            // 2. Appeler la DEUXIÈME méthode d'affichage
            displayAdjacencyMatrix(resultGraph);
        });

        // Que faire si l'analyse échoue
        analysisTask.setOnFailed(event -> {
            graphPane.getChildren().clear();
            matrixPane.getChildren().clear();
            graphPane.getChildren().add(new Label("Erreur durant l'analyse."));
            matrixPane.getChildren().add(new Label("Erreur durant l'analyse."));
            analysisTask.getException().printStackTrace();
        });

        // Démarrer la tâche
        new Thread(analysisTask).start();
    }

    // REMPLACEZ VOTRE ANCIENNE MÉTHODE PAR CELLE-CI
    private void displayAdjacencyMatrix(CallGraph callGraph) {
        System.out.println("--- DÉBUT AFFICHAGE MATRICE D'ADJACENCE ---");

        // VÉRIFICATION CRITIQUE : Cible le bon panneau
        if (matrixPane == null) {
            System.err.println("ERREUR CRITIQUE : matrixPane est null.");
            return;
        }
        matrixPane.getChildren().clear(); // Nettoie le bon panneau

        if (callGraph == null || callGraph.getGraph().isEmpty()) {
            matrixPane.getChildren().add(new Label("Aucune relation d'appel trouvée."));
            return;
        }

        try {
            // --- 1. Préparer les données (Trier, mapper, etc.) ---
            Set<String> allMethodsSet = new TreeSet<>();
            // ... (logique pour remplir allMethodsSet)
            for (Map.Entry<String, Set<String>> entry : callGraph.getGraph().entrySet()) {
                allMethodsSet.add(entry.getKey());
                allMethodsSet.addAll(entry.getValue());
            }
            List<String> allMethods = new ArrayList<>(allMethodsSet);
            Map<String, Integer> methodIndexMap = new HashMap<>();
            for (int i = 0; i < allMethods.size(); i++) {
                methodIndexMap.put(allMethods.get(i), i);
            }

            // --- 2. Créer la Grille (GridPane) ---
            GridPane grid = new GridPane();
            grid.setGridLinesVisible(true);

            // --- AJOUTER CETTE PARTIE POUR LES CONTRAINTES DE COLONNE ---
            int numberOfColumns = allMethods.size() + 1; // +1 pour la colonne des en-têtes de ligne
            double minColumnWidth = 300.0; // Essayez cette valeur (en pixels), ajustez si besoin

            for (int i = 0; i < numberOfColumns; i++) {
                ColumnConstraints colConst = new ColumnConstraints();
                colConst.setMinWidth(minColumnWidth);
                // Optionnel : Pourrait aider si le texte est long mais pas assez pour forcer la largeur
                 colConst.setPrefWidth(Region.USE_COMPUTED_SIZE);
                grid.getColumnConstraints().add(colConst);
            }
            System.out.println("DEBUG: Ajout de " + numberOfColumns + " ColumnConstraints avec minWidth=" + minColumnWidth); // Message de débogage
// --- FIN DE L'AJOUT ---

            // --- 3. Remplir les en-têtes (Ligne 0 et Colonne 0) ---
            grid.add(createHeaderCell("Appelant \\ Appelé"), 0, 0);
            for (int i = 0; i < allMethods.size(); i++) {
                String methodName = allMethods.get(i).replaceAll("\\(.*\\)", "");
                grid.add(createHeaderCell(methodName), i + 1, 0); // En-têtes Colonnes
                grid.add(createHeaderCell(methodName), 0, i + 1); // En-têtes Lignes
            }

            // --- 4. Remplir la matrice avec les appels ---
            for (Map.Entry<String, Set<String>> entry : callGraph.getGraph().entrySet()) {
                int callerIndex = methodIndexMap.get(entry.getKey());
                for (String callee : entry.getValue()) {
                    int calleeIndex = methodIndexMap.get(callee);
                    grid.add(createMatrixCell(), calleeIndex + 1, callerIndex + 1);
                }
            }

            // --- 5. Mettre la grille dans un ScrollPane et l'afficher ---
            ScrollPane scrollPane = new ScrollPane(grid);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.prefWidthProperty().bind(matrixPane.widthProperty());
            scrollPane.prefHeightProperty().bind(matrixPane.heightProperty());

            // AFFICHER DANS LE BON PANNEAU
            matrixPane.getChildren().add(scrollPane);
            System.out.println("Matrice d'adjacence affichée.");

        } catch (Exception e) {
            System.err.println("ERREUR pendant displayAdjacencyMatrix :");
            e.printStackTrace();
            matrixPane.getChildren().add(new Label("Erreur fatale : " + e.getMessage()));
        }
    }


// --- MÉTHODES UTILITAIRES (à ajouter dans votre MetricsController) ---

    /**
     * Crée une cellule d'en-tête (Label) avec un style
     */
    private Label createHeaderCell(String text) {
        Label label = new Label(text);
        label.setMaxWidth(Double.MAX_VALUE); // Pour qu'il remplisse la case
        label.setPadding(new Insets(5));
        label.setStyle("-fx-font-size: 14px; -fx-background-color: #EEEEEE; -fx-font-weight: bold; -fx-border-color: #CCCCCC; -fx-border-width: 0.5;");

        // Rotation pour les en-têtes de colonnes (plus lisible si longs)
         //label.setRotate(-45); // Décommentez si vous préférez
        return label;
    }

    /**
     * Crée une cellule de la matrice (un carré coloré)
     */
    private Pane createMatrixCell() {
        Rectangle square = new Rectangle(20, 20); // Carré de 20x20
        square.setFill(Color.web("#0078D7")); // Couleur bleue

        // StackPane pour centrer le carré (au cas où la cellule est plus grande)
        StackPane cellPane = new StackPane(square);
        cellPane.setPadding(new Insets(2));
        cellPane.setStyle("-fx-border-color: #DDDDDD; -fx-border-width: 0.5;");
        return cellPane;
    }

    /**
     * Remplit le TreeView avec les packages et les classes
     * à partir des métriques calculées.
     */
    // Dans MetricsController.java

    /**
     * Remplit le TreeView avec les packages et les classes
     * à partir des métriques calculées.
     */
    private void populateClassTree(Map<String, ClassMetric> classMetrics) {
        // 1. Créer un nœud racine invisible
        TreeItem<String> rootItem = new TreeItem<>("Projet");
        rootItem.setExpanded(true);
        this.classTreeView.setRoot(rootItem);
        this.classTreeView.setShowRoot(false); // Cache le nœud "Projet"

        // 2. Grouper les classes par package
        Map<String, List<ClassMetric>> classesByPackage = classMetrics.values().stream()
                .collect(Collectors.groupingBy(ClassMetric::getPackageName));

        // 3. Créer les nœuds de package
        for (String packageName : new TreeSet<>(classesByPackage.keySet())) {

            // Gère le cas où il n'y a pas de package
            String displayedPackageName = packageName.isEmpty() ? "(Default Package)" : packageName;
            TreeItem<String> packageItem = new TreeItem<>(displayedPackageName);
            rootItem.getChildren().add(packageItem);

            // 4. Ajouter les classes (avec leur nom simple) à ce package
            for (ClassMetric classMetric : classesByPackage.get(packageName)) {
                // On ajoute juste l'item avec le nom simple. C'est tout.
                TreeItem<String> classItem = new TreeItem<>(classMetric.getName());
                packageItem.getChildren().add(classItem);
            }
        }
    }


    /**
     * Génère la représentation DOT du dendrogramme à partir du cluster racine.
     */
    private String generateDendrogramDotString(Cluster root) {
        StringBuilder dot = new StringBuilder();
        dot.append("digraph Dendrogram {\n");
        dot.append("  node [shape=record, fontname=\"Arial\", height=.1];\n"); // Style pour les nœuds
        dot.append("  edge [arrowhead=none];\n"); // Pas de flèches pour un arbre

        // Map pour stocker les IDs uniques des nœuds Graphviz
        Map<Cluster, String> nodeIds = new HashMap<>();

        // Lancer la construction récursive
        addClusterToDot(root, dot, nodeIds);

        dot.append("}\n");
        return dot.toString();
    }

    /**
     * Helper récursif pour ajouter un cluster (et ses enfants) au DOT.
     */
    private String addClusterToDot(Cluster cluster, StringBuilder dot, Map<Cluster, String> nodeIds) {
        // Si on a déjà traité ce nœud, on retourne son ID
        if (nodeIds.containsKey(cluster)) {
            return nodeIds.get(cluster);
        }

        String nodeId;
        if (cluster.isLeaf()) {
            // C'est une feuille (une classe)
            nodeId = "class_" + cluster.getClassName().replaceAll("[^a-zA-Z0-9_]", "_");
            dot.append(String.format("  %s [label=\"%s\", shape=box];\n",
                    nodeId, cluster.getClassName()));
        } else {
            // C'est un nœud interne (une fusion)
            // On crée un ID unique basé sur hashCode (simple)
            nodeId = "node_" + Math.abs(cluster.hashCode());

            // Crée le nœud (on peut y mettre la hauteur/le couplage de fusion)
            dot.append(String.format("  %s [label=\"\", shape=point];\n", nodeId));
            // ou label=\"%.3f\", cluster.getHeight() si getHeight existe

            // Appel récursif pour les enfants
            String leftChildId = addClusterToDot(cluster.getLeftChild(), dot, nodeIds);
            String rightChildId = addClusterToDot(cluster.getRightChild(), dot, nodeIds);

            // Ajoute les arêtes vers les enfants
            dot.append(String.format("  %s -> %s;\n", nodeId, leftChildId));
            dot.append(String.format("  %s -> %s;\n", nodeId, rightChildId));
        }

        // Stocker l'ID généré pour ce cluster
        nodeIds.put(cluster, nodeId);
        return nodeId;
    }

    /**
     * Affiche le dendrogramme (arbre de clustering) dans l'onglet dédié.
     * PREND MAINTENANT ModuleFinder en argument.
     */
    private void displayDendrogram(ModuleFinder finder) { // Reçoit le finder
        System.out.println("DEBUG: Entrée dans displayDendrogram."); // <-- DEBUG 1

        if (dendrogramPane == null) {
            System.err.println("ERREUR CRITIQUE: dendrogramPane est null."); // <-- DEBUG 2
            return;
        }
        dendrogramPane.getChildren().clear();

        if (finder == null) {
            System.out.println("DEBUG: Le ModuleFinder reçu est null."); // <-- DEBUG 3
            dendrogramPane.getChildren().add(new Label("Veuillez d'abord lancer l'analyse et sélectionner une source."));
            return;
        }

        // Récupère la racine (buildDendrogram a déjà été appelé dans updateGraphViews)
        Cluster dendrogramRoot = finder.getDendrogramRoot();

        if (dendrogramRoot == null) {
            System.out.println("DEBUG: dendrogramRoot est null (clustering échoué ou graphe de couplage vide?)."); // <-- DEBUG 4
            dendrogramPane.getChildren().add(new Label("Impossible de générer le dendrogramme (analyse échouée ou graphe de couplage vide?)."));
            return;
        }
        System.out.println("DEBUG: Racine du dendrogramme trouvée."); // <-- DEBUG 5

        try {
            System.out.println("DEBUG: Tentative de génération du DOT pour le dendrogramme..."); // <-- DEBUG 6
            String dotDefinition = generateDendrogramDotString(dendrogramRoot);
            System.out.println("DEBUG: DOT généré."); // <-- DEBUG 7

            File imageFile = generateGraphImage(dotDefinition);
            System.out.println("DEBUG: Tentative de génération de l'image terminée."); // <-- DEBUG 8

            if (imageFile != null && imageFile.exists()) {
                System.out.println("DEBUG: L'image existe. Chargement..."); // <-- DEBUG 9
                // ... (code pour charger et afficher l'ImageView) ...
                Image image = new Image(new FileInputStream(imageFile));
                ImageView imageView = new ImageView(image);
                imageView.setPreserveRatio(true);
                ScrollPane scrollPane = new ScrollPane(imageView);
                scrollPane.setFitToWidth(true);
                scrollPane.setFitToHeight(true);
                scrollPane.prefWidthProperty().bind(dendrogramPane.widthProperty());
                scrollPane.prefHeightProperty().bind(dendrogramPane.heightProperty());
                dendrogramPane.getChildren().add(scrollPane);
                System.out.println("Image du dendrogramme affichée."); // <-- DEBUG 10
            } else {
                System.err.println("ERREUR: L'image n'a pas été générée ou est introuvable."); // <-- DEBUG 11
                dendrogramPane.getChildren().add(new Label("Erreur : L'image du dendrogramme n'a pas pu être générée."));
            }
        } catch (Exception e) {
            System.err.println("ERREUR Exception dans displayDendrogram:"); // <-- DEBUG 12
            e.printStackTrace();
            dendrogramPane.getChildren().add(new Label("Erreur de génération du dendrogramme : " + e.getMessage()));
        }
    }



}


//        FxViewPanel viewPanel = (FxViewPanel) viewer.addDefaultView(false);