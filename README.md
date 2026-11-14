# ORION AI — prototype Android pour Galaxy Tab A9+

Version de développement 0.3.0.

## Cible
Samsung Galaxy Tab A9+ 4 Go RAM / 64 Go, Wi-Fi + 5G. L'IA fonctionne à distance pour préserver la mémoire de la tablette.

## Nouveautés v0.3
- Interface futuriste adaptée à la tablette.
- Fournisseur IA distant compatible OpenAI Chat Completions configurable.
- Dictée française Android et synthèse vocale.
- Réglage du nom, endpoint, modèle, clé et prompt système.
- Phrase d'activation personnalisable enregistrée dans les réglages.
- Option de confirmation avant les actions locales importantes.
- Confirmation explicite avant la commande locale `/local parametres`.
- Commandes locales de démonstration : `/local heure`, `/local date`, `/local parametres`.

## Important : mot d'activation
Cette version permet de configurer et sauvegarder la phrase souhaitée (par défaut « Bonjour Orion »), mais **ne réalise pas encore de détection permanente du mot-clé**. L'écoute continue fiable nécessite un moteur de détection de mot-clé (par ex. modèle embarqué dédié), un service Android au premier plan, une notification persistante, une gestion de la batterie et des tests sur l'appareil. La dictée actuelle démarre à la demande via le bouton micro. Aucune écoute cachée n'est effectuée.

## Installation / compilation
1. Décompresser l'archive.
2. Ouvrir le dossier dans Android Studio sur un ordinateur avec JDK 17 et Android SDK 35.
3. Synchroniser Gradle et lancer sur la tablette, ou choisir **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Installer l'APK généré.

Cette archive contient le code source, pas un APK précompilé ni certifié testé sur le matériel.

## Première configuration IA
Dans Paramètres :
- URL par défaut : `https://api.openai.com/v1/chat/completions`
- Modèle : identifiant disponible chez ton fournisseur.
- Clé API : clé personnelle du fournisseur.

La clé est enregistrée dans Preferences DataStore. Pour un déploiement sérieux, préférer un backend relais sécurisé : un APK contenant une clé ne constitue pas un coffre-fort.

## Limites
Ce prototype n'est pas un agent qui peut librement contrôler toutes les applications. Agenda, e-mail, fichiers, domotique et actions système nécessitent des intégrations séparées, des permissions et des confirmations. Le mode hors ligne complet n'est pas inclus.

## Compilation dans le cloud (sans ordinateur)
Le dépôt contient un workflow GitHub Actions dans `.github/workflows/build-apk.yml`.
Après avoir placé les fichiers du projet dans un dépôt GitHub personnel, ouvrir l'onglet **Actions**, choisir **Build ORION AI APK**, puis **Run workflow**. À la fin, télécharger l'artefact **ORION-AI-debug-APK** depuis le résumé de l'exécution. Le dépôt doit contenir les fichiers du projet, pas seulement l'archive ZIP.
