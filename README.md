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

## Adaptations du Lab 3 au Lab 4

| Point de départ | Adaptation pour cet exercice |
|---|---|
| L'interface et l'implémentation du repository partageaient un package. Cela ne constitue pas, à lui seul, une violation du DIP. | L'interface est placée dans `entities` et l'implémentation dans `adapters.persistence`, conformément aux anneaux de la figure. |
| Les UIs recevaient directement des entités `Order`. | Des records `OrderOutputData` traversent maintenant la frontière de sortie définie par les use cases. |
| `OrderService` dépendait de l'interface `ItemFactory`, ce qui respectait déjà le DIP. | La factory est supprimée pour la simplification demandée : l'interactor crée les `LineItem`, jamais l'UI. |
| La GUI demandait l'état pour se rafraîchir. | L'interactor appelle `OrderOutputBoundary`, implémentée par le presenter, pour transmettre résultats et erreurs. |
| Les validations étaient réparties entre service et modèle. | Les entités valident les données et le cycle de vie ; l'interactor orchestre les opérations. |
| Le formatter utilisait les entités. | Il utilise les données de sortie. |

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
java -cp out com.hsb.mkss1.OrderSystemTests        # tests (26)
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

## Limites de concurrence

Les opérations de la map et l’attribution des IDs supportent des accès concurrents.
Une séquence lecture-modification-mise à jour d’une même commande n’est toutefois
pas atomique : deux clients pourraient écraser leurs modifications respectives.
Le test des insertions concurrentes ne garantit pas la sécurité de ces éditions.
