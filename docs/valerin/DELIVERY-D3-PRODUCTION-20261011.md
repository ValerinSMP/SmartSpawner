# D3: propiedad, capacidad y cambio con huevo
Fecha: 2026-10-11. Canonica: personal, /root/development/projects/forks/upstream/SmartSpawner.
Rama work/valerin-production-contract-v1. Plugin 1.8.3-valerin-production.1.
Build limitado personal; 128 tests, cero fallos/errores.
SHA256 plugin: 7885739de608c4a5c1f2dc4720f5fb8fafb7bc54e6880e92fe0bf9a68009decb.

## Entrega real
Instalado SOLO en Test f2ea450e-02a0-416c-9838-e73458d37b1c:
plugins/SmartSpawner-1.6.6.jar (nombre historico, version interna production.1).
Configuracion preparada: enforce, huevo50, farm100, spatial33/cap100.
NO reiniciado ni recargado por el agente. Activacion/gameplay D3 pendientes del propietario.
Custom conserva SHA256 2308273a90cc5d9f1fac0c21dc9cd94e14e95fe4df0b85b97de07425b0ba16e2.
Backup consistente SQLite + JAR + configuracion:
 /mnt/storage/backups/ai/survival-custom/smartspawner-test-production-20261011T180726Z
Candidato HomeSV: /home/mtyn/staging/survival-custom/smartspawner-production-20261011/.
No credenciales, bases reales ni JAR privados publicados.

## Politica aprobada para Test
| Rango efectivo | Unidades productivas por responsable y tipo base |
|---|---:|
| default | 50 |
| guardian | 60 |
| ascendido | 70 |
| arcano | 80 |
| elite | 90 |
| leyenda e inheritores efectivos | 100 |

Lectura LuckPerms en worker: permisos numericos smartspawner.production.capacity.60..100;
si no hay grant/denegacion explicita, grupo efectivo heredado group.<rango>.
OP/default de Bukkit no concede capacidad100. No se consulta solo primaryGroup.
Contexto de granja: servidor configurado por LuckPerms + mundo del bloque.
Contextos dinamicos desconocidos no se inventan para propietarios offline: capacidad retenida.
Para un mismo responsable/tipo en varios mundos, se comparte UN presupuesto y se usa el
menor cupo entre sus contextos resueltos. No multiplica cupos por mundo. Esta regla es
conservadora y explicita. No hay un limite global adicional entre todos los tipos por usuario.
No se han editado grupos/permisos de LuckPerms (pueden estar compartidos con Custom).

Granja registrada explicitamente: mismo label administrativo = mismo presupuesto100,
sumando propietarios, todos los tipos, alturas y mundos. Miembros no suman sus cupos.
No depende del activador ni de quien coloca. El administrador debe registrar con el mismo
label todas las partes de una misma granja; no existe deteccion automatica de socios.
Ademas, en cada mundo, cualquier ventana de33x33 posiciones X/Z contiene como maximo100
unidades productivas. Cuenta todos los tipos y alturas; distancia horizontal32 inclusiva.
No es un limite por chunk ni una cuadricula fija; cubre todas las ventanas, bordes y esquinas.
Ejemplo (-1,-1) y (31,31) coinciden; (-1,-1) y (32,31) no caben juntos en33 posiciones.
Separar fincas fraudulentamente y alejarlas mas alla de33 exige revision de registro:
la geometria no demuestra por si sola que sean negocios independientes.

Se asigna capacidad en orden lexicografico estable de ID del bloque. Un stack puede quedar
parcialmente productivo. Se conservan stock, loot previo y XP; solo se escalan nuevas
tiradas/XP por cantidad productiva. Menus muestran conservadas/productivas/retenidas.
Replanificacion coalescida500ms tras cambios y reconciliacion SQL60s, todo fuera del tick.
No JDBC, consultas LuckPerms ni escaneo global por tick. Aplicacion de lotes valida revisiones;
publicacion espera aplicaciones anteriores mediante fence; regiones no esperan ese fence.
Cambios de nodo LP invalidan el plan. Estado transitorio mantiene produccion en espera.
D3 es autoridad LOCAL: con sync-across-servers=true, enforce retiene produccion y reporta
CROSS_SERVER_AUTHORITY_REQUIRED. NO entregar como autoridad distribuida Custom/Sync.

## Propiedad y migracion
Sidecar SQL aditivo: <tabla_spawners>_production_owners_v1 y
<tabla_spawners>_production_owner_events_v1. Registro y auditoria en una transaccion.
No modifica columnas/blob/loot/XP upstream. Toda lectura/escritura SQL va al worker.
No usa last_interacted_player ni propietario de proteccion como propietario economico.
Legado sin declaracion permanece UNRESOLVED; en enforce no produce hasta adjudicacion
administrativa explicita. En observation conserva produccion.
Antes de Custom se debe inventariar legado y adjudicar granjas compartidas con sus responsables;
no ejecutar asignacion masiva al dueno de proteccion.
El comando give declara al destinatario, sin inferir procedencia certificada.
Retirada/rotura/split conserva owner y farm en PDC del clon individual; colocacion recupera
esa declaracion. No se estampan plantillas cacheadas. No mezclar declaracion owner/farm
distinta en un mismo stack; colocar aparte o transferir explicitamente.
El placer/activador no reasigna propiedad. Transferencia es una asignacion administrativa.
Una asignacion pendiente bloquea rotura/stack/huevo. Fallo de commit retiene el bloque
hasta reintento deliberado; no habilita una transferencia ambigua.
Asignacion y autosave nativo de stock no son una transaccion distribuida: una caida puede
dejar un bloque nuevo sin declaracion durable; queda retenido, requiere reconciliacion.
No se afirma procedencia demostrada ni identidad durable de cada unidad por este PDC.

