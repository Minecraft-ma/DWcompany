# 📚 Guide d'Exemples - DWcompany Plugin

## 🎯 Scénarios d'Utilisation Pratiques

---

## 1️⃣ Créer sa Première Entreprise

### Scénario
Jean veut créer son entreprise "TechCorp" sur le serveur.

### Étapes
```
1. Jean vérifie son argent: /balance
   → Il a 150,000$

2. Jean crée son entreprise:
   /entreprise create TechCorp
   → ✅ "Entreprise TechCorp créée! Vous êtes maintenant CEO."
   → Coût: 100,000$ (première entreprise)
   → Solde restant: 50,000$

3. Jean ouvre le menu:
   /entreprise
   → Interface graphique s'ouvre
   → Il voit son entreprise listée

4. Jean définit son siège social:
   /entreprise batiment
   → ✅ "Siège social défini!"
   → Un marqueur apparaît sur Dynmap
```

---

## 2️⃣ Recruter des Membres

### Scénario
Marie veut rejoindre TechCorp, et Jean (CEO) doit accepter.

### Côté Marie
```
1. Marie cherche l'entreprise:
   /entreprise list
   → Liste des entreprises s'affiche
   → Elle clique sur TechCorp

2. Marie demande à rejoindre:
   /entreprise join TechCorp
   → ✅ "Demande envoyée à TechCorp"
   → Cooldown de 60 secondes activé
```

### Côté Jean (CEO)
```
1. Jean reçoit une notification:
   → "Marie veut rejoindre votre entreprise"
   → "Utilisez /entreprise accept Marie"

2. Jean vérifie les demandes:
   /entreprise requests
   → Liste: "Marie (accept/deny)"

3. Jean accepte Marie:
   /entreprise accept Marie
   → ✅ "Marie a rejoint votre entreprise!"
   → Marie reçoit: "Vous avez rejoint TechCorp!"

4. Jean vérifie les membres:
   /entreprise members
   → CEO: Jean
   → - Marie
   → Total: 2/5 membres
```

---

## 3️⃣ Gérer la Banque d'Entreprise

### Scénario
L'entreprise TechCorp veut gérer ses finances.

### Dépôt d'Argent
```
1. Marie dépose de l'argent:
   /entreprise bank deposit 5000
   → ✅ "Déposé 5,000$. Nouveau solde: 5,000$"
   → Son argent personnel: -5,000$

2. Jean dépose aussi:
   /entreprise bank deposit 10000
   → ✅ "Déposé 10,000$. Nouveau solde: 15,000$"
```

### Retrait d'Argent (CEO uniquement)
```
1. Jean retire de l'argent:
   /entreprise bank withdraw 3000
   → ✅ "Retiré 3,000$. Nouveau solde: 12,000$"
   → Son argent personnel: +3,000$

2. Marie essaie de retirer:
   /entreprise bank withdraw 1000
   → ❌ "Seul le CEO peut retirer de l'argent"
```

### Interface Graphique
```
1. Jean ouvre l'interface bancaire:
   /entreprise bank
   → GUI s'ouvre avec:
     - Solde actuel: 12,000$
     - Boutons: Déposer 100/500/1000/5000
     - Boutons: Retirer 100/500/1000 (CEO uniquement)

2. Jean clique sur "Déposer 1000":
   → Transaction instantanée
   → GUI se rafraîchit automatiquement
```

---

## 4️⃣ Créer des Filiales

### Scénario
TechCorp veut créer une filiale "TechCorp Mobile".

### Méthode 1: Créer une Nouvelle Filiale
```
1. Jean crée la filiale:
   /entreprise filiale create TechCorpMobile
   → ✅ "Filiale TechCorpMobile créée!"
   → Jean est CEO des deux entreprises

2. Jean vérifie ses filiales:
   /entreprise filiale list
   → Filiales de TechCorp:
     - TechCorpMobile
```

