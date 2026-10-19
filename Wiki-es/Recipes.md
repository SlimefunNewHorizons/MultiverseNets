# 📜 Recetas y funciones de los ítems de MultiverseNets

Cada ítem del plugin, su **receta** tal como se ve en la mesa de crafteo (cuadrícula 3×3) y qué
**hace** en la red. Las cifras son los valores por defecto de `config.yml`; "ciclo" significa un ciclo
de transferencia (`network.op-interval-ticks.transfer`, 5 ticks).

> En las cuadrículas, `·` marca un hueco vacío. Todo ítem se puede dar también con `/mvnets give <id>`.
> Volver al [índice de la wiki](README.md).

> **Componentes y dispositivos como ingredientes.** Cuando una receta pide un componente u otro
> dispositivo (Network Circuit, Storage Matrix, celda anterior, módulo anterior, cable, celda de
> fluidos, DRAM Bay, Monitor, Advanced Pusher...) tiene que ser ese ítem: el material simple de debajo
> (fragmento de prismarina, fragmento de disco, terracota, lingote de cobre, vidrio, ladrillos de
> prismarina, pistón...) no vale. Las mejoras de celdas y módulos conservan la carga del ingrediente;
> cualquier otra receta rechaza un dispositivo que aún guarde algo. Ningún componente ni dispositivo
> sirve como ingrediente de una receta vanilla.

---

## 🔩 Componentes

Las recetas de los dispositivos base se hacen con estas piezas intermedias, así ninguna receta de
dispositivo choca con una receta vanilla. Son ítems de mano: no se colocan, no se usan y ninguna
receta vanilla los acepta. Los Blueprints codificados con las recetas antiguas de los dispositivos
ya no valen: vuelve a codificarlos.

### Oblea de Silicio (Silicon Wafer) (×2) · `mvn_silicon_wafer`

```
· Q ·
Q S Q
· Q ·
```

> Q = **Cuarzo** · S = **Arena**

- **Resultado**: 2× Silicon Wafer (ítem de mano, Cristales de prismarina)
- **Función**: El primer componente. Con ella se fabrican los Network Circuits, las Storage Matrix y
  los Blueprints en blanco. Solo es un ingrediente.

---

### Circuito de Red (Network Circuit) (×2) · `mvn_network_circuit`

```
R G R
W I W
R G R
```

> R = **Redstone** · G = **Pepita de oro** · W = **Silicon Wafer** · I = **Lingote de hierro**

- **Resultado**: 2× Network Circuit (ítem de mano, Fragmento de prismarina)
- **Función**: Lógica básica de red. La usan el Controlador, los cables, el Terminal, el Grabber y
  el Pusher simples, el Vacuum, el Purger, el Quota Limiter, el Monitor, la Probe, los dos Encoders
  y la Configuration Wrench, y es la base del Logic Processor y la Storage Matrix. Solo es un
  ingrediente.

---

### Procesador Lógico (Logic Processor) · `mvn_logic_processor`

```
Q C Q
C R C
Q C Q
```

> Q = **Cuarzo** · C = **Network Circuit** · R = **Bloque de redstone**

- **Resultado**: 1× Logic Processor (ítem de mano, Caparazón de shulker)
- **Función**: El cerebro de las máquinas que deciden: Controlador, Auto-Crafters, Request Crafters,
  Request Terminal y Crafting Grid. Con dos se hace un Quantum Core. Solo es un ingrediente.

---

### Matriz de Almacenamiento (Storage Matrix) · `mvn_storage_matrix`

```
I W I
C D C
I W I
```

> I = **Lingote de hierro** · W = **Silicon Wafer** · C = **Network Circuit** · D = **Diamante**

- **Resultado**: 1× Storage Matrix (ítem de mano, Fragmento de disco)
- **Función**: El núcleo de todo almacén: Quantum Cell T1, Infinity Barrel, Greedy Cell, Quantum
  Fluid Cell, DRAM Bay y el módulo L1. Solo es un ingrediente.

---

### Bobina de Transferencia (Transfer Coil) (×2) · `mvn_transfer_coil`

```
K R K
R I R
K R K
```

> K = **Lingote de cobre** · R = **Redstone** · I = **Lingote de hierro**

- **Resultado**: 2× Transfer Coil (ítem de mano, Gancho de cuerda)
- **Función**: Mueve cosas: Grabbers y Pushers (simples y avanzados), Vacuum y Liquid Pump. Solo es
  un ingrediente.

---

### Núcleo Cuántico (Quantum Core) · `mvn_quantum_core`

```
A D A
P E P
A D A
```

> A = **Fragmento de amatista** · D = **Diamante** · P = **Logic Processor** ·
> E = **Perla de ender**

