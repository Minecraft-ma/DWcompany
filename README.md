# 🏢 DWcompany - Plugin Minecraft 1.16.5

Plugin complet de gestion d'entreprises pour serveurs Minecraft avec support Vault, EssentialsX et Dynmap.

## 📋 Fonctionnalités

### 🏗️ Système d'Entreprises
- Création d'entreprises avec nom personnalisé
- Système de niveaux (1-7) basé sur les gains
- Statut National (5 membres) ou International (10 membres)
- Gestion complète des membres et permissions
- Siège social avec marqueurs Dynmap

### 💰 Banque d'Entreprise
- Compte bancaire partagé par entreprise
- Dépôts et retraits via Vault
- Historique des gains totaux
- Interface graphique intuitive

### 🏭 Système de Filiales
- Création de filiales
- Ajout d'entreprises existantes comme filiales
- Approbation du CEO requise
- Affichage hiérarchique dans l'interface

### 🎨 Interface Graphique
- Menu principal interactif
- Liste des entreprises avec pagination
- Tri automatique par niveau
- Icônes de matériaux selon le niveau
- Effets sonores configurables

## 🚀 Installation

### Prérequis
1. **Serveur Minecraft 1.16.5** (Spigot/Paper)
2. **Vault** - Pour le système économique
3. **Plugin d'économie** (EssentialsX recommandé)
4. **Dynmap** (optionnel) - Pour les marqueurs de carte

### Étapes d'Installation
1. Télécharger le fichier `DWcompany-1.0-SNAPSHOT.jar`
2. Placer dans le dossier `plugins/` de votre serveur
3. Redémarrer le serveur
4. Configurer les fichiers générés:
   - `plugins/DWcompany/config.yml`
   - `plugins/DWcompany/messages.yml`

## 📖 Commandes

### Commandes de Base
```
/entreprise                          - Ouvrir le menu principal
/entreprise help                     - Afficher l'aide
/entreprise list                     - Liste des entreprises
/entreprise info [entreprise]        - Voir les informations
```

### Gestion d'Entreprise
```
/entreprise create <nom>             - Créer une entreprise (100k-500k)
/entreprise delete                   - Supprimer votre entreprise
/entreprise transfer <joueur>        - Transférer la propriété
/entreprise batiment                 - Définir le siège social
/entreprise international            - Upgrade International (20k)
```

### Membres
```
/entreprise join <entreprise>        - Demander à rejoindre
/entreprise leave                    - Quitter l'entreprise
/entreprise accept <joueur>          - Accepter une demande (CEO)
/entreprise deny <joueur>            - Refuser une demande (CEO)
/entreprise kick <joueur>            - Expulser un membre (CEO)
/entreprise members                  - Liste des membres
/entreprise requests                 - Voir les demandes (CEO)
```

### Banque
```
/entreprise bank                     - Ouvrir l'interface bancaire
/entreprise bank deposit <montant>   - Déposer de l'argent
/entreprise bank withdraw <montant>  - Retirer de l'argent (CEO)
/entreprise bank balance             - Voir le solde
```

### Filiales
```
/entreprise filiale create <nom>     - Créer une filiale
/entreprise filiale add <entreprise> - Ajouter une filiale
/entreprise filiale remove <nom>     - Retirer une filiale
/entreprise filiale list             - Liste des filiales
```

### Administration
```
/entreprise reload                   - Recharger la configuration
/entreprise admin delete <entreprise> - Supprimer une entreprise
```

## 🔐 Permissions

### Permissions Joueurs
```yaml
dwcompany.use              # Utiliser les commandes de base (défaut: true)
dwcompany.create           # Créer une entreprise (défaut: true)
dwcompany.join             # Rejoindre une entreprise (défaut: true)
dwcompany.leave            # Quitter une entreprise (défaut: true)
dwcompany.bank.deposit     # Déposer dans la banque (défaut: true)
```

### Permissions CEO
```yaml
dwcompany.bank.withdraw    # Retirer de la banque (défaut: false)
dwcompany.filiale.create   # Créer des filiales (défaut: false)
dwcompany.filiale.add      # Ajouter des filiales (défaut: false)
dwcompany.batiment         # Définir le siège social (défaut: false)
dwcompany.status.change    # Changer le statut (défaut: false)
dwcompany.ceo              # Permissions CEO complètes (défaut: false)
```

### Permissions Admin
```yaml
dwcompany.admin            # Commandes administrateur (défaut: op)
```

## ⚙️ Configuration

### config.yml - Paramètres Principaux

