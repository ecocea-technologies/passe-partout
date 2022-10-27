# Passe Partout

Passe partout est un gestionnaire de clés AWS IAM.
Il permet de régénérer de manière régulière des paires de clés afin de mettre en place une rotation automatique.
Il dispose à ce jour de deux implémentations, pour ensuite transmettre ces clés à une/des app(s) Heroku ou un/des
projet(s) Gitlab.

Les notifications sont envoyées par mail à l'aide de [SendGrid](https://sendgrid.com/), lors de la création/suppression d'une paire de clés.

## Liste des variables d'environnement

- **SENDGRID_API_KEY** : clé sendgrid pour les mails d'alerte (toutes les clés ont expirées, etc.)
- **FROM** : Adresse email depuis laquelle sont envoyés les mails
- **EMAIL** : Adresse email à laquelle envoyer les mails d'erreur/rapport
- **DELETE_AFTER** : Nombre de jours après lequel une clé doit être supprimée
- **GENERATE_AFTER** : Nombre de jours après lequel une nouvelle clé doit être générée

## Profiles

### heroku

#### Liste des variables d'environnement :

- **sourceApplication** : Application dans laquelle aller chercher les credentials AWS
- **dispatchApplications** : Liste des applications Heroku sur lesquelles propager les changements (liste séparée par
  des virgules)
- **token** : Token avec les droits de changer les config vars sur les apps

#### Exemple de ligne de commande

``java -jar --passe-partout.heroku[X].sourceApplication=XX --passe-partout.heroku[X].dispatchApplications==XX --passe-partout.heroku[X].token=XX``

Il est possible d'avoir plusieurs configurations avec le [X] (0,1,2...)

### gitlab

#### Liste des variables d'environnement :

- **token** : Gitlab access token
- **projectId** : Id du projet dans lequel aller chercher les credentials AWS
- **environment** : Environnement sur lequel faire la rotation (* pour l'environnement "All" de gitlab)
- **dispatchProjects** : Liste des projets sur lesquels propager les changements (liste séparée par des virgules)

#### Exemple de ligne de commande

``java -jar --passe-partout.gitlab[X].projectId=XX --passe-partout.gitlab[X].environment==XX --passe-partout.gitlab[X].token=XX --passe-partout.gitlab[X].dispatchProjects=XX``

Il est possible d'avoir plusieurs configurations avec le [X] (0,1,2...)

### gitlab,heroku

#### Exemple de ligne de commande

``java -DSENDGRID_API_KEY=$SENDGRID_API_KEY -DEMAIL=$EMAIL -DDELETE_AFTER=$DELETE_AFTER -DGENERATE_AFTER=$GENERATE_AFTER -jar passepartout.jar --spring.profiles.active="gitlab,heroku" --passe-partout.gitlab.projectId=$PROJECT_ID --passe-partout.gitlab.environment=$ENVIRONMENT --passe-partout.gitlab.token=$TOKEN --passe-partout.gitlab.dispatchProjects=$DISPATCH_PROJECTS --passe-partout.heroku.dispatch-applications=$DEST_APP --passe-partout.heroku.token=$HEROKU_TOKEN``
