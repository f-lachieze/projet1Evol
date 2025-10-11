Outil d'Analyse Statique pour Code Orienté Objet

Ce projet est une application de bureau développée en JavaFX pour l'analyse statique du code source d'applications orientées objet. Il a été réalisé dans le cadre du cours HAI913I : Évolution et Restructuration des Logiciels.

L'outil calcule un ensemble de métriques logicielles (taille, complexité, cohésion, couplage), identifie les classes et méthodes critiques, et construit un graphe d'appel visuel pour aider à la compréhension et à la restructuration du code.

Prérequis
Avant de commencer, assurez-vous d'avoir les outils suivants installés sur votre système :

Java Development Kit (JDK) : Version 21 ou supérieure.

Apache Maven : Version 3.6 ou supérieure, pour la gestion des dépendances et la construction du projet.


Lancement
La manière la plus simple de lancer l'application est via le plugin Maven JavaFX, qui gère automatiquement les modules Java.
Exécutez la commande suivante à la racine du projet : "mvn javafx:run"


1. Guide d'Utilisation
Sélectionner le Répertoire Source

Cliquez sur le bouton "Parcourir".

Dans la fenêtre qui s'ouvre, naviguez jusqu'au dossier contenant le code source Java que vous souhaitez analyser (par exemple, le dossier src/main/java d'un projet).

Cliquez sur "Ouvrir". Le chemin du répertoire s'affichera dans le champ de texte.


2. Lancer l'Analyse

Cliquez sur le bouton "Analyser".

Une boîte de dialogue "Analyse en cours..." apparaîtra. Pour les gros projets, cette étape peut prendre de quelques secondes à plusieurs minutes. L'interface ne se figera pas pendant ce temps.


3. Consulter les Résultats
Une fois l'analyse terminée, les résultats sont répartis dans les onglets suivants :

Synthèse & Totaux : Affiche les métriques globales du projet : nombre total de classes, de méthodes, de lignes de code (LoC), ainsi que les moyennes et le nombre maximal de paramètres.

Classes et Méthodes Critiques : Cet onglet permet d'identifier les classes qui peuvent devenir problématique.

Filtrage par Nombre de Méthodes (X) : Permet de trouver rapidement les classes qui dépassent un certain seuil de complexité.

Entrez un nombre (par exemple 10) dans le champ "X".

Cliquez sur "Filtrer". Le tableau affichera toutes les classes ayant plus de X méthodes.

Graphe d'Appel : Affiche une représentation textuelle du graphe d'appel, montrant quelle méthode en appelle une autre.

Graphe d'Appel Visuel : Affiche une représentation graphique du graphe d'appel avec des nœuds et des arêtes, permettant de visualiser les dépendances fonctionnelles.


4. Technologies Utilisées
JavaFX : Framework pour l'interface graphique.

JavaParser : Bibliothèque pour l'analyse du code source Java et la construction de l'AST.

GraphStream : Bibliothèque pour la modélisation et la visualisation de graphes.

Maven : Outil de gestion de projet et de dépendances.