```yaml
companies:
  max-per-player: 2                    # Nombre max d'entreprises par joueur
  creation-cost:
    first: 100000.0                    # Coût première entreprise
    second: 500000.0                   # Coût deuxième entreprise
  member-limits:
    national: 5                        # Limite membres National
    international: 10                  # Limite membres International
  international-upgrade-cost: 20000.0  # Coût upgrade International

levels:
  level-1-money: 0                     # Argent requis niveau 1
  level-2-money: 10000                 # Argent requis niveau 2
  level-3-money: 50000                 # Argent requis niveau 3
  level-4-money: 100000                # Argent requis niveau 4
  level-5-money: 250000                # Argent requis niveau 5
  level-6-money: 500000                # Argent requis niveau 6
  level-7-money: 1000000               # Argent requis niveau 7

storage:
  type: YAML                           # Type de stockage (YAML/MYSQL)
  mysql:                               # Configuration MySQL
    host: localhost
    port: 3306
    database: dwcompany
    username: root
    password: ''

autosave:
  enabled: true                        # Sauvegarde automatique
  interval-minutes: 15                 # Intervalle en minutes

dynmap:
  enabled: true                        # Activer Dynmap
  marker-icon: building                # Icône des marqueurs
```

### messages.yml - Messages Personnalisables

Tous les messages du plugin sont personnalisables avec support des codes couleur (`&`).

## 📊 Système de Niveaux

| Niveau | Argent Requis | Icône | Couleur |
|--------|---------------|-------|---------|
| 1 | 0$ | 🪨 Cobblestone | Gris |
| 2 | 10,000$ | ⚙️ Iron Block | Gris clair |
| 3 | 50,000$ | 🥇 Gold Block | Or |
| 4 | 100,000$ | 💎 Diamond Block | Aqua |
| 5 | 250,000$ | 💚 Emerald Block | Jaune |
| 6 | 500,000$ | 🖤 Obsidian | Violet |
| 7 | 1,000,000$ | 🔥 Netherite Block | Rouge |

## 🎮 Guide d'Utilisation

### Créer une Entreprise
1. Avoir au moins 100,000$ (première entreprise)
2. Taper `/entreprise create MonEntreprise`
3. Vous devenez automatiquement CEO

### Recruter des Membres
1. Les joueurs tapent `/entreprise join VotreEntreprise`
2. Vous recevez une notification
3. Accepter avec `/entreprise accept NomJoueur`

### Gérer la Banque
1. Ouvrir avec `/entreprise bank`
2. Cliquer sur les boutons pour déposer/retirer
3. Ou utiliser les commandes directes

### Créer des Filiales
1. Être CEO d'une entreprise
2. `/entreprise filiale create NomFiliale`
3. Ou ajouter une existante: `/entreprise filiale add AutreEntreprise`

### Définir le Siège Social
1. Se placer à l'emplacement souhaité
2. `/entreprise batiment`
3. Un marqueur apparaît sur Dynmap (si activé)

### Passer International
1. Avoir 20,000$ disponibles
2. `/entreprise international`
3. Confirmer avec `/entreprise international confirm`
4. Limite de membres passe de 5 à 10

## 🔧 Dépannage

### Le plugin ne se charge pas
- Vérifier que Vault est installé
- Vérifier la version de Spigot (1.16.5)
- Consulter les logs dans `logs/latest.log`

### L'économie ne fonctionne pas
- Installer Vault
- Installer un plugin d'économie (EssentialsX)
- Vérifier que Vault détecte l'économie: `/vault-info`

### Les marqueurs Dynmap n'apparaissent pas
- Vérifier que Dynmap est installé
- Activer dans `config.yml`: `dynmap.enabled: true`
- Redémarrer le serveur

### Erreur de sauvegarde MySQL
- Vérifier les identifiants dans `config.yml`
- Créer la base de données manuellement
- Ou utiliser YAML: `storage.type: YAML`

## 📝 Support et Contribution

### Signaler un Bug
1. Vérifier que le bug n'est pas déjà connu
2. Fournir les logs d'erreur
3. Décrire les étapes pour reproduire

### Demander une Fonctionnalité
1. Décrire clairement la fonctionnalité
2. Expliquer le cas d'usage
3. Proposer une implémentation si possible

## 📜 Licence

Ce plugin est fourni "tel quel" sans garantie. Libre d'utilisation sur serveurs privés et publics.

## 👨‍💻 Auteur

**Dominatuin**
- Version: 1.0-SNAPSHOT
- Compatible: Minecraft 1.16.5
- API: Spigot/Paper

## 🔄 Changelog

### Version 1.0-SNAPSHOT
- ✅ Système complet de gestion d'entreprises
- ✅ Interface graphique interactive
- ✅ Intégration Vault pour l'économie
- ✅ Support Dynmap pour les marqueurs
- ✅ Système de filiales
- ✅ Banque d'entreprise
- ✅ Système de niveaux (1-7)
- ✅ Statut National/International
- ✅ Stockage YAML/MySQL
- ✅ Sauvegarde automatique
- ✅ Messages personnalisables
- ✅ Permissions complètes

---

**Bon jeu! 🎮**