- **Resultado**: 1× Quantum Core (ítem de mano, Fragmento de eco)
- **Función**: Une lugares lejanos: Terminal Inalámbrico, Wireless Transmitter y Receiver, Network
  Router y Quantum Workbench. Solo es un ingrediente.

---

## 🖥️ Núcleo y acceso

### Controlador de Red · `mvn_controller`

```
I P I
C N C
I P I
```

> I = **Bloque de hierro** · P = **Logic Processor** · C = **Network Circuit** ·
> N = **Estrella del Nether**

- **Resultado**: 1× Controlador de Red (Magnetita)
- **Función**: Raíz de la red. Cada escaneo empieza aquí y recorre todos los bloques de MultiverseNets
  conectados. Quien lo coloca pasa a ser el **dueño** de la red (lo usa la protección de terrenos).
  Muestra un holograma con estado, número de nodos y totales almacenados. Los módulos de memoria van
  en un **DRAM Bay**, no en el controlador; un controlador que aún tenga un módulo antiguo lo sigue
  usando y su menú puede sacarlo con sus ítems. Clic derecho abre su menú de estado. Un solo
  controlador por red: un segundo cableado a los mismos cables se reporta como `foreign controller`.

---

### Cable de Red (×16) · `mvn_cable`

```
G G G
G C G
G G G
```

> G = **Vidrio** · C = **Network Circuit**

- **Resultado**: 16× Cable de Red (Vidrio)
- **Función**: Conecta dispositivos. Todos los bloques de MultiverseNets conducen; los cables son la
  forma barata de cubrir distancia. Clic derecho en un cable (mano vacía) para ver si llega a un
  controlador y el tamaño de la red, y para abrir su menú.
- **Colores**: 4 cables de cualquier color alrededor de un tinte (en cruz: arriba, izquierda, derecha
  y abajo del tinte) dan 4 cables de ese color, en los 16 colores de tinte. Se ven como cristal tintado
  y conectan exactamente igual que el cable normal, así que el color solo sirve para distinguir zonas;
  al romperlo sale con su color y el camuflaje lo conserva.
- **Camuflaje**: pon un bloque en el hueco del menú del cable y el cable tiene exactamente su aspecto
  (una pared de cuarzo sigue siendo de cuarzo). El bloque se guarda dentro del cable, no se gasta:
  recupéralo y el cable vuelve a ser de cristal; romper el cable suelta el cable y el bloque. Solo
  bloques sólidos y completos que no caen, no cambian solos y no tienen inventario.

---

### Terminal de Red · `mvn_terminal`

```
G C G
E B E
G C G
```

> G = **Vidrio** · C = **Network Circuit** · E = **Perla de ender** · B = **Faro**

- **Resultado**: 1× Terminal de Red (Faro)
- **Función**: La cuadrícula de almacenamiento. La fila superior tiene las vistas: **Almacenamiento de
  red**, **Modo Purga** (ítems que destruyen los purgadores), **Fluidos** y **5 páginas
  personalizadas**, más la ranura de entrada. La fila inferior tiene páginas, orden, búsqueda, resumen
  de la red, **Modo Pin**, **Depositar inventario** y un libro de ayuda. Clic izquierdo saca 1, clic
  derecho un stack, shift+clic al inventario; clic en la cuadrícula con un ítem en el cursor (o
  shift+clic izquierdo en tus ítems, o dejarlos en la ranura de entrada) para guardarlos. Los cubos y
  botellas de miel van al almacenamiento de fluidos.
- **Páginas personalizadas (favoritos)**: pasa el ratón sobre un ítem y pulsa **1-5** para fijarlo o
  quitarlo de la página 1-5 (o activa el Modo Pin y haz clic). Una página personalizada muestra solo lo
  fijado, también sin stock; **Q** sobre un ítem ahí lo quita. Clic derecho en la pestaña para
  renombrarla, shift+clic derecho para vaciarla. Se guardan por jugador y valen en cualquier Terminal.

---

### Terminal Inalámbrico · `mvn_wireless_terminal`

```
· Q ·
P N P
· C ·
```

> Q = **Quantum Core** · P = **Perla de ender** · N = **Estrella del Nether** · C = **Brújula**

- **Resultado**: 1× Terminal Inalámbrico (ítem de mano)
- **Función**: Abre el terminal de una red a distancia. Shift+clic derecho en un Controlador o un
  Terminal para vincularlo, luego clic derecho al aire. Sin **Network Router** solo funciona en el
  mismo mundo y a 64 bloques (`wireless.local-range-without-router`). Se bloquea 10 s tras un combate y
  comprueba que puedas acceder al terreno de la red.

