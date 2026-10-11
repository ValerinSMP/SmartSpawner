# SmartSpawner: propiedad, capacidad y API v1 — entrega D1

Fecha: 2026-10-11. Estado: contrato propuesto y simulación ejecutable; no runtime integrado.
La primera entrega solicitada termina en diseño/simulación. La siguiente entrega será
contabilidad durable y observación, sin limitar generación. No instalar este candidato
como si implementara límites, autenticación o reciclaje.

## Evidencia y política elegida

Fuente canónica para esta tarea: personal,
 /root/development/projects/forks/upstream/SmartSpawner, partida e12d173432aa73b841db077d12895ccff3facbe8.
Upstream integrado: 08f4e8b. El JAR de Custom
 /var/lib/pterodactyl/volumes/9292cb5d-ec3b-4833-a0c3-52aff6c35045/plugins/SmartSpawner-1.6.6.jar
es internamente 1.8.3 y SHA-256 2308273a90cc5d9f1fac0c21dc9cd94e14e95fe4df0b85b97de07425b0ba16e2,
igual al candidato personal de esa fuente. Sync no contiene SmartSpawner en el
volumen revisado. Es prueba de archivo desplegado; no añade validación gameplay.
STATE anterior decía no instalado: el nuevo estado distingue instalación posterior
observada de la entrega de desarrollo. Handoff ajeno conservado sin seguimiento.

Referencias de negocio: los cuatro SPAWNERS-*.md del 2026-10-11 indicados por el
propietario en docs/audits de Survival Custom. No son órdenes de ejecutar lifecycle.
Los drops restaurados en producción no se editan ni se sustituyen por defaults del JAR.

El texto definitivo de opción 1 no estaba en los informes. El propietario corrigió
y confirmó explícitamente en este chat la escala, en este orden:

| Rango | Unidades productivas por tipo y responsable |
|---|---:|
| default | 50 |
| guardian | 60 |
| ascendido | 70 |
| arcano | 80 |
| elite | 90 |
| leyenda y herederos | 100 |

Permisos propuestos: smartspawner.production.capacity.60, .70, .80, .90 y .100,
todos default false. Base 50. Resolver el mayor nodo permitido EFECTIVO mediante
API de permisos en contexto Survival/world/server; negar .100 no niega .90 si
su resultado efectivo es permitido. No inspeccionar nombres de grupos,
primary_group, OP o el rango del visitante. No asignar permisos en esta entrega.
Validar reglas wildcard/negativas/temporales con LuckPerms antes de registrar los
nodos; lectura offline asíncrona. UNKNOWN si contexto/datos no disponibles:
observación muestra incertidumbre; futura aplicación no inventa capacidad.
Registrar contexto, versión y expiración de resolución; invalidar al cambiar nodos,
grupo, mundo, servidor o tiempo. No conservar 100 tras expirar un permiso.
Herederos obtienen nodos por herencia efectiva, nunca suman cupos.

Tipo de cupo: MOB:<entity> o ITEM:<material>, independientemente de custom_spawners,
aliases o display_name. Cambiar variante del mismo tipo no crea otro cupo.
No existe un límite total global adicional por usuario. Cada tipo se cuenta por
separado en todo el ámbito de autoridad, incluyendo colocaciones descargadas.
Stock en cofres/inventarios sigue siendo propiedad, pero no ocupa capacidad colocada.

## Modelo durable: propuesta, no migración ejecutada

Distinguir cuatro ejes: identidad de unidad, titular de stock, responsable económico
de producción y operador de una granja. Un permiso de protección o interacción
no transfiere ninguno. last_interacted_player conserva su función de auditoría.

Tablas NUEVAS previstas (nombres bajo prefix/authority controlados por SmartSpawner):
- unit_issuance: UUID de emisión, autoridad, tipo emitido, fuente referenciada,
  fecha y evidencia; procedencia VERIFIED_ISSUANCE o LEGACY_UNVERIFIED.
- unit_custody: intervalos de ordinales de emisión, contenedor/custodia actual,
  titular UUID nullable, responsable UUID nullable, estado de reclamación y revisión.
  Una unidad (issuance,ordinal) tiene una sola custodia actual.
- unit_container: identidad/version de token, cantidad, tipo/variante actual, ubicación
  item/placed/escrow, spawner_id/server/world/xyz, farm_id nullable. No lore.
- production_owner/production_farm: responsables/miembros/operadores autorizados,
  prioridad elegida, validación y revisión; farm UUID estable, no por chunk.
- unit_operations y operation_payload: clave autoridad+cliente+operation UUID,
  hash canónico del payload completo, actor/purpose/revisión/slices, estado y resultado.
- unit_delivery_outbox: entrega/retirada y prueba durable de conciliación del item.

