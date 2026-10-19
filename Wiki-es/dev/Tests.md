# 🧪 Tests del plugin MultiverseNets

Este documento explica cómo funcionan los tests del proyecto y qué cubre cada uno. Pensado para quien
quiera ejecutarlos, entenderlos o ampliarlos.

> Zona de desarrollo: [Estructura](Structure.md) · [Cómo funciona el código](Code.md) · **Los tests**

---

## 1. Ejecutar los tests

Los tests son **JUnit 5 (Jupiter)** y no necesitan un servidor de Minecraft real:

```
mvn test
```

Un build completo (`mvn clean package`) también los ejecuta. Requisitos: JDK 21 y Maven.

Los tests corren sobre la API 1.21.11 (MockBukkit solo existe para 1.21). Para 26.1 y 26.2 la CI
(`.github/workflows/verify.yml`) compila el mismo código contra esas APIs de Paper con JDK 25:
`mvn -P api-26.1 clean compile` y `mvn -P api-26.2 clean compile`. Ejecutar la suite con esos
perfiles no está soportado: MockBukkit no puede arrancar un servidor 1.21 sobre la API 26.x.

## 2. Infraestructura: MockBukkit

La mayoría de tests usan **[MockBukkit](https://github.com/MockBukkit/MockBukkit)**, un servidor
Bukkit en memoria:

```java
@BeforeEach void setUp() {
    server = MockBukkit.mock();                     // servidor falso
    plugin = MockBukkit.load(MultiverseNets.class); // ejecuta onEnable
    world = server.addSimpleWorld("world");
    player = server.addPlayer();
}

@AfterEach void tearDown() {
    MockBukkit.unmock();
}
```

Así se pueden colocar bloques, lanzar eventos (`server.getPluginManager().callEvent(...)`), simular
clics de inventario, explosiones y pistones, ejecutar el ticker
(`new NetworkTicker(plugin, plugin.networks()).tick()`) y volver a leer el estado de los nodos con
`NodeStore`. Los archivos de región van a la carpeta de datos del plugin (MockBukkit no expone las
carpetas de mundo), una carpeta por UUID de mundo, así los tests nunca ven los archivos de otros.

A tener en cuenta:
- Un test que coloca un dispositivo a mano debe guardar también su blob
  (`NodeStore.put(block, NodeBlob.create(type.name()))`); sin él, el bloque no es un nodo.
- MockBukkit no implementa `Display.setBillboard` ni `HumanEntity.openWorkbench`. El ticker captura
  los fallos del holograma, así que los tests del ticker corren con normalidad; el único test que
  llama directamente a la API del holograma (`FluidAndRequesterTest.testHologramRedesignNoFlowOrRouted`)
  aparece como **skipped**. Los tests de crafteo abren un
  `server.createInventory(player, InventoryType.WORKBENCH)` en vez de `openWorkbench`.
- `src/test/java/dev/espi/protectionstones/PSRegion.java` es un stub de la API de ProtectionStones que
  los tests del provider cargan por reflexión.

## 3. Resumen: 52 clases, 359 tests

| Clase (paquete `com.chagui68.multiversenets` salvo que se indique) | Tests | Cubre |
| --- | --- | --- |
| `BlockFlowsTest` | 26 | Romper/colocar con estado embebido, pistones, explosiones, vínculo inalámbrico, rake, llave, corte por agachado, dimensiones, enlace del puente desde el ítem Transmisor, estado del cable. |
| `BlueprintDupeTest` | 3 | Los Blueprints del Encoder nunca se duplican (dos jugadores, romperlo con el menú abierto); instalar un Blueprint lo consume y *Clear All* lo devuelve. |
| `CableCamouflageTest` | 5 | Clic derecho en un cable abre su menú; un bloque en el hueco de camuflaje se guarda (una unidad), el cable toma su aspecto y sigue conectando, recuperarlo devuelve el cristal; shift+clic cambia el bloque y devuelve el anterior; se rechazan bloques inestables, interactivos o no completos y dispositivos; romper suelta el cable y el bloque. |
| `CellGuiTest` | 9 | Menú de la Celda Cuántica: plantilla, depósito rápido, retirada, capacidad, sin duplicación al correr el ticker. |
| `ChickenSorterTest` | 6 | Los genes de GeneticChickengineering se leen como en el addon (producto, nivel, fuerza, pureza, edad, especies especiales); todas las reglas deben cumplirse; el clasificador mueve solo los pollos que cumplen, solo mientras está activo, y nunca otros ítems. Menú: el resumen dice qué pasa, los productos muestran su propio ítem, un pollo en el cursor añade su producto y un clic lo quita; los botones de reglas suben, bajan, se reinician, brillan y mantienen válido el rango de niveles; la barra de control cambia estado, dirección y lado (hacia atrás con clic derecho). |
| `ColoredCableTest` | 4 | Un cable de color sigue siendo `mvn_cable` (cristal tintado, color en el PDC); los cables rojo y azul conectan como los normales y salen con su color; recuperar el camuflaje devuelve el color; las 16 recetas de color dan 4 cables cada una. |
| `CrafterGuiTest` | 10 | Menú del crafter: instalar/desinstalar/limpiar; los crafters de Slimefun aceptan Blueprints de Slimefun y vanilla, los estándar rechazan los de Slimefun. |
| `DeviceTypeTest` | 7 | Clasificación de `DeviceType`: dispositivos con filtro, la Greedy Cell no es celda, ítems de mano, dispositivos direccionales, request y crafters de Slimefun. |
| `DramBayTest` | 13 | Un módulo en un DRAM Bay guarda ítems; un módulo sacado lleva su stock a otra red; romper el bay suelta todos los módulos con su stock; el Fluid DRAM guarda varios fluidos y viaja con ellos; un controlador ya no acepta módulos; un módulo antiguo del controlador espera en el Terminal; un bay admite 18 módulos (el 19.º se queda en la mano) y la red los llena todos; hacer clic en un módulo del menú saca solo ese; un bay guardado con un solo módulo lo conserva con su stock; el menú muestra 18 huecos y un medidor de ítems encendido según el llenado. |
| `FilterGuiTest` | 16 | Menú de filtro: añadir/quitar plantillas, whitelist/blacklist, shift+clic, caras, limpiar; los botones de cara (y, en un Grabber simple, shift+clic) abren el contenedor vecino. |
| `FluidAndRequesterTest` | 14 | Almacenamiento de fluidos e interacción rápida con la celda de fluidos, Liquid Pump (drena una fuente; su menú no tiene botones de modo ni de lado), página de fluidos del terminal, Request Terminal (pedidos, cantidad por chat, cadenas recursivas, ignora Auto-Crafters, Slimefun Request Crafter), Slimefun Auto-Crafter, texto del holograma del controlador. |
| `GrabberQuotaTest` | 9 | `extractMatching`: respeta toda la cuota por ciclo (también HT), junta ranuras del mismo ítem, no toca otros ítems; un ítem rechazado vuelve con `returnToSlots` sin perder nada y se llega al siguiente. |
| `GreedyCellTest` | 8 | Greedy Cell: almacenamiento multi-ítem, capacidad compartida, menú e integración con el terminal. |
| `GreedyReserveTest` | 3 | Un Pusher saca de una Greedy Cell pero deja 1 de un ítem definido; cualquier retirada deja esa unidad; un Pusher con el ítem en su whitelist vacía la Greedy Cell hacia las celdas, los depósitos nuevos se la saltan y sin ese Pusher la Greedy Cell vuelve a tomar el ítem. |
| `GuideMenuTest` | 6 | Cada dispositivo está documentado en los dos idiomas y se encuentra su receta; `/mvnets guide en|es` abre el menú (sin libro); navegar a un dispositivo muestra su receta real; el cambio de idioma conserva la página; los ingredientes que son dispositivos abren su página; no se puede sacar nada. |
| `GuiDupeGuardTest` | 3 | `GuiListener` cancela los clics peligrosos (también en los menús de celda y barril); los depósitos con shift+clic nunca duplican. |
| `GuiFlowsTest` | 11 | Flujos de Terminal, Encoder, Auto-Crafter (crafteo atómico), Crafting Grid y Monitor; la tecla 1 fija un ítem en la página personalizada 1, que luego muestra solo ese ítem y permite retirarlo. |
| `InfinityBarrelTest` | 7 | Capacidad del barril, menú, integración en la red y persistencia al romper/colocar; Void excess destruye el sobrante en vez de ocupar una celda vacía (sin él, el sobrante va a una); el Terminal explica por qué rechaza un ítem parecido. |
| `ModuleIOTest` | 5 | El DRAM Unloader vacía una tarjeta en la red y sus tarjetas no son almacenamiento de la red; lo que la red no admite se queda en la tarjeta; el DRAM Loader llena una tarjeta hasta la cantidad (y hasta su capacidad) y se para; el menú del Loader fija el objetivo e inicia; se leen cantidades como `1m`/`500k`/`max`. |
| `NetworksCoexistenceTest` | 5 | Nombre, clase principal, comandos y permisos nunca chocan con NetworksV6; Slimefun es dependencia blanda. |
| `NewDevicesTest` | 6 | Propiedades del Purger y la Probe; todo `DeviceType` tiene material y nombre. |
| `PluginResourcesTest` | 3 | `plugin.yml` y `config.yml` en el classpath; comprobación de versión. |
| `PosUtilTest` | 2 | El empaquetado de coordenadas ida y vuelta, incluidos bordes del mundo e Y negativa. |
| `QuantumWorkbenchTest` | 2 | La mejora de celdas conserva la carga; los ingredientes se devuelven al cerrar. |
| `QuotaLimiterMenusTest` | 3 | La importación automática se detiene en la cuota; un jugador que deposita en el Terminal (shift+clic) o en el menú de un almacén no tiene límite. |
| `QuotaManagerTest` | 3 | El Quota Manager lista los limitadores de la red, añade un ítem a uno libre sin quitarlo, no añade nada sin uno libre, quita y activa/desactiva; un Grabber vacía un Infinity Barrel que no está conectado a la red (y no lo mete en ella); un barril conectado por cable sigue siendo almacenamiento y el Grabber no lo toca. |
| `RecipeTest` | 11 | Cada receta registrada una vez, también al volver a registrarlas, lo que olvida toda resolución de Blueprint en caché; las recetas del cable y de las celdas T1/T2 funcionan; los componentes son ítems de mano que usan las recetas de dispositivos; una celda con carga casa con su receta de mejora y la conserva; la cuadrícula 2×2 del jugador se completa y los tamaños raros se ignoran. |
| `RecipeValidationTest` | 5 | Los ingredientes dispositivo se registran por receta; un material simple no sustituye a un dispositivo; las mejoras de celdas y módulos conservan su carga; una celda de fluidos con fluido se rechaza; los dispositivos nunca alimentan recetas vanilla. |
| `ReportedIssuesTest` | 8 | El Advanced Pusher nunca pierde ítems (un stack por ranura); una whitelist de varios ítems deja sitio a cada ingrediente; lo que está en la blacklist se queda en la red; nada entra en el inventario del bloque de un Infinity Barrel; las tolvas nunca tocan un dispositivo; los ítems custom coinciden aunque su nombre se guarde distinto; el barril acepta de vuelta un ítem custom que entregó; el Slimefun Recipe Encoder guarda sus Blueprints. |
| `SettingsCellCapacityTest` | 9 | Valores por defecto y casos límite de `Settings` (capacidades, límites, config null). |
| `SlimefunBridgeTest` | 6 | El puente con Slimefun es inerte y nunca lanza errores sin Slimefun; los ítems de Slimefun se identifican por su id del PDC, también con el namespace antiguo. |
| `SlimefunMachinesSwitchTest` | 3 | La sección `slimefun-machines`: activa por defecto; el interruptor general quita las recetas (también al recargar) e impide colocar todas las máquinas de Slimefun pero ninguna vanilla; las claves por máquina y la antigua `sf-crafter.enabled` siguen funcionando. |
| `StackUtilsFastPathTest` | 4 | La comparación rápida de ítems: ítems idénticos casan sea cual sea la cantidad, ids de Slimefun distintos nunca casan, un ítem con meta nunca casa con uno sin ella, el lore se ignora cuando se pide. |
| `StatusLightTest` | 4 | La luz de estado es verde sobre un bloque conectado y roja al cortar el enlace; roja sin red; nunca une dos redes; romperla la olvida. |
| `ToolsTest` | 3 | Llave y Rake son herramientas de mano; el Receptor tiene filtro; los filtros empiezan en whitelist. |
| `TransmissionFixesTest` | 13 | Transmisión de ítems y fluidos: depósitos de fluido todo o nada, la bomba nunca duplica fluido, puente con filtro solo de plantillas, el puente nunca vacía Greedy Cells, un dispositivo compartido por dos controladores trabaja una vez por ciclo, los resultados de crafteo parciales se deshacen, la llave pega plantillas exactas, el rake devuelve el dispositivo, filtros/cara/búfer de tránsito sobreviven a romper y colocar, los búferes de tránsito de más de 99 unidades se guardan sin fallar. |
| `UpgradedFeaturesTest` | 6 | Módulo de memoria en un DRAM Bay, Router, conteo de nodos por chunk, búfer de tránsito del grabber, caché conservada al romper, romper en creativo no suelta nada. |
| `compat.NetworkOwnershipTest` | 9 | Una red funciona dentro del reclamo de su dueño; otras redes y un dueño null son extraños; el terreno público sigue abierto; providers rotos o sin conectar no dan acceso; las respuestas de dueño no se filtran entre redes. |
| `compat.ProtectionCacheTest` | 4 | Las respuestas de protección se cachean: el terreno libre se pregunta una vez, dueño y extraño conservan sus propias respuestas, el barrido conserva las vigentes, vaciar la caché vuelve a preguntar. |
| `compat.ProtectionStonesProviderTest` | 18 | El provider de ProtectionStones contra la forma real de la API (`PSRegion.fromLocation*`, firmas exactas), certificación de dueño/miembro, comportamiento cerrado ante fallos; la búsqueda de regiones de WorldGuard falla de forma segura. |
| `compat.ProtectionWhitelistTest` | 15 | Valores por defecto de la protección y parseo y geometría de `exempt-locations`; el puente queda inerte sin providers. |
| `compat.SlimefunInsertRuleTest` | 4 | Insertar en máquinas de Slimefun: una Electric Smeltery falsa (misma respuesta de transporte que la de Slimefun) deja cada ingrediente en un stack y las demás ranuras libres; un stack se rellena sin abrir otra ranura; una máquina normal llena todas sus ranuras, una por ciclo; un menú que falla a mitad nunca devuelve lo que ya entró. |
| `listen.SneakingRightClickTest` | 3 | Agachado + clic derecho nunca abre el menú de un dispositivo y sigue permitiendo colocar bloques. |
| `net.PerformanceGuardsTest` | 4 | Un Grabber inactivo mucho tiempo sigue saltándose ciclos y despierta en seis como mucho; un Purger ocioso purga en el siguiente ciclo un ítem nuevo y un filtro escrito por un menú; celdas vaciadas y rellenadas en el mismo tick siguen de acuerdo con el índice por Material; las pistas de Material de un filtro cubren todo lo que el filtro deja pasar. |
| `net.ScanCostTest` | 2 | El recorrido de vecinos del BFS no reserva memoria por nodo y un escaneo grande sigue siendo lineal. |
| `net.ScanSchedulingTest` | 5 | Reescaneos: una red intacta no se reescanea en cada intervalo pero el reescaneo completo periódico detecta cambios silenciosos; una red sucia se reescanea al momento; una "stale" espera el intervalo; cargar un chunk solo marca las redes cercanas. |
| `net.StorageCachingTest` | 5 | Las cachés del bucle nunca sirven datos viejos: una escritura de un menú se ve en el mismo tick, el sello de cambios sigue a depósitos y retiradas, los predicados de filtro siguen al filtro, networkAt sigue a los reescaneos, los contadores de flujo se podan. |
| `persist.IncrementalAutosaveTest` | 2 | El autoguardado repartido guarda todas las regiones sucias al empezar, y una región reescrita en cada tick no lo mantiene abierto. |
| `persist.NodeStoreCanonicalTest` | 5 | El blob compartido nunca es más viejo que la última escritura. |
| `persist.NodeStoreCorruptionTest` | 6 | Los blobs corruptos se leen como ausentes, en silencio y barato, se quedan donde están y se pueden sobrescribir; un blob grande sigue cabiendo en el límite de texto del PDC de un ítem. |
| `persist.NodeStoreLazyEncodeTest` | 4 | Codificación diferida: los bytes guardados son los de una codificación inmediata, los blobs por defecto no guardan nada, la instancia viva reescrita llega a las copias y al disco, un blob no serializable conserva sus últimos bytes buenos. |
| `persist.NodeStoreRegionTest` | 12 | Almacenamiento por regiones: miles de nodos y cualquier número de dispositivos activos en un chunk (sin techo ni tope), los blobs por defecto solo guardan su tipo, los datos sobreviven a un reinicio (también en regiones negativas) y un Slimefun Request Crafter con Blueprints pesados conserva su tipo, una región vacía borra su archivo, un archivo corrupto se aparta, el contador de nodos, la migración desde el PDC del chunk de la 5.2 (una escritura fallida conserva los datos antiguos; reimportar nunca pisa un registro más nuevo), los chunks descargados responden "sin nodo". |

## 4. Notas sobre algunas suites

### `TransmissionFixesTest` y `BlueprintDupeTest`
Cada test reproduce una pérdida o duplicación que existía en el código y se corrigió. Se comprobó que
**fallan** con el código anterior a la corrección, así que protegen contra regresiones en vez de
repetir la implementación. Ejemplos: un purgador compartido por dos controladores borraba 256 ítems
por ciclo en vez de 128; una red de fluidos casi llena se quedaba con parte del cubo y con el cubo;
dos jugadores abriendo el mismo Recipe Encoder convertían 16 Blueprints guardados en 32.

### `BlockFlowsTest`
Los tres tests de dimensiones (`networkExtractsInsideTheNether/End/Overworld`) montan una red real con
un grabber y un cofre y ejecutan el planificador. Demuestran que el escaneo, el ticker y el
almacenamiento no tienen ninguna comprobación de dimensión propia, así que lo que bloquee una
dimensión en el servidor es el puente de protección.

### `compat.*`
Los plugins de protección no están en el classpath de test. Se prueba todo lo que no los necesita
(valores por defecto, geometría de la lista blanca, puente inerte) más el provider de ProtectionStones
contra un stub de su API; la decisión real de cada provider se verifica en un servidor.

---

Para entender la funcionalidad que cubren estos tests, ver [Cómo funciona el código](Code.md).