### Méthode 2: Ajouter une Entreprise Existante
```
1. Pierre a créé "MobileApp"
2. Jean propose à Pierre:
   /entreprise filiale add MobileApp
   → ✅ "MobileApp est maintenant une filiale!"
   → Pierre reçoit: "Votre entreprise est filiale de TechCorp"

3. Dans l'interface graphique:
   → TechCorp affiche maintenant:
     - Filiales: TechCorpMobile, MobileApp
```

---

## 5️⃣ Passer en Statut International

### Scénario
TechCorp a 5 membres et veut en recruter plus.

### Étapes
```
1. Jean vérifie le statut actuel:
   /entreprise info
   → Statut: National
   → Membres: 5/5 (COMPLET)

2. Jean veut upgrader:
   /entreprise international
   → Message d'information:
     "Upgrade vers International"
     "Coût: 20,000$"
     "Limite membres: 5 → 10"
     "Tapez /entreprise international confirm"

3. Jean confirme:
   /entreprise international confirm
   → ✅ "TechCorp est maintenant International!"
   → Coût: 20,000$ déduit
   → Limite: 10 membres maintenant

4. Jean peut recruter 5 membres de plus:
   → Accepte Sophie: /entreprise accept Sophie
   → Accepte Lucas: /entreprise accept Lucas
   → etc.
```

---

## 6️⃣ Transférer la Propriété

### Scénario
Jean veut passer CEO à Marie avant de partir en vacances.

### Étapes
```
1. Jean initie le transfert:
   /entreprise transfer Marie
   → Message d'avertissement:
     "Vous allez transférer TechCorp à Marie"
     "ATTENTION: Vous perdrez le statut CEO!"
     "Tapez /entreprise transfer Marie confirm"

2. Jean confirme:
   /entreprise transfer Marie confirm
   → ✅ "Propriété transférée à Marie"
   → Marie reçoit: "Vous êtes maintenant CEO de TechCorp!"
   → Tous les membres sont notifiés

3. Marie a maintenant tous les pouvoirs:
   → Peut retirer de l'argent
   → Peut accepter/refuser membres
   → Peut créer des filiales
   → Peut transférer à nouveau
```

---

## 7️⃣ Gérer les Membres Problématiques

### Scénario
Un membre (Thomas) ne respecte pas les règles.

### Expulsion
```
1. Marie (CEO) décide d'expulser Thomas:
   /entreprise kick Thomas
   → ✅ "Thomas a été expulsé de TechCorp"
   → Thomas reçoit: "Vous avez été expulsé de TechCorp"

2. Thomas ne peut plus:
   → Accéder à la banque
   → Voir les informations internes
   → Représenter l'entreprise
```

### Refuser une Demande
```
1. Alex demande à rejoindre:
   /entreprise join TechCorp

2. Marie refuse:
   /entreprise deny Alex
   → ✅ "Demande d'Alex refusée"
   → Alex reçoit: "Votre demande a été refusée"
```

---

## 8️⃣ Quitter une Entreprise

### Scénario Membre Normal
```
1. Sophie veut quitter TechCorp:
   /entreprise leave
   → ✅ "Vous avez quitté TechCorp"
   → Marie (CEO) est notifiée
```

### Scénario CEO
```
1. Marie (CEO) veut quitter:
   /entreprise leave
   → ❌ Message d'avertissement:
     "ATTENTION: Vous êtes CEO!"
     "Options:"
     "1. Transférer: /entreprise transfer <joueur>"
     "2. Supprimer: /entreprise delete"
     "Ou: /entreprise leave confirm (supprime si seul membre)"

2. Marie a 2 options:

   Option A - Transférer puis partir:
   /entreprise transfer Sophie confirm
   /entreprise leave
   → ✅ Sophie devient CEO
   → Marie quitte l'entreprise

   Option B - Supprimer l'entreprise:
   /entreprise delete confirm
   → ❌ TechCorp est supprimée
   → Tous les membres sont notifiés
```

---

## 9️⃣ Utiliser l'Interface Graphique