---

### Network Router · `mvn_router`

```
· L ·
· C ·
· Q ·
```

> L = **Pararrayos** · C = **Cable de Red** · Q = **Quantum Core**

- **Resultado**: 1× Network Router (Pararrayos)
- **Función**: Conectado en cualquier punto de una red, quita los límites del Terminal Inalámbrico
  para esa red: cualquier distancia y cualquier mundo.

---

### Network Monitor · `mvn_monitor`

```
G G G
K C K
G G G
```

> G = **Panel de vidrio** · K = **Comparador** · C = **Network Circuit**

- **Resultado**: 1× Network Monitor (Nexo de reaparición)
- **Función**: Panel de diagnóstico en vivo: nodos por tipo, uso de almacenamiento y errores del
  escaneo. Se actualiza mientras está abierto.

---

### Network Probe · `mvn_probe`

```
· A ·
A S A
· C ·
```

> A = **Fragmento de amatista** · S = **Catalejo** · C = **Network Circuit**

- **Resultado**: 1× Network Probe (ítem de mano)
- **Función**: Clic derecho en cualquier bloque (sea nodo o no) para ver a qué red pertenece, cuántos
  nodos tiene, dónde está su controlador y cualquier aviso del escaneo — incluidos los enlaces
  cortados por protección de terrenos.

---

### Network Status Light (Luz de estado) · `mvn_status_light`

```
R G R
L P D
R C R
```

> R = **Redstone** · G = **Piedra luminosa** · L = **Tinte lima** · P = **Network Probe** ·
> D = **Tinte rojo** · C = **Cable de Red**

- **Resultado**: 1× Network Status Light (Cristal tintado)
- **Función**: Colócala encima de cualquier bloque de red. Se pone **verde** si ese bloque llega a un
  Controlador y **roja** si no, así ves el estado de tu sistema desde lejos. Se comprueba una vez por
  segundo, no forma parte de la red y nunca une dos redes.

---

### DRAM Bay · `mvn_dram_bay`

```
I M I
R C R
I Q I
```

> I = **Lingote de hierro** · M = **Storage Matrix** · R = **Redstone** · C = **Bloque de cobre** ·
> Q = **Cuarzo**

- **Resultado**: 1× DRAM Bay (Bombilla de cobre encerada)
- **Función**: Bloque de red que aloja **hasta 18 módulos de memoria** (cualquier mezcla de los
  módulos de ítems de abajo y Fluid DRAMs), cada uno con su propio stock. Mientras están instalados,
  su stock forma parte de la red. Clic derecho al bay con un módulo en la mano para instalarlo en el
  siguiente hueco libre, o clic derecho para abrir su menú: el resumen del bay, 18 huecos de
  módulo con su barra de llenado, y medidores de ítems y fluidos. Instala desde el cursor o con shift+clic; **haz clic en un módulo instalado
  para sacarlo**. Un módulo sacado conserva todo su stock: instálalo en un DRAM Bay de otra red y el
  stock aparece allí y desaparece de la primera. Al romper el bay suelta el bay y cada módulo (con su
  stock) por separado.

---

### Módulos de memoria · `mvn_cache_l1` … `mvn_cache_quantum`

Módulos de ítems para el **DRAM Bay**: almacenamiento multi-ítem, cualquier mezcla de tipos, con la
capacidad de cada nivel en `virtual-cache.tier-1` … `tier-5`. Un módulo conserva sus ítems al sacarlo
del bay. Cada nivel se craftea con el anterior; mejorar un módulo con ítems los conserva.

#### L1 CPU Cache Module — 2.048 ítems

```
C R C
R M R
C R C
```

> C = **Lingote de cobre** · R = **Redstone** · M = **Storage Matrix**

#### L2 CPU Cache Module — 8.192 ítems

```
G L G
L P L
G L G
```

> G = **Lingote de oro** · L = **Lapislázuli** · P = **L1 CPU Cache Module**

#### L3 CPU Cache Module — 32.768 ítems

```
D A D
A P A
D A D
```

> D = **Diamante** · A = **Fragmento de amatista** · P = **L2 CPU Cache Module**

#### DRAM Memory Module — 131.072 ítems

```
N E N
E P E
N E N
```

> N = **Lingote de netherita** · E = **Ojo de ender** · P = **L3 CPU Cache Module**

#### Quantum Cache Matrix — 524.288 ítems

```
N S N
S P S
N S N
```

> N = **Bloque de netherita** · S = **Estrella del Nether** · P = **DRAM Memory Module**

---

### Fluid DRAM Module · `mvn_fluid_dram`

```
D B D
F E F
D B D
```