El namespace autoridad no es el UUID de jugador ni un nombre de nodo intercambiable.
Custom es la autoridad inicial. Sync devuelve UNAVAILABLE/FOREIGN hasta habilitar
un proveedor que use la misma autoridad durable. Nunca copiar SQLite entre nodos.
Si se requiere autoridad multiservidor, MySQL/InnoDB y locks/transacciones son
prerrequisito; Redis sirve para invalidación, no confirma consumo o transferencia.

No cambiar schema v5 de producción en D1. Próxima migración tendrá su versión
propia para las tablas de unidades, transacción por lote e idempotencia de checkpoint.
No depender del orden de columnas de tablas viejas ni sobrescribir loot/XP.

### Legado y granjas compartidas

1. Importar snapshot consistente de TODAS las filas persistidas, sin cargar chunks:
   count = stack_size; vínculo único autoridad+server+spawner_id+revisión de migración.
   Un único lote LEGACY_UNVERIFIED por colocación es identidad retrospectiva, no
   prueba de origen. Repetir el import no emite unidades otra vez.
2. Copiar stock al índice espejo de observación; no cambiar el bloque/loot/XP.
   Propietario/responsable quedan UNRESOLVED. Titular/members de protección y
   last_interacted_player solo candidatos de revisión, nunca asignación automática.
3. Reclamación firmada por actores y revisión administrativa con evidencias externas.
   Granjas compartidas pueden dividir intervalos entre titulares/responsables;
   las sumas no exceden stock. Cada intervalo tiene un solo responsable productivo.
   La protección da derecho de acceso, no crédito retroactivo de inversión.
4. Hasta resolver, observación cuenta stock y muestra would-hold-unresolved; producción
   actual sigue íntegra. Antes de enforcement se requiere transición aprobada.
   Propiedad nueva confirmada no convierte procedencia antigua en VERIFIED_ISSUANCE.
5. Items legados se reconocen como LEGACY_UNVERIFIED. Su PDC antiguo acredita un
   formato compatible, no autenticidad. No emitir procedencia certificada al tocar
   una GUI, ni registrar automáticamente una copia fabricada. El canje los rechaza
   salvo admisión explícita por campaña/revisión durable y cupo individual de unidades.
6. Transferencia legítima conserva emisión/ordinales y origen, cambia custodia/titular
   y, si se acepta, responsable. Usa capacidad del receptor; no crea otra unidad.
   No convertir todas las unidades del stack al titular de quien hace shift-place.

## Stock y selección productiva

La cantidad conservada es stock. Cada colocación puede contener stock mixto por
responsable y procedencia; no basta añadir owner_uuid al bloque. Producción parcial
de stack se calcula sobre intervalos elegibles, sin reducir stack_size, quitar loot,
XP acumulada ni capacidad de almacenamiento ajena a esta función.

Selección propuesta estable: prioridad explícita elegida por responsable, después
orden durable de registro y finalmente ID estable; ordinal para unidades del lote.
Sin prioridad, registro antiguo primero. Permitir selección visible futura con revisión
y cooldown para no oscilar cada tick; una pérdida de rango solo pausa excedentes.
El simulador usa filas homogeneous por responsable y devuelve wouldProduce/wouldHold.
La implantación debe descomponer stacks mixtos por intervalos y luego sumar por bloque.
Reportar razones: propietario sin resolver, permiso sin resolver, cuota por tipo,
presupuesto de granja y ventana espacial. No presentar would-hold como confiscación.

Granja compartida propuesta: UUID aprobado y operadores explícitos; su presupuesto
fijo NO suma rangos, miembros, colocadores o activadores. No crear una nueva farm por
cada visitante/chunk/protección hija. Presencia/no-AFK del responsable u operador
autorizado es una política adicional a aprobar, no una detección anti-alt por IP.
No activarla en D1. Dar acceso a varios propietarios no debe multiplicar capacidad
en el mismo footprint: se aplica también el presupuesto espacial fijo.
Política de fronteras de farm y cifra de presupuesto de farm pendientes.
No se promete detectar que dos UUID pertenecen a una misma persona.

## Geometría espacial propuesta

Comparar ventanas XZ deslizantes [a,a+D] x [b,b+D], inclusivas para posiciones de
bloque, por mundo, todas las Y y tipos juntos. D=32 corresponde a 33 posiciones
enteras por lado y cubre el diámetro horizontal de un radio 16; D=48 amplía el buffer.
Cap U independiente de propietarios/activadores. Propuestas estudiadas U=100/150/200;
NINGUNA cifra espacial está aprobada ni configurada en runtime.