## Venta y huevos
Un huevo permite cambio de tipo hasta50 unidades (51+ bloqueado); <=0 desactiva ese umbral.
Antes del cambio se prevalidan TODOS los objetos con precio finito/positivo y se vende
por el flujo nativo de venta/deposito/callback SpawnerSellEvent.
Cancelacion de cambio/venta, deposito fallido o objetos sin precio bloquean cambio;
no consume huevo ni descarta loot. XP previo no se cobra por este flujo.
Mantiene el callback de cambio y el fix upstream de no consumir huevo si se cancela.
Mending/Eco y ventas manuales siguen en su flujo nativo.
No añade atomicidad entre Vault/economia e inventario SQL: mantiene limitaciones del flujo
upstream; no se afirma resistencia exactamente-una-vez a kill durante un deposito.

## Pruebas y simulacion
Automatizadas:128, incluyendo tiers/inherencia de decisiones puras/denegaciones, stock parcial,
propiedad legacy nula, misma granja con cuentas amigas, mundos/contextos, cambio de tipo,
transferencia, SQLite sidecar/reapertura/rollback de transaccion, fence concurrente,
precio invalido/venta completa, huevos50/51, ventanas aleatorias contra chequeo exhaustivo.
| Escenario sintetico | Conservadas | Productivas |
|---|---:|---:|
| mismo responsable/tipo: stacks40+30, default | 70 | 50 |
| default con150 BLAZE y150 ZOMBIE, fincas lejanas distintas | 300 | 100 |
| dos Leyenda con80+80 de tipos diferentes en misma granja | 160 | 100 |
| dos granjas y propietarios con80+80 en esquinas a distancia32 | 160 | 100 |
| mismos dos bloques a distancia33, granjas distintas | 160 | 160 |
| legacy sin responsable/granja/contexto | 600 | 0 |
3359 bloques sinteticos: planificador ejercitado en personal limitado. Esta simulacion
NO prueba TPS, latencia SQL/LP real ni impacto sobre las granjas de Custom sin adjudicacion.
Datos reales de Custom no se migraron ni se invento su propiedad.

Gameplay PENDIENTE D3: Leaf26.2 + Vault, cancelacion real de callbacks, LP contextual real,
splits y eggs con interfaces, descarga de chunks/mundos, varios activadores, transferencias,
reinicio/kill del servidor y recuperacion, MySQL real/cross-server. D2 fue aceptado por usuario
para Mending/Eco, venta y stock; eso no equivale a aceptar estos nuevos caminos.

## Prueba por el propietario
1. Reiniciar Test completo; comprobar log SmartSpawner version production.1, sin excepciones.
2. Mirando CADA bloque existente: /ss production assign <nombre-online-o-UUID> prueba
   (mismo label prueba para la granja compartida). Esperar confirmacion guardada.
   /ss production muestra stock/cupo/productivas del bloque observado.
   Un bloque sin asignar debe conservarse y quedar sin produccion; esto es esperado.
3. Default existente:70 unidades BLAZE (en varios bloques) de ese responsable/tipo ->50
   productivas y70 conservadas. Probar otras cuentas ya existentes de los seis rangos.
   NO modificar LP compartido con Custom para preparar estas pruebas.
4. Dos responsables Leyenda,80+80 de tipos distintos, mismo label ->100 productivas total.
   Labels distintos pero dentro33x33 ->100; cambiar Y/chunk/esquina/activadores no lo multiplica.
5. Retirar parte, colocarla y revisar stock total/owner/farm; mezclar diferentes declaraciones
   debe bloquear sin consumo. Transferir mediante assign y confirmar redistribucion del cupo.
6. Acumular loot y XP. Cambiar stack50 con huevo: dinero por loot anterior, XP conservado,
   tipo cambiado. Stack51 bloqueado sin gastar huevo. Probar precio ausente/callback cancelado.
7. Guardado normal, alejarse para descargar chunks y reiniciar: stock/owner/farm/cupo persisten.
   Generacion GUI/pre-generada no debe añadir lotes de tipo/cupo anterior tras cambiar.
   Comprobar ventas manuales, drops, Mending/Eco y callbacks existentes.
8. Registrar lag/TPS y cualquier excepcion. No pasar a Custom hasta aceptacion de D3.

Asignar filas descargadas sin cargar chunks:
 /ss production assign-id <ID-existente> <UUID-owner> <label>
IDs en /ss production (mirando bloque) o listado administrativo. Sin generacion de stock.
Mismo comando assign sirve para transferir; permiso smartspawner.production.admin (op admin).
Configuracion production.* requiere reinicio; el umbral de huevo conserva reload nativo.

## Rollback
Preferido: cambiar production.mode a observation y reiniciar Test por el propietario.
Conserva tablas, declaraciones y loot/XP; vuelve a producir todo el stock.
Si hay un fallo del nuevo JAR: con Test detenido por propietario, restaurar SOLO JAR anterior
y config.yml de backup; arrancar por propietario. No restaurar DB automaticamente:
borraria progreso/loot posterior. El esquema sidecar es aditivo e ignorado por D2.
Copia SQLite consistente disponible para recuperacion excepcional con revision manual
de diferencias y ventas realizadas; no revierte depositos economicos.
No desinstalar ni borrar tablas como parte de rollback ordinario.

## Handoff reciclador
API nueva de reservas/consumo/procedencia NO publicada ni registrada por D3, por decision
del propietario: plugin independiente aun no listo y los spawners se venderan.
docs/valerin/PRODUCTION-OWNERSHIP-API-V1.md sigue DRAFT. Ver HANDOFF-API-D3.md.
No confiar en lore/PDC de propiedad como prueba de autenticidad, ni editar SQL directamente.