> D = **Diamante** · B = **Cubo** · F = **Quantum Fluid Cell** (vacía) · E = **Ojo de ender**

- **Resultado**: 1× Fluid DRAM Module (ítem de mano, Corazón del mar)
- **Función**: Módulo de memoria **exclusivo para fluidos** para el DRAM Bay. Guarda varios fluidos a
  la vez, hasta `fluids.dram-capacity-mb` (512.000 mB = 512 cubos) en total, y lo suma al
  almacenamiento de fluidos de la red. Igual que los módulos de ítems, al sacarlo del bay se lleva sus
  fluidos a la red donde lo instales.

---

### DRAM Unloader (Descargador) · `mvn_module_unloader`

```
I H I
R B R
I C I
```

> I = **Lingote de hierro** · H = **Tolva** · R = **Redstone** · B = **DRAM Bay** ·
> C = **Cable de Red**

- **Resultado**: 1× DRAM Unloader (Bombilla de cobre expuesto encerada)
- **Función**: **Vacía tarjetas** (módulos de memoria) en la red. Admite hasta 9 tarjetas; en cada
  ciclo de transferencia mueve hasta 4.096 ítems y 64.000 mB (`transfer.module-items-per-op`,
  `transfer.module-fluid-per-op`). A diferencia del DRAM Bay, las tarjetas de dentro no son
  almacenamiento de la red. Lo que la red no puede guardar se queda en la tarjeta y se prueba el
  siguiente ítem, así la tarjeta queda lo más vacía posible. Necesita espacio libre en la red. Clic
  derecho con una tarjeta en la mano o abre su menú; **Take empty cards** recoge todas las terminadas.

---

### DRAM Loader (Cargador) · `mvn_module_loader`

```
I D I
K B K
I R I
```

> I = **Lingote de hierro** · D = **Soltador** · K = **Comparador** · B = **DRAM Bay** ·
> R = **Bloque de redstone**

- **Resultado**: 1× DRAM Loader (Bombilla de cobre oxidado encerada)
- **Función**: **Llena una tarjeta** con un único ítem (o un fluido, en un Fluid DRAM) sacado de la
  red. Meter una tarjeta no hace nada: elige el objetivo (clic en el hueco de objetivo con el ítem, o
  un cubo de fluido), fija la **cantidad** (botones ±64 / ±1k / ±10k, shift ×100, o haz clic y escribe
  `1m`, `500k`, `max`) y pulsa **Start**. Se para solo cuando la tarjeta tiene esa cantidad o está
  llena. Ejemplo: tarjeta Quantum vacía + adoquín + 500k + Start = una tarjeta lista para vender. Su
  objetivo y cantidad viajan con el bloque al romperlo.

---

## 📦 Almacenamiento de ítems

### Celda Cuántica T1 – T6 · `mvn_cell_t1` … `mvn_cell_t6`

**T1:**

```
G G G
G M G
G G G
```

> G = **Vidrio** · M = **Storage Matrix**

**Tn+1 (n ≥ 1):** la celda anterior en el centro, rodeada de diamantes.

```
D D D
D P D
D D D
```

> D = **Diamante** · P = **Celda anterior** (con o sin carga)

- **Resultado**: 1× celda del siguiente nivel (terracota: normal, naranja, amarilla, lima, cian, morada)
- **Función**: Guarda **un tipo de ítem** cada una. Una celda vacía adopta el primer tipo que no tenga
  otro sitio. Clic derecho para ver o gestionar su contenido; al romperla la carga queda dentro del
  ítem. Una celda **con carga** se mejora conservándola (en la mesa de crafteo o en la Quantum
  Workbench). Capacidades (`cells.capacities`):

| Nivel | Capacidad |
|---|---|
| T1 | 65.536 |
| T2 | 262.144 |
| T3 | 1.048.576 |
| T4 | 16.777.216 |
| T5 | 268.435.456 |
| T6 | 2.000.000.000 |

---

### Infinity Barrel · `mvn_infinity_barrel`

```
N M N
D B D
N D N
```

> N = **Lingote de netherita** · M = **Storage Matrix** · D = **Bloque de diamante** ·
> B = **Barril**

- **Resultado**: 1× Infinity Barrel (Barril)
- **Función**: Como una celda, un tipo de ítem, con `barrel.capacity` (2.000.000.000). **Sigue
  registrado** a su ítem cuando se vacía. En su menú, clic en *Set Item* con un ítem en el cursor para
  registrarlo; clic derecho en *Set Item* con el cursor vacío borra el registro (solo si está vacío).
  Los ítems con id propio (Slimefun y otros plugins) se reconocen por su id y su texto visible, así
  lo que sacas siempre vuelve a entrar. Las tolvas no interactúan con él.