### Menu Principal
```
1. Ouvrir le menu:
   /entreprise
   → GUI s'ouvre avec options:
     - Créer Entreprise
     - Rejoindre Entreprise
     - Ma Entreprise
     - Banque
     - Liste des Entreprises
     - Filiales (si CEO)
     - Siège Social (si CEO)
     - International (si CEO)
     - Gestion (si CEO)

2. Navigation:
   → Cliquer sur les icônes
   → Sons de feedback
   → Retour avec bouton "Fermer"
```

### Liste des Entreprises
```
1. Ouvrir la liste:
   /entreprise list
   → GUI avec toutes les entreprises
   → Triées par niveau (7 → 1)
   → Icônes selon le niveau:
     - Niveau 1: Cobblestone
     - Niveau 7: Netherite Block

2. Cliquer sur une entreprise:
   → Détails complets s'affichent
   → CEO, membres, balance, filiales
   → Bouton "Demander à rejoindre"
```

---

## 🔟 Commandes Administrateur

### Scénario
Un admin doit gérer les entreprises du serveur.

### Supprimer une Entreprise
```
1. Admin supprime une entreprise inactive:
   /entreprise admin delete InactiveCompany
   → ✅ "InactiveCompany supprimée par admin"
   → Tous les membres sont notifiés
```

### Recharger la Configuration
```
1. Admin modifie config.yml
2. Admin recharge:
   /entreprise reload
   → ✅ "Plugin rechargé avec succès!"
   → Nouvelles valeurs appliquées
```

---

## 💡 Astuces et Bonnes Pratiques

### Pour les CEOs
```
✅ Définir le siège social rapidement (marqueur Dynmap)
✅ Accepter les demandes régulièrement
✅ Gérer la banque avec prudence
✅ Créer des filiales pour organiser
✅ Upgrader International si besoin de plus de membres
✅ Transférer la propriété avant absence prolongée
```

### Pour les Membres
```
✅ Déposer régulièrement dans la banque
✅ Respecter les règles de l'entreprise
✅ Communiquer avec le CEO
✅ Participer aux activités communes
```

### Pour les Admins
```
✅ Configurer les coûts selon l'économie du serveur
✅ Ajuster les limites de membres
✅ Activer les sauvegardes automatiques
✅ Monitorer les performances
✅ Faire des backups réguliers
```

---

## 🎓 Cas d'Usage Avancés

### Réseau d'Entreprises
```
Entreprise Mère: "MegaCorp"
├── Filiale 1: "MegaCorp Tech"
├── Filiale 2: "MegaCorp Finance"
└── Filiale 3: "MegaCorp Retail"

Commandes:
/entreprise filiale create MegaCorpTech
/entreprise filiale create MegaCorpFinance
/entreprise filiale create MegaCorpRetail
```

### Fusion d'Entreprises
```
1. Entreprise A absorbe Entreprise B:
   → CEO de B transfère à CEO de A
   → A ajoute B comme filiale
   → Membres de B rejoignent A

Commandes:
/entreprise transfer CEOdeA (depuis B)
/entreprise filiale add EntrepriseB (depuis A)
```

---

## 📊 Progression Typique

### Semaine 1
```
- Créer l'entreprise (100k$)
- Recruter 2-3 membres
- Définir le siège social
- Atteindre niveau 2 (10k$ gagnés)
```

### Mois 1
```
- 5 membres (limite National)
- Niveau 3-4 (50k-100k$ gagnés)
- Première filiale créée
- Banque: 20k-50k$
```

### Mois 3
```
- Upgrade International (20k$)
- 8-10 membres
- Niveau 5-6 (250k-500k$ gagnés)
- 2-3 filiales
- Banque: 100k+$
```

### Mois 6+
```
- Niveau 7 (1M$ gagnés)
- 10 membres actifs
- Réseau de filiales
- Banque: 500k+$
- Siège social prestigieux
```

---

**Bon jeu et bonne gestion d'entreprise! 🏢💼**
