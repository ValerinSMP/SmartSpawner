# D4: migracion automatica y listado personal
2026-10-11. SmartSpawner 1.8.3-valerin-production.2.
Canonica personal /root/development/projects/forks/upstream/SmartSpawner.
Rama work/valerin-production-contract-v1. Build personal limitado,141 tests aprobados.
SHA256: 427d06d9d3c1f19f19cc590b194cd5f35f6cd893690ed4e154f82fc7ea924747.
Instalado SOLO Test f2ea450e-02a0-416c-9838-e73458d37b1c, archivo historico
plugins/SmartSpawner-1.6.6.jar. No lifecycle por agente; pendiente reinicio del propietario.
Custom intacto SHA2562308273a90cc5d9f1fac0c21dc9cd94e14e95fe4df0b85b97de07425b0ba16e2.
Backup JAR/config/SQLite consistente verificado:
 /mnt/storage/backups/ai/survival-custom/smartspawner-test-auto-20261011T183040Z
Entrega local: /home/mtyn/staging/survival-custom/smartspawner-production-auto-20261011/.

## Regla nueva elegida expresamente por el propietario
El propietario sustituyo la politica anterior de revision individual del legado por:
ultimo jugador registrado; si no se puede resolver, titular principal de la proteccion.
Es una regla RETROSPECTIVA de asignacion de responsabilidad productiva, no prueba de
compra/procedencia. No afirma que el ultimo visitante compro todos los spawners.
Consulta player->UUID por LuckPerms async (tambien offline/Floodgate); no inventa UUID
offline ni consulta red/JDBC en el hilo principal. Nombres no resolubles -> fallback
proteccion. Un fallo temporal de LP no se confunde con ausencia y no cambia silenciosamente
a otro responsable: la migracion se reintenta.
No propietario/responsable resoluble por ninguna fuente -> pendiente.
Ultimo actor vive en last_interacted_player SQL, no una reconstruccion de visitas sin guardar.

En production.legacy-migration.enabled=true:
1. Captura durable aditiva de filas SQL (incluidas descargadas) en
   <tabla>_production_legacy_inputs_v1: ID, mundo,xyz,primer last_player observado,fecha.
   La captura por ID es idempotente; cambios posteriores de last_interacted_player
   no cambian ese input, incluso si cae a mitad de la migracion.
2. Snapshot de metadatos RPB por batches de hasta32 puntos y objetivo2ms por task.
   Sin carga de chunks ni acceso a bloques. API publica RPB opcional por reflection
   (no sombrear/distribuir su JAR privado); query cacheada se filtra por isInside exacto.
   Padre raiz agrupa protecciones fusionadas. Si RPB no esta instalado, usa WG.
   UUID propietario RPB viene de getOwnerUuid; no confundir miembros con titular principal.
   En WG, solo dominio UUID unico para fallback. Un mismo root con varios titulares
   no impide asignar al ultimo actor conocido; sin actor no inventa titular.
   Varios roots superpuestos/ambiguos retienen para revision.
3. Guarda assignment_kind LEGACY_LAST_INTERACTOR o LEGACY_PROTECTION_OWNER y auditoria
   transaccional. No modifica stock, inventario, XP ni campos originales.
4. No sobrescribe una asignacion completa ya confirmada ni una transferencia administrativa.
   Abrir el menu DESPUES de migrar no reasigna propietario. Repetir/reiniciar no duplica
   asignaciones ni emite stock. Caso sin fuentes queda pendiente; reporte /ss production.

Una proteccion raiz = una granja compartida, independiente del actor asignado a cada bloque.
Cuenta todos los tipos/alturas y miembros;100 productivas como antes.
Sin proteccion: farm provisional por celda33x33 y mundo, compartida entre responsables.
Esta celda no sustituye el limite CONTINUO de cualquier ventana33x33: el planificador
sigue comprobando todas las ventanas y cierra bordes/esquinas. Cada celda cabe en una
ventana aprobada, por lo que su cap100 no introduce un cap global extra por jugador.
No crea componentes por cadenas que unan ilimitadamente granjas distantes.
Granjas reales extendidas fuera de una proteccion requieren registrar un mismo label
administrativo; no se afirma poder deducir sociedades privadas de la geometria.