---

### Greedy Cell · `mvn_greedy_cell`

```
G H G
H S H
G M G
```

> G = **Lingote de oro** · H = **Tolva** · S = **Bloque de slime** · M = **Storage Matrix**

- **Resultado**: 1× Greedy Cell (Bloque de slime)
- **Función**: Búfer multi-ítem de hasta `greedy.capacity` (262.144) en total.
  - **Con filtro** es un sumidero prioritario: los ítems que entran en la red y pasan su filtro van
    a ella primero; cada ciclo extrae hasta 512 más de la red y empuja hasta 256 a los contenedores
    vecinos **que no son de la red** (cofres, máquinas de Slimefun). Ideal para alimentar una línea
    de máquinas.
  - **Sin filtro** es almacenamiento de desbordamiento, solo cuando todo lo demás está lleno.
  - **Filtro interno**: Pushers, terminales y crafteo pueden sacar de ella, pero un ítem definido en
    su filtro siempre deja 1 unidad dentro. Un Pusher con ese ítem en su whitelist lo libera: la
    Greedy Cell pasa todo a las celdas y deja de tomarlo. El puente inalámbrico nunca saca de ella.

---

### Quantum Workbench · `mvn_quantum_workbench`

```
D Q D
D C D
D D D
```

> D = **Diamante** · Q = **Quantum Core** · C = **Mesa de crafteo**

- **Resultado**: 1× Quantum Workbench (Bloque de coral cerebro)
- **Función**: Sube una Celda Cuántica T1–T5 al siguiente nivel conservando su carga. Pon la celda en
  el centro, 8 diamantes alrededor, pulsa *Entangle & Upgrade* y recoge el resultado. Los ingredientes
  que queden en la cuadrícula se devuelven al cerrar el menú.

---

## 💧 Fluidos

### Quantum Fluid Cell · `mvn_fluid_cell`

```
G B G
G M G
G L G
```

> G = **Vidrio** · B = **Cubo** · M = **Storage Matrix** · L = **Bloque de lapislázuli**

- **Resultado**: 1× Quantum Fluid Cell (Ladrillos de prismarina)
- **Función**: Guarda un fluido — Agua, Lava, Leche, Nieve polvo o Miel — hasta
  `fluids.cell-capacity-mb` (64.000 mB = 64 cubos). Todas las celdas de fluidos de una red forman su
  almacenamiento de fluidos. Clic derecho con un cubo lleno / botella de miel para verter, con un cubo
  vacío para llenarlo (Agua, Lava, Leche, Nieve polvo). Su menú muestra el nivel, extrae un cubo y
  tiene un botón *Void Fluid Tank* (shift+clic derecho para confirmar) que vacía la celda para siempre.

---

### Liquid Pump · `mvn_liquid_pump`

```
· G ·
P B P
· T ·
```

> G = **Vidrio tintado de azul** · P = **Pistón** · B = **Cubo** · T = **Transfer Coil**

- **Resultado**: 1× Liquid Pump (Vidrio tintado de azul)
- **Función**: Cada ciclo drena un bloque **fuente** de agua o lava justo **debajo** (1.000 mB) hacia
  las celdas de fluidos de la red. La fuente solo desaparece si los 1.000 mB caben enteros. Clic
  derecho para elegir ANY / WATER / LAVA.

---

## 🔄 Transporte de ítems

### Simple Grabber (Importador) · `mvn_grabber`

```
I T I
O C O
I T I
```

> I = **Lingote de hierro** · T = **Transfer Coil** · O = **Observador** · C = **Network Circuit**

- **Resultado**: 1× Simple Grabber (Observador)
- **Función**: Importa de los contenedores vecinos a la red: hasta 128 ítems de un tipo por ciclo, por
  las seis caras. Filtro whitelist/blacklist; **una whitelist vacía no importa nada**, una blacklist vacía lo importa todo. Si la red rechaza
  parte, el sobrante va primero a los Pushers que lo acepten, luego vuelve a las ranuras de donde
  salió y el grabber sigue con el **siguiente ítem**: un ítem que la red no admite nunca bloquea el
  resto del cofre. Una Celda Cuántica o un Infinity Barrel que toque y no esté conectado a
  la red se vacía igual. Solo si no puede volver espera en el búfer de tránsito del grabber (se conserva
  aunque lo rompas).

---

### Advanced Grabber · `mvn_grabber_ht`

```
T P T
```

> T = **Transfer Coil** · P = **Pistón pegajoso**

