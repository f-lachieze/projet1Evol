Outil d'Analyse Statique pour Code Orienté Objet
Ce projet est une application de bureau développée en JavaFX pour l'analyse statique du code source d'applications orientées objet. 
Il a été réalisé dans le cadre du cours HAI913I : Évolution et Restructuration des Logiciels.

L'outil calcule un ensemble de métriques logicielles (taille, complexité, cohésion), identifie les classes et méthodes critiques, et analyse les dépendances fonctionnelles via des graphes d'appel, de couplage, et l'identification de modules potentiels pour aider à la compréhension et à la restructuration du code.

Prérequis
Avant de commencer, assurez-vous d'avoir les outils suivants installés sur votre système :

- Java Development Kit (JDK) : Version 21 ou supérieure.

- Apache Maven : Version 3.6 ou supérieure, pour la gestion des dépendances et la construction du projet.

- Graphviz : Requis pour la visualisation de certains graphes (Dendrogramme, Explorateur d'Appels).

Lancement
La manière la plus simple de lancer l'application est via le plugin Maven JavaFX, qui gère automatiquement les modules Java. Exécutez la commande suivante à la racine du projet :

(Bash)
mvn javafx:run

Guide d'Utilisation
1. Sélectionner le Répertoire Source
   Cliquez sur le bouton "Parcourir".

Dans la fenêtre qui s'ouvre, naviguez jusqu'au dossier contenant le code source Java que vous souhaitez analyser (par exemple, le dossier racine d'un projet Maven contenant src/main/java).

Cliquez sur "Ouvrir". Le chemin du répertoire s'affichera dans le champ de texte.

2. Choisir le Moteur d'Analyse (Optionnel)
   Utilisez les boutons radio "Moteur d'Analyse" (situés à droite du bouton "Analyser") pour choisir entre :

JavaParser : Utilise JavaParser pour l'analyse (par défaut).

Spoon : Utilise la bibliothèque Spoon pour l'analyse.

Ce choix affecte les données utilisées pour tous les onglets liés aux graphes (Appel, Couplage, Matrice, Explorateur, Modules, Dendrogramme).

3. Lancer l'Analyse
   Cliquez sur le bouton "Analyser".

Une boîte de dialogue "Analyse en cours..." apparaîtra. Pour les gros projets, cette étape peut prendre de quelques secondes à plusieurs minutes (Spoon peut être plus lent que JavaParser). L'interface ne se figera pas.

4. Consulter les Résultats
   Une fois l'analyse terminée (un message "Analyse terminée..." s'affiche), les résultats sont répartis dans les onglets suivants :

Synthèse & Totaux : Affiche les métriques globales du projet (nombre de classes, méthodes, LoC, moyennes, etc.). Basé sur l'analyse JavaParser.

Classes et Méthodes Critiques : Identifie les classes et méthodes potentiellement problématiques (Top 10% par taille, complexité, etc.). Basé sur l'analyse JavaParser.

Filtrage par Nombre de Méthodes (X) : Permet de trouver les classes ayant plus de X méthodes. Basé sur l'analyse JavaParser.
Entrez un nombre dans le champ "X".
Cliquez sur "Filtrer".

Graphe d'Appel : Représentation textuelle du graphe d'appel (quelle méthode appelle quelle autre). Utilise le moteur sélectionné (JavaParser/Spoon).

Graphe d'Appel Visuel : Représentation graphique (nœuds et arêtes) du graphe d'appel. Utilise le moteur sélectionné (JavaParser/Spoon).

Graphe de Couplage : Visualisation graphique des dépendances entre classes. Le label des arêtes représente la force du couplage. Utilise le moteur sélectionné (JavaParser/Spoon).
Le slider "Seuil de couplage" permet de filtrer visuellement les arêtes.

Matrice d'Adjacence : Représentation matricielle du graphe d'appel (méthodes x méthodes). Une case colorée indique un appel. Utile pour les grands projets. Utilise le moteur sélectionné (JavaParser/Spoon).

Explorateur d'Appels : Permet de visualiser un sous-graphe d'appel centré sur une classe spécifique.
Cliquez sur une classe dans l'arborescence à gauche.
Le graphe à droite montre les appels entrants et sortants de cette classe. Utilise le moteur sélectionné (JavaParser/Spoon).

Modules et Clustering : Identifie des groupes de classes fortement couplées (modules potentiels) basés sur l'algorithme de clustering hiérarchique.
Utilisez le slider "Seuil de Couplage (CP)" pour définir le niveau de cohésion interne requis pour former un module.
Cliquez sur "Identifier les Modules".
L'arborescence affiche les modules trouvés pour le seuil CP choisi. Utilise le moteur sélectionné (JavaParser/Spoon).

Dendrogramme Clustering : Visualisation graphique de l'arbre de clustering hiérarchique construit par l'algorithme. 
Montre comment les classes ont été regroupées en fonction de leur couplage. 
Utilise le moteur sélectionné (JavaParser/Spoon).

Technologies Utilisées
JavaFX : Framework pour l'interface graphique.

JavaParser : Bibliothèque pour l'analyse du code source Java et la construction de l'AST (utilisée comme un des moteurs d'analyse).

Spoon : Bibliothèque pour l'analyse et la transformation du code source Java (utilisée comme l'autre moteur d'analyse).

GraphStream : Bibliothèque pour la modélisation et la visualisation de graphes (utilisée pour le Graphe d'Appel Visuel et le Graphe de Couplage).

Graphviz : Outil externe pour la génération d'images de graphes (utilisé pour l'Explorateur d'Appels et le Dendrogramme).

Maven : Outil de gestion de projet et de dépendances.