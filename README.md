# Exercise 4 – Clean Architecture for the Order System

## Structure des packages (anneaux de la Clean Architecture)

```
com.hsb.mkss1
├── entities                       RING 1  – Enterprise Business Rules
│   ├── LineItem                   (remplace Item/Product/Service/MergeableItem)
│   ├── Order
│   └── OrderRepository            (interface – "RepositoryInterface" de la figure)
│
├── usecases                       RING 2  – Application Business Rules
│   ├── OrderInteractor            (remplace OrderService)  = UseCaseInteractor
│   ├── input
│   │   ├── OrderInputBoundary     <<Interface>> InputBoundary
│   │   ├── AddLineItemInputData   <<Data Structure>> InputData
│   │   └── FinishOrderInputData
│   └── output
│       ├── OrderOutputBoundary    <<Interface>> OutputBoundary
│       ├── OrderOutputData        <<Data Structure>> OutputData
│       └── LineItemOutputData
│
├── adapters                       RING 3  – Interface Adapters
│   ├── UserInterface              (Controller + Presenter combinés)
│   ├── OrderFormatter             (formatage prix, tri, résumé)
│   ├── console  (CLI, util/Input) Controller + Presenter console
│   ├── gui      (GUI)             Controller + Presenter Swing
│   └── persistence
│       └── InMemoryOrderRepository   Repository (implémente entities.OrderRepository)
│
└── OrderSystemMain                Composition root (câblage de toutes les classes concrètes)
```

## Violations trouvées dans le Lab 3 (et correction)

| Violation (Lab 3) | Correction |
|---|---|
| Lab 3 : l'interface `OrderRepository` était dans le package `repositories`, à côté de son implémentation `InMemoryOrderRepository` ; `OrderService` dépendait de ce package « détail » (il utilisait bien l'interface, mais celle-ci n'appartenait pas au cercle intérieur) | `OrderRepository` est maintenant dans `entities` ; seule l'implémentation `InMemoryOrderRepository` est dans l'anneau externe et dépend vers l'intérieur |
| `OrderService` retournait des entités `Order` aux UIs (`finishOrder`, `getOrder`) → l'UI connaît les entités et les manipule (`order.getItems()`) | L'interactor renvoie des `OrderOutputData` (simples records) via `OrderOutputBoundary` ; CLI/GUI n'importent plus aucune entité |
| `OrderService` dépendait de `ItemFactory` (concret créé/configuré par le main) et c'était la logique « service » qui construisait les items | Factory supprimée ; l'interactor crée lui-même les `LineItem` à partir de l'`InputData` (l'UI ne crée jamais de LineItem) |
| La GUI devait redemander l'état (`getOrder`) pour se rafraîchir ; erreurs via exceptions attrapées par l'UI | Le use case **pousse** le nouvel état au presenter (`presentLineItemAdded`, …) ; les erreurs passent par `presentError` |
| Règles métier (validation, « une commande finie est figée », « pas de commande vide ») dispersées entre `OrderService` et `Order` | Règles dans les entités (`LineItem` constructeur, `Order.addItem/finish`) ; l'interactor orchestre seulement |
| `OrderFormatter` travaillait sur les entités `Order`/`Item` | Il travaille sur `OrderOutputData` |

## Règle de dépendance (vérifiée automatiquement dans `OrderSystemTests`)

* `entities` n'importe rien de `usecases` / `adapters` / Swing / `java.io`
* `usecases` n'importe rien de `adapters` / Swing et n'utilise jamais `System.out`
* `adapters.console` et `adapters.gui` n'importent ni `entities` ni `persistence`

## Flux de contrôle (correspond à la figure)

```
CLI/GUI (Controller) --AddLineItemInputData--> OrderInputBoundary <|-- OrderInteractor
                                                                        |  utilise  OrderRepository (interface, entities)
                                                                        |  crée     LineItem / Order (entities)
CLI/GUI (Presenter)  <|-- OrderOutputBoundary <-- OrderInteractor  --OrderOutputData-->
```

## Compiler / lancer / tester

```
javac -d out $(find src test -name '*.java')
java -cp out com.hsb.mkss1.OrderSystemTests        # tests (23)
java -cp out com.hsb.mkss1.OrderSystemMain cli     # ou gui
```
(depuis le dossier `OrderSystem/`)

## Évaluation : flexibilité vs. overhead

**Flexibilité (gain)**
* Le repository est interchangeable (in-memory → base de données) sans toucher aux use cases : un test le prouve avec un repository « fake » anonyme.
* CLI et GUI sont deux plugins interchangeables sur le même interactor ; ajouter une 3ᵉ UI (web, REST) = une nouvelle classe dans `adapters`, rien d'autre ne change.
* L'interactor est testable sans UI ni base (faux presenter `RecordingPresenter`) ; les règles métier sont testables en pur Java.
* Les changements « UI » (format de prix, tri, textes) ne touchent plus ni le métier ni les entités.

**Overhead (coût)**
* Beaucoup plus de fichiers/types pour un domaine minuscule : 3 interfaces, 5 data structures, 1 interactor, au lieu d'un `OrderService`. Un simple « ajouter un article » traverse Controller → InputData → InputBoundary → Interactor → Entity → Repository → OutputData → OutputBoundary → Presenter.
* Mapping répétitif entité ↔ OutputData/InputData (duplication de champs).
* Câblage circulaire Presenter ↔ Interactor (setter + composition root) et flux de contrôle indirect (callbacks) plus difficile à suivre/déboguer qu'un `return`.
* Rentable pour un système qui va vivre, grossir et changer de technologies ; excessif pour un petit script jetable. Ici le gain principal est pédagogique + testabilité.