- **Resultado**: 1× Advanced Grabber (Pistón pegajoso)
- **Función**: Igual que el Simple Grabber ×8 (`transfer.ht-multiplier`): 1.024 ítems por ciclo. Su
  menú permite limitarlo a **una cara**.

---

### Simple Pusher (Exportador) · `mvn_pusher`

```
I T I
D C D
I T I
```

> I = **Lingote de hierro** · T = **Transfer Coil** · D = **Soltador** · C = **Network Circuit**

- **Resultado**: 1× Simple Pusher (Diana)
- **Función**: Exporta de la red a los contenedores vecinos por **las seis caras** (no tiene selector
  de cara): hasta 128 ítems por ciclo, solo si tiene un contenedor al lado y nunca a otro bloque de la
  red. **Una whitelist vacía no hace nada** (un pusher recién puesto nunca vacía la red); una
  **blacklist exporta todo menos lo listado** (lo listado se queda en la red). Como mucho un stack por
  ranura; una whitelist de varios ítems da a cada uno su parte de las ranuras del destino y los sirve
  por turnos, así un ingrediente no llena la máquina. A los hornos el combustible va a su ranura y lo
  demás a la entrada, nunca al resultado. Lo que no cabe vuelve a la red.

---

### Advanced Pusher · `mvn_pusher_ht`

```
T P T
```

> T = **Transfer Coil** · P = **Pistón**

- **Resultado**: 1× Advanced Pusher (Pistón)
- **Función**: Igual que el Simple Pusher ×8 (1.024 por ciclo). Con una cara elegida entrega
  **solo** a ese lado; úsalo para alimentar una máquina.

---

### Network Vacuum · `mvn_vacuum`

```
S T S
T H T
S C S
```

> S = **Hilo** · T = **Transfer Coil** · H = **Tolva** · C = **Network Circuit**

- **Resultado**: 1× Network Vacuum (Esponja)
- **Función**: Cada 10 ticks recoge los ítems del suelo dentro de `vacuum.radius` (4 bloques) hacia la
  red. Filtro whitelist/blacklist: **una whitelist vacía no recoge nada** (un vacuum recién puesto no
  se traga todo el suelo); una blacklist vacía lo recoge todo. Lo que no cabe se queda en el suelo.

---

### Network Purger · `mvn_purger`

```
I L I
L H L
I C I
```

> I = **Lingote de hierro** · L = **Bloque de magma** · H = **Tolva** · C = **Network Circuit**

- **Resultado**: 1× Network Purger (Bloque de magma)
- **Función**: Borra hasta 128 ítems por ciclo que pasen su filtro, para que los residuos (grava,
  semillas…) nunca atasquen la red. **Sin filtro no borra nada**, a propósito.

---

### Network Quota Limiter · `mvn_limiter`

```
R K R
T C T
R K R
```

> R = **Redstone** · K = **Comparador** · T = **Diana** · C = **Network Circuit**

- **Resultado**: 1× Network Quota Limiter (Diana)
- **Función**: Limita cuánto de un ítem puede guardar la red. Todo depósito se detiene en el tope:
  grabbers, vacuum, terminal, resultados de crafteo y el puente inalámbrico. En su menú: clic con un
  ítem para fijar el objetivo, botones ±1/10/64/1.000 o un número por chat para el límite, y un
  interruptor de activado/desactivado. Varios limitadores sobre el mismo ítem: gana el más bajo.

---

### Quota Manager (Gestor de cuotas) · `mvn_quota_manager`

```
R K R
P M P
R L R
```

> R = **Redstone** · K = **Comparador** · P = **Papel** · M = **Network Monitor** ·
> L = **Network Quota Limiter**

- **Resultado**: 1× Quota Manager (Luz de rana verde)
- **Función**: Lista todos los Quota Limiters de su red: el ítem limitado, su límite, el stock de la
  red con una barra de uso, si se aplica y cuántos limitadores quedan libres. Añade un ítem (botón Add
  con el ítem en el cursor, un limitador libre o Shift+clic desde tu inventario): ocupa un Quota
  Limiter libre y el ítem solo se copia. Sin un limitador libre no se añade nada. En un ítem limitado:
  clic izquierdo escribe el límite, Shift+clic derecho lo activa/desactiva, clic derecho quita el ítem
  y libera el limitador.

---

### Wireless Transmitter · `mvn_transmitter`

```
I R I
R C R
I Q I
```

> I = **Lingote de hierro** · R = **Bloque de redstone** · C = **Conducto** · Q = **Quantum Core**