Colocaciones nuevas en modo automatico: conservan responsable declarado en item; si falta,
usan quien coloca (la ultima interaccion inicial registrada), con fallback de proteccion.
La granja fisica se calcula en el NUEVO destino para impedir trasladar IDs de varias granjas
a una misma proteccion y multiplicar el presupuesto100.
Retirada/split conserva declaracion owner/farm en el item; recolocacion automatica reagrupa
farm sin cambiar owner. Registro manual completo ya confirmado gana sobre tarea automatica.
Items sin farm o sin declaracion pueden sumarse a un bloque registrado; toman la
responsabilidad/cupo de destino, sin crear presupuesto adicional ni acreditar procedencia.
Declaraciones confirmadas de otro owner/farm no se mezclan silenciosamente.
Fallo de asignacion nueva deja fence y conserva stock hasta reintento/revision.
Persistencia de colocacion/inventario nativo y sidecar no es una transaccion unica; mantener
la limitacion de caidas D3 y no afirmar garantia distribuida/ledger de identidad de unidades.

Default de source: migration.enabled=false, modo observation; entrega Test: true/enforce.
Ambas opciones requieren reinicio completo. No activar enforcement/migracion en Custom
antes de revisar Test y una simulacion actual del legado; esta entrega no modifica produccion.
En MySQL cross-server=true, sigue retenido: autoridad compartida aun no implementada.

## /ss mine
Nuevo menu personal paginado (45 bloques/pagina):
- mundo,xyz,tipo,unidades conservadas,productivas/retenidas;
- incluye registros SQL en chunks descargados, sin cargarlos;
- solo UUID del ejecutor; ni OP ve otros jugadores por este comando;
- sin teleport, edicion, asignacion ni acciones del listado administrativo;
- clicks/shift/drag no permiten retirar iconos del menu.
Permiso smartspawner.command.mine default true; acceso raiz permite este permiso sin
conceder smartspawner.command.use ni smartspawner.command.list.
El listado /ss list sigue administrativo/default op y no se habilita para jugadores.
Listado personal calcula un snapshot en worker, sin JDBC/escaneo por tick.
UI/ItemStack/inventario se construye en el hilo de entidad. Textos en todos los locales
actuales command_gui/command_messages, nombres/lore sin cursiva.
Es listado de spawners asignados al CUPO del jugador, no certificado de titulo de inversion.
Refrescar reabriendo o cambiando pagina; no es un panel de datos actualizado por tick.

## Prueba siguiente por el propietario
1. REINICIAR Test completo; comprobar version interna production.2 en log.
2. NO asignar los bloques antiguos manualmente: esperar migracion, luego /ss production.
   Debe mostrar assigned y unresolved. Tres bloques de prueba se asignan por la regla elegida.
3. /ss mine desde la cuenta del ultimo jugador registrado: comprobar ubicaciones/cantidad.
   Otra cuenta debe ver solo sus asignaciones. Probar sin OP; no debe acceder a /ss list.
4. Abrir un bloque desde una segunda cuenta y reiniciar: responsable asignado NO cambia.
5. En un legado sin last_player resoluble, probar titular de proteccion; fuera de proteccion
   sin actor resoluble, debe quedar pendiente sin borrar stock/loot/XP.
6. Probar paginacion45/46, shift-click/drag, chunks descargados y varias alturas.
7. Probar nueva colocacion/split/stack; en proteccion compartida con cuentas amigas todos
   comparten100, aunque se retiren de otras granjas y se recolocan alli.
8. Repetir las pruebas D3 (tiers, huevo50/51, venta anterior, XP, Mending/Eco, callbacks).

141 pruebas AUTOMATIZADAS: decisiones/migracion idempotente, checkpoint congelado tras
reapertura, prioridad actor/fallback/retencion, no sobrescribir transferencias, granja compartida,
celdas negativas/bordes, filtrado exclusivo/paginacion/extremos, mas suite D3.
Compilado, hash e instalacion verificados. GAMEPLAY RPB real/GUI/LuckPerms/Leaf26.2,
latencia/TPS a escala y recuperacion tras kill parcial siguen pendientes.
Snapshot SQL de Custom consultado solo lectura en esta tarea:3358 bloques,23729 unidades;
solo last_interacted_player existe como campo de actor. NO son23729 bloques que registrar.
No se ejecutaron asignaciones, writes SQL ni instalacion de Custom.

## Rollback y API
Rollback preferido: production.mode=observation y reinicio por propietario.
Para detener futuras asignaciones: migration.enabled=false y reinicio; no borra las ya
confirmadas. Reasignar solo mediante comando administrativo para mantener auditoria.
Revertir JAR/config a backup con Test detenido por propietario; preservar DB para no
perder loot/XP posterior. Tercera tabla sidecar es aditiva y versiones anteriores la ignoran.
NO restaurar la DB o borrar assignments automaticamente. No revierte ventas monetarias.
API de reciclaje/unidades/procedencia sigue DRAFT y diferida por el propietario;
ver HANDOFF-API-D3.md. No permitir al otro plugin escribir SQL o confiar en lore/PDC.
