# SmartSpawner — D11 durabilidad Eco instalado, activación pendiente
2026-10-11. Fuente canónica personal: /root/development/projects/forks/upstream/SmartSpawner.
Rama work/valerin-production-contract-v1; versión 1.8.3-valerin-production.9.
Custom .9 instalado en disco, pendiente reinicio diario/propietario. Runtime último enable observado .7. Sin lifecycle.
SHA256 7a66d64965bf16b79670e76a0366a86dcd386f5c7bff4f1d39d52b99ce0aa9f2.
Backup original .3 HDD smartspawner-custom-overlap-fix-20261011T212446Z.
Backup final HDD smartspawner-custom-overlap-fix-20261011T212643Z (candidato intermedio .4).
Ambos snapshots SQLite consistentes/integrity ok:3182 bloques/18878 unidades al respaldo;
config intacta, ningún restore/SQL directo a stock. Los conteos son actuales, no snapshot inicial3354.

D11: SpawnerBreakListener respeta maxDamage personalizado (Eco8124 frente netherite2031),
conserva metadata/reglas existentes; desgaste0/unbreakable y overflow cubiertos.170tests/0fallos
+shadowJar personal,9regresiones nuevas. BackupHDD smartspawner-tool-durability-20261012T014152Z
JARprevio .8/config/manifest verificados; config y BD intactas, sin migración/lifecycle.
Entrega/rollback: docs/valerin/DELIVERY-D11-TOOL-DURABILITY-20261011.md. Gameplay Eco pendiente.

D10: /ss mine <jugador|UUID> [página] admin/read-only, nombres registrados offline mediante
LPworker. Permiso adicional smartspawner.command.mine.others/defaultop; sin cambiar rangos.
Viewer/target separados, revalida permisos y conserva target/filtros/página.161tests/0fallos+build
personal y locale keys validados. BackupHDD smartspawner-custom-mine-admin-20261011T232513Z (.7),
SQLite consistente3098/17695/integrityok; config intacta; solo nuevas claves admin de idioma.
Entrega/rollback vigente: docs/valerin/DELIVERY-D10-MINE-ADMIN-20261011.md. Gameplay pendiente.

D9 regla vigente: objetos guardados/retirados sin propietario productivo; etiquetas viejas
ignoradas, nuevos drops/give limpian solo owner/farm. Apilar a bloque propio no compara etiquetas
antiguas. Bloques puestos conservan dueños; apilado ajeno rechazado.159tests/0fallos+build personal.
BackupHDD smartspawner-custom-stored-stack-20261011T231511Z (.6), snapshot3099/17720/integrityok;
config intacta; aviso bloqueo actualizado,GUI semánticamente intacta. Sin lifecycle.
Entrega/rollback vigente: docs/valerin/DELIVERY-D9-UNPLACED-NO-OWNER-20261011.md. Gameplay pendiente.

D8 interfaz: /ss mine español, título y resumen colocados/produciendo, filtros estado/mob,
orden y paginación propios. Colores/sonido de /ss list.157tests+shadowJar personal/YAML validado.
3archivos de idioma Custom actualizados solo claves de esta entrega; resto semántico preservado.
Backup HDD smartspawner-custom-mine-ui-20261011T220438Z (.5 previa), SQLite consistente
3171bloques/18816unidades/integrityok/config intacta. Sin lifecycle. Gameplay GUI pendiente.
Entrega/rollback vigente: docs/valerin/DELIVERY-D8-MINE-SPANISH-20261011.md.

D7 decisión vigente: granja100/tipo y ventana33x33 cap100/tipo, entre jugadores y todas
las alturas. Corrige suma entre tipos de D3–D6. Stack50 continúa activo tras reload previo.
155tests/0fallos y shadowJar personal. Graal standalone3354filas/seisrangos sin negativos;
no gameplay. Backup HDD smartspawner-custom-per-type-20261011T215717Z (.4 previa), snapshot
3194bloques/19305unidades/integrityok,config intacta. Sin lifecycle en D7.
Entrega/migración/rollback vigente: docs/valerin/DELIVERY-D7-PER-TYPE-20261011.md.

D6 conservado: colocador UUID durable antes de protección; nuevo bloque cambia responsable del objeto
retirado al colocador. Apilado por otro responsable rechazado antes de consumo. Asignaciones
completas existentes no se reatribuyen. Raíces superpuestas/alias administrativos comparten
un presupuesto durable/transitivo100 por tipo, sin agrupar terrenos independientes por dueño.
Cupos por tipo50/60/70/80/90/100 y ventana continua33x33/cap100 se mantienen. Preserva stock,
loot/XP, callbacks, huevo<=50/venta previa, Mending/Eco y fix Graal .3.
152 pruebas unitarias y shadowJar aprobados en personal limitado. Simulación standalone
GraalVM25.0.3:3354 filas históricas/seisrangos uniformes sin negativos; no prueba gameplay.
Entrega/migración/rollback: docs/valerin/DELIVERY-D6-OVERLAP-PLACER-20261011.md.
Pendiente después del reinicio diario: log .8, READY, /ss mine colocador, generación y
pruebas de granja compartida/stack/Mending/huevo. No reiniciar/recargar por inferencia.
Test continúa .2; Sync no modificado. API reciclaje permanece DRAFT/sin proveedor runtime.

D5 .3 activo tras reinicio propietario19:20Z y READY6918 confirmado en juego.
Ticket solapamiento recuperado con comandos nativos:12bloques/38unidades misma granja;
producción real original confirmada. Drowned pausa manual preexistente preservada.
Detalles privados solo HomeSV staging/KYO-RECOVERY-20261011.md; no UUID/coords en Git.
DEVELOPMENT_HANDOFF.md ajeno intacto/untracked SHA256
4df0d596f2b4c23c983c4c8721cfa518bb78b5606038d4d9110af8e5e42dcb48.
Lecturas SQLite vivas siempre con usuario servidor pterodactyl/UID999 para no crear WAL
con dueño incorrecto. Rollback conserva datos posteriores; restaurar solo JAR .3 autorizado.

Ajuste autorizado posterior: Custom config spawner_properties.default.max_stack_size=50; /ss reload ejecutado correctamente 21:32:17Z, sin reinicio. Reload llama loadConfigurationValues para spawners actuales (incluye maxStackSize); el cambio de dueño/solapamientos .4 sigue pendiente del reinicio diario. Config SHA25618f3ac2d0d8c51e111d42e3df9dd2605110bba8470ef773ea32a03da15b7c07b. Backup HDD smartspawner-stack50-20261011T213217Z. Sin cambios de código/JAR ni SQL directo. Gameplay apilado50 pendiente.