- **Resultado**: 1× Wireless Transmitter (Conducto)
- **Función**: Un extremo del puente inalámbrico entre dos redes. Shift+clic derecho a un Transmisor
  colocado con un Receptor en la mano para que ese **receptor traiga** ítems de esta red. O enlaza al
  revés: shift+clic derecho a un Receptor colocado con un Transmisor en la mano, y este transmisor
  **envía** hasta 128 ítems por ciclo que pasen su filtro a la red del receptor. Su menú es un menú de
  filtro con un botón que abre el terminal de su propia red.

---

### Wireless Receiver · `mvn_receiver`

```
I P I
P L P
I Q I
```

> I = **Lingote de hierro** · P = **Perla de ender** · L = **Lámpara de redstone** ·
> Q = **Quantum Core**

- **Resultado**: 1× Wireless Receiver (Lámpara de redstone)
- **Función**: El otro extremo del puente. Enlazado a un Transmisor, **trae** hasta 128 ítems por ciclo
  que pasen **su** filtro desde la red del transmisor a la suya, a cualquier distancia y entre mundos
  mientras el chunk del otro extremo esté cargado. **Una whitelist vacía no mueve nada**, a propósito;
  una blacklist vacía lo mueve todo. Nunca vacía Greedy Cells. Su menú tiene un botón que abre el
  terminal de la red remota (si tienes acceso a ese terreno).

---

## 🛠️ Crafteo

### Blueprint en blanco (×4) · `mvn_blueprint`

```
P P P
P W P
P B P
```

> P = **Papel** · W = **Silicon Wafer** · B = **Tinte azul**

- **Resultado**: 4× Blueprint (Libro)
- **Función**: Lleva una receta (cuadrícula 3×3 + resultado) una vez escrita por un Recipe Encoder.
  Craftear nunca lo consume. Instalarlo lo mueve al crafter; quitarlo, reemplazarlo o *Clear All* lo
  devuelve.

---

### Recipe Encoder · `mvn_encoder`

```
K P K
C S C
K P K
```

> K = **Saco de tinta** · P = **Papel** · C = **Network Circuit** · S = **Mesa de herrería**

- **Resultado**: 1× Recipe Encoder (Mesa de herrería)
- **Función**: Monta la receta en su plantilla 3×3 persistente (hacer clic solo marca huecos, no gasta
  ítems), pon un Blueprint (en blanco o ya codificado) en la ranura azul y pulsa *Encode*. Clic en un
  Blueprint codificado vuelve a cargar su receta en la plantilla. Los Blueprints que dejes en sus
  ranuras se guardan en el bloque; solo un jugador a la vez los ve.

---

### Slimefun Recipe Encoder · `mvn_sf_encoder`

```
E P E
C B C
E P E
```

> E = **Perla de ender** · P = **Papel** · C = **Network Circuit** · B = **Mesa de encantamientos**

- **Resultado**: 1× Slimefun Recipe Encoder (Mesa de encantamientos)
- **Función**: Igual que el Recipe Encoder para recetas de Slimefun. Los Blueprints que dejes en sus
  ranuras se quedan guardados en el bloque al cerrarlo. Necesita Slimefun. Su receta, su colocación y
  su menú solo existen con `slimefun-machines.enabled` y `slimefun-machines.encoder` en `true` (por
  defecto).

---

### Auto-Crafter · `mvn_crafter`

```
R C R
I P I
R C R
```

> R = **Redstone** · C = **Mesa de crafteo** · I = **Lingote de hierro** · P = **Logic Processor**

- **Resultado**: 1× Auto-Crafter (Mesa de crafteo)
- **Función**: Guarda hasta 18 Blueprints vanilla (`crafter.max-recipes`) y cada 20 ticks intenta cada
  uno una vez con el stock de la red. Todo o nada: si falta un ingrediente no se toca nada, y si el
  resultado no cabe el crafteo entero se deshace. Rechaza Blueprints de Slimefun (usa el Slimefun
  Auto-Crafter).

---

### Slimefun Auto-Crafter · `mvn_sf_crafter`

```
E C E
I P I
E C E
```

> E = **Perla de ender** · C = **Obsidiana llorosa** · I = **Lingote de hierro** ·
> P = **Logic Processor**

- **Resultado**: 1× Slimefun Auto-Crafter (Obsidiana llorosa)
- **Función**: Igual que el Auto-Crafter y acepta **Blueprints de Slimefun y vanilla**.
  `slimefun-machines.crafters: false` (o `slimefun-machines.enabled: false`) quita su receta, su
  colocación, su menú y su crafteo.

---

### Request Crafter · `mvn_request_crafter`

```
R C R
L P L
R C R
```

> R = **Redstone** · C = **Mesa de crafteo** · L = **Atril** · P = **Logic Processor**