Cada conjunto productivo debe cumplir todas las ventanas, no solo una rejilla.
No hay un centro vinculado al jugador: mover/añadir activadores no cambia la ventana.
Evita bypass de borde/esquina de chunk y stacking vertical. Granjas contiguas legítimas
pueden compartir una ventana: coste explícito a decidir antes de activación.
No usar componentes conexas transitivas que fusionan indefinidamente una cadena.
Una cadena que se extiende fuera de ventanas puede tener más capacidad local:
esto es el alcance espacial propuesto, no cap ilimitado para un mismo farm UUID.
Por eso presupuesto farm fijo y geometría son complementarios.
Para radios configurados >D/2 o activación nueva más extensa hay que revisar D;
no afirmar cobertura universal de la zona de activación.

El reference solver usa las coordenadas X y Z de colocaciones como anchors
extremos. Todo rectángulo con unidades puede deslizarse hasta el mínimo X/Z
ocupado sin perder sus unidades: esos anchors cubren la máxima densidad.
Aplica greedy determinista, con asignación parcial; seguro pero no óptimo/equitativo
entre farms contiguas. Mostrar esta preferencia y permitir prioridad explícita.
Algoritmo acotado a 256 colocaciones con espacio, fuera del tick; prohibido usarlo
en el hilo principal como algoritmo productivo. Implementación futura:
índices por autoridad/owner/baseType, farm y celdas XZ; recalcular solo vecindades
sucias en workers y publicar snapshots con revision/fencing. Sin scans globales por tick.
Movimientos/cambios concurrentes invalidan el plan viejo antes de aplicar loot nuevo.

## Invariantes y caminos de integración futura

| Camino | Invariante requerido |
|---|---|
| Dar/comprar/item factory | Emisión durable por operación y ordinal; clonar plantilla antes de identidad; dos give crean lotes diferentes. Auditoría de crates/objetos serializados necesaria. |
| Colocar/shift-place/natural conversion | Transferir intervalos de item a placed; consumir exactamente cantidad; natural/legacy no inventan origen. |
| Apilar/separar/retirar/romper | Partición y unión de mismos intervalos, nunca duplicación; custodias viejas revocadas. No perder loot/XP ni cambiar callbacks. |
| Huevos | Bloqueo stack>1 antes de evento/consumo conservado; cambia tipo actual, jamás issuedBaseType/origen; recalcular cuota nueva. |
| API modifier/comandos/admin/explosión | Usar mismo servicio de mutación; emitir/remover unidades explícitamente por operación y reconciliar efectos. |
| Transferir propiedad/farm | Consentimiento y operación durable; soltar capacidad antigua y cargar receptor atómicamente; no sumar cuentas. |
| Cargar/reiniciar/chunk unload | Ledger/índices desde almacenamiento completo; unload no libera cupo; identidad y selección persistentes. |
| Generación normal y pregen GUI | Gating antes de crear loot/XP en ambos caminos; revision al aplicar resultado async; invalidar lote pre-generado viejo. |
| Ventas/retirada XP | Stock de loot/XP anterior no sujeto a cuota; conservar SpawnerSell/Take/Drop/ExpClaim y Mending/Eco. |

D1 no toca ninguno de estos caminos. La contabilidad en observación será el primer
incremento runtime; luego verificación con el propietario antes de gating productivo.

## API y reciclador

Contrato compilable DRAFT: api/.../production/v1/SpawnerUnitServiceV1.java.
No añade métodos al SmartSpawnerAPI actual ni registra proveedor en ServicesManager.
El reciclador NO debe intentar consumir con el JAR D1. La existencia de una interfaz
no significa capacidad runtime. La negociación futura exige major=1 y capacidades
AUTHENTICATE_UNITS, DURABLE_RESERVATION, DURABLE_CONSUMPTION y RECOVERY_LOOKUP;
ausencia/incompatibilidad => deshabilitar canjes, nunca fallback por lore/comandos/SQL.

Identity: (issuance UUID, ordinal) comprimida en intervalos. Container token firmado/
validado contra ledger, cantidad/type/version ligados al registro durable. PDC copiado
no crea otra unidad: token/custodia/revisión/intervalos deben corresponder al ledger.
Conservar identidad al unir/separar, emitir nuevo token solo para el nuevo contenedor.
No almacenar IDs en plantillas caches de SpawnerItemFactory. Minting ocurre fuera
del cache, tras clonar, con operación durable. Conservar dos items originalmente
iguales al fusionar sus intervalos, no dar a todo el stack el ID del primer item.
Movimientos de inventario vanilla requieren reconciliación de contenedores:
no se promete autenticidad sin cubrir merge/split/drop/death/hopper/stack manual.
Copias en crates/creativo quedan stale o legacy, nunca emisiones certificadas gratis.