- **Resultado**: 1× Request Crafter (Mesa de flechas)
- **Función**: Guarda Blueprints vanilla que **solo** se craftean bajo demanda desde un Request
  Terminal, nunca automáticamente.

---

### Slimefun Request Crafter · `mvn_sf_request_crafter`

```
E C E
L P L
E C E
```

> E = **Perla de ender** · C = **Pilar de púrpura** · L = **Atril** · P = **Logic Processor**

- **Resultado**: 1× Slimefun Request Crafter (Pilar de púrpura)
- **Función**: Igual que el Request Crafter, con Blueprints de Slimefun y vanilla. Depende de
  `slimefun-machines.crafters` y `slimefun-machines.enabled`.

---

### Request Terminal · `mvn_request_terminal`

```
G L G
C P C
G G G
```

> G = **Vidrio** · L = **Atril** · C = **Mesa de crafteo** · P = **Logic Processor**

- **Resultado**: 1× Request Terminal (Atril)
- **Función**: Lista todo lo que pueden fabricar los Request Crafters de la red y lo craftea bajo
  demanda, resolviendo cadenas con el stock de la red (p. ej. troncos → tablones → mesa de crafteo).
  Clic izquierdo 1 lote, shift+clic izquierdo 10, clic derecho 64, shift+clic derecho pide una
  cantidad exacta por chat (un valor no numérico o ≤ 0 cancela). Un botón alterna la entrega a tu
  inventario o a la red. Los Auto-Crafters no aparecen aquí.

---

### Network Crafting Grid · `mvn_crafting_grid`

```
C R C
R G R
C P C
```

> C = **Mesa de crafteo** · R = **Redstone** · G = **Mesa de cartografía** · P = **Logic Processor**

- **Resultado**: 1× Network Crafting Grid (Mesa de cartografía)
- **Función**: Una mesa de crafteo que saca los ingredientes de la red de forma transaccional. La
  plantilla se guarda en el bloque; *Craft 1* / *Craft All* te entregan el resultado.

---

## 🐔 GeneticChickengineering

### Genetic Chicken Sorter · `mvn_chicken_sorter`

```
F E F
C P C
F E F
```

> F = **Pluma** · E = **Huevo** · C = **Comparador** · P = **Advanced Pusher**

- **Resultado**: 1× Genetic Chicken Sorter (Bala de heno)
- **Función**: Mueve **solo** los pollos de bolsillo del addon GeneticChickengineering, elegidos por
  sus genes; cualquier otro ítem se ignora. **Push** envía los pollos que cumplen de la red al bloque
  al que mira; **Pull** los trae de ese bloque a la red (hasta 16 por ciclo).
- **Menú** (clic derecho), de arriba abajo:
  - **Barra de control**: arrancar/parar, dirección (Push/Pull), lado, un libro de resumen que dice
    en palabras simples qué pollos pasan ahora mismo, y ayuda.
  - **Productos aceptados**: 18 huecos, cada uno con el ítem propio del producto. Haz clic en un hueco
    con un pollo de bolsillo en el cursor, o shift+clic a un pollo del inventario, para añadir su
    producto; clic en un producto para quitarlo. Lista vacía = cualquier producto.
  - **Reglas de genes** (todas deben cumplirse), en tres parejas: nivel (mín/máx), genes (fuerza de
    ADN mínima, solo genes puros) e identidad (ADN secuenciado/sin secuenciar, adulto/bebé). Cada
    regla muestra su valor en el nombre y en el tamaño del stack y brilla mientras está activa: clic
    izquierdo +1, derecho −1, shift+clic la reinicia. El rango de niveles nunca puede quedar vacío.
  - **Fila inferior**: vaciar productos, reiniciar reglas, cerrar.
- Empieza **parado**, así no puede vaciar una red antes de configurarlo.

---

## 🧰 Herramientas

### Configuration Wrench · `mvn_configurator`

```
I · I
· C ·
· I ·
```

> I = **Lingote de hierro** · C = **Network Circuit**

- **Resultado**: 1× Configuration Wrench (ítem de mano)
- **Función**: Shift+clic derecho en un dispositivo con filtro para copiarlo (plantillas exactas,
  materiales y modo whitelist/blacklist); clic derecho en otro para pegarlo, reemplazando su filtro.

---

### Network Rake · `mvn_rake`

```
D · D
· S ·
· S ·
```

> D = **Arbusto seco** · S = **Palo**

- **Resultado**: 1× Network Rake (ítem de mano, 250 usos — `rake.uses`)
- **Función**: Desmonta un nodo al instante y te lo devuelve con su estado (filtro, Blueprints,
  enlace…). Rechaza controladores y almacenamientos que aún tengan ítems o fluido.