inspect recibe locator actor/slot/fingerprint, no un lore ni ItemStack mutable
compartido entre hilos; implementación captura en región y valida ledger async.
Assessment devuelve recognition, cantidad, tipos actual/emitido, variante,
provenance/revisión y slices. Only ISSUED significa autoridad demostrada.
UNKNOWN/LEGACY_UNVERIFIED no son elegibles automáticamente. El reciclador fija
reglas/precios y campaña; este servicio no paga shards ni valora mobs.

reserve(request) liga la clave autoridad+client+operationId al payload canónico
inmutable: actor, propósito, revisión, fingerprint y slices exactas. Same key,
same payload => receipt anterior; changed payload => CONFLICT. Los intervalos de
otra reserva/consumo no están disponibles. Serializar transacción por contenedor
y intervalos, orden de locks fijo y constraints únicos; retry solo con misma clave.
No confiar en lock de GUI, futuros completados, Redis o ttl para idempotencia.
Solicitudes reservan una sola custodia inventory target; el proveedor revalida
cantidad visible, tipo, fingerprint y revisión antes de mutar el item.

Estados durables PREPARED -> RESERVED -> CONSUMED o RELEASED.
Cualquier discrepancia o caída con efecto externo incierto -> RECOVERY_REQUIRED.
consume/release/lookup exigen cliente autorizado propietario de la operación;
no basta conocer un UUID. No sobrescribir receipt ni permitir release tras CONSUMED.

Retirada de inventario y SQL no son atómicos:
1. Journal PREPARED/fence durable bloquea intervalos; captura preimagen/postimagen.
2. Region task revalida fingerprint y retira/escrow exactamente slices reservadas.
3. Journal registra prueba de escrow y RESERVED. Caída entre 2 y 3: recuperación
   compara revisión/token/custodia con inventario persistido, no repite retirada.
4. consume confirma ledger CONSUMED. Solo ese receipt permite al reciclador pagar.
5. release de reserva no consumida usa outbox de restitución misma identidad y
   reconciliación. Si entrega al inventario tiene resultado incierto no volver a
   entregar; RECOVERY_REQUIRED. Nunca volver a emitir el lote ni liberar por timeout.
Payload conflict, replays, dos plugins y dos nodos usan la misma autoridad transaccional.

El reciclador conserva su propia operación/cotización/campaña/pago. Un receipt
CONSUMED no demuestra shards abonados. Pago con respuesta incierta se concilia;
sin idempotencia/evidencia del receptor no repetir, devolver spawners ni pagar dos veces.
Procedencia no demuestra precio pagado; tipo actual cambiante no aumenta tasación.
No atribuir a legado una compra/origen acreditado.

## Migración, rollback y siguientes entregas

D1: sin cambios de BD/config/data, sin runtime provider, sin nuevos permisos efectivos.
Rollback D1: no instalar el candidato; borrar solo rama/artefactos propios si se
decide, conservando evidencia. Si se hubiera probado el JAR, volver al exacto JAR
previo con lifecycle realizado por propietario; D1 no introduce schema nuevo.
No ejecutar acciones de antiguos informes archivados.

Fase 2: tablas/index espejo y operaciones de observación; migración reanudable
idempotente, chunks descargados incluidos. Propiedad incierta visible, no límite.
SQL/JSON exports agregados sin nombres/coords; JDBC async. Validar SQLite y
MySQL/InnoDB aislados antes de prometer soporte multiservidor.
Rollback fase2: modo observation off, conservar ledger/checkpoints y operaciones;
no restaurar DB completa sobre progreso posterior.
Fase3: custodia real/identidad/give/stack/split/transfer/recovery y proveedor API.
No activar canje hasta crash tests de inventario+SQL y pago en segundo proyecto.
Rollback fase3: congelar mutaciones/canjes nuevos y conciliar reservas; no
downgrade que desconozca tokens ni reimporte legado como unidades nuevas.
Fase4: selección/quotas en Test tras aprobar geometría y cifra farm; observar antes
de enforcement. Reversión de cuotas solo permite stock conservado producir de nuevo,
no cancela canjes/pagos consumidos ni debe generar lotes atrasados de golpe.

Pruebas D1: tier máximo, unknown contexto/owner, stacks parcial, chunks distintos,
tipo independiente, orden estable, transfer snapshot, alturas/bordes/esquinas,
dos propietarios mismo footprint, mundos, cadena/boundary, overflow y planificación
concurrente pura; payload inmutable e intervalos de API sin overlap/overflow.
Estos tests NO acreditan permisos LuckPerms reales, JDBC/MySQL, inventarios, crash,
migration real, emisión/cache ni callbacks en servidor. Matriz posterior obligatoria:
place/break/stack/split/egg/admin/reconnect/unload, reserva doble, payload conflict,
caída en cada transición/journal y payment recovery; gameplay Test por propietario.
